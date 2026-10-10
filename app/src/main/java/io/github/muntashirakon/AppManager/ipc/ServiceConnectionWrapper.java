// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.ipc;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import io.github.muntashirakon.AppManager.utils.ThreadUtils;

/** Each connection attempt owns its callback, Binder, and completion signal. */
class ServiceConnectionWrapper {
    private static final String TAG = "SphereRootBinding";
    private final Object mLock = new Object();
    private final ComponentName mComponentName;
    private final Runnable mDeathCallback;
    private volatile Attempt mAttempt;

    private final class Attempt implements ServiceConnection {
        final CountDownLatch completed = new CountDownLatch(1);
        IBinder binder;
        IBinder.DeathRecipient deathRecipient;
        String failure;
        boolean cancelled;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (mLock) {
                if (cancelled || mAttempt != this) {
                    RootService.unbind(this);
                    return;
                }
                try {
                    deathRecipient = () -> lost("Binder died");
                    service.linkToDeath(deathRecipient, 0);
                    binder = service;
                    android.util.Log.i(TAG, "BOUND " + name.flattenToShortString());
                } catch (RemoteException e) {
                    failure = "Binder died during handoff";
                }
                completed.countDown();
            }
        }

        private void lost(String reason) {
            boolean notify;
            synchronized (mLock) {
                notify = mAttempt == this && !cancelled && binder != null;
                binder = null;
                failure = reason;
                completed.countDown();
            }
            if (notify && mDeathCallback != null) mDeathCallback.run();
        }

        @Override public void onServiceDisconnected(ComponentName name) { lost("Service disconnected"); }
        @Override public void onBindingDied(ComponentName name) { lost("Binding died"); }
        @Override public void onNullBinding(ComponentName name) { lost("Service returned a null Binder"); }
    }

    public ServiceConnectionWrapper(@NonNull String pkg, @NonNull String cls) {
        this(new ComponentName(pkg, cls), null);
    }
    public ServiceConnectionWrapper(@NonNull String pkg, @NonNull String cls, @Nullable Runnable callback) {
        this(new ComponentName(pkg, cls), callback);
    }
    public ServiceConnectionWrapper(@NonNull ComponentName name) { this(name, null); }
    public ServiceConnectionWrapper(@NonNull ComponentName name, @Nullable Runnable callback) {
        mComponentName = name;
        mDeathCallback = callback;
    }

    @NonNull
    public IBinder getService() throws RemoteException {
        synchronized (mLock) {
            if (active(mAttempt)) return mAttempt.binder;
            throw new RemoteException("Binder unavailable: " + mComponentName.flattenToShortString());
        }
    }

    @NonNull
    @WorkerThread
    public IBinder bindService() throws RemoteException {
        final Attempt attempt;
        boolean launch = false;
        synchronized (mLock) {
            if (active(mAttempt)) return mAttempt.binder;
            if (mAttempt == null) {
                mAttempt = new Attempt();
                launch = true;
            }
            attempt = mAttempt;
        }
        if (launch) ThreadUtils.postOnMainThread(() -> {
            synchronized (mLock) {
                if (attempt.cancelled || mAttempt != attempt) return;
            }
            Intent intent = new Intent().setComponent(mComponentName);
            android.util.Log.i(TAG, "BIND_START " + mComponentName.flattenToShortString());
            try {
                RootService.bind(intent, attempt);
            } catch (RuntimeException e) {
                attempt.lost("Launch failed: " + e.getClass().getSimpleName());
                android.util.Log.e(TAG, "Service launch failed", e);
            }
        });
        try {
            if (!attempt.completed.await(12, TimeUnit.SECONDS)) {
                synchronized (mLock) { attempt.failure = "Timed out waiting for Binder handoff"; }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            synchronized (mLock) { attempt.failure = "Binding interrupted"; }
        }
        synchronized (mLock) {
            if (mAttempt == attempt && !attempt.cancelled && active(attempt)) return attempt.binder;
        }
        cancel(attempt);
        String reason = attempt.failure != null ? attempt.failure : "Binding cancelled";
        android.util.Log.e(TAG, "BIND_FAILED " + mComponentName.flattenToShortString() + ": " + reason);
        throw new RemoteException(reason + ": " + mComponentName.flattenToShortString());
    }

    private boolean active(Attempt attempt) {
        return attempt != null && !attempt.cancelled && attempt.binder != null && attempt.binder.pingBinder();
    }

    private void cancel(Attempt attempt) {
        synchronized (mLock) {
            if (attempt.cancelled) return;
            attempt.cancelled = true;
            if (attempt.binder != null && attempt.deathRecipient != null) {
                attempt.binder.unlinkToDeath(attempt.deathRecipient, 0);
            }
            attempt.binder = null;
            attempt.completed.countDown();
            if (mAttempt == attempt) mAttempt = null;
        }
        ThreadUtils.postOnMainThread(() -> RootService.unbind(attempt));
    }

    @MainThread public void unbindService() { stopDaemon(); }
    public void stopDaemon() {
        Attempt attempt;
        synchronized (mLock) { attempt = mAttempt; }
        if (attempt != null) cancel(attempt);
    }
    boolean isBinderActive() { synchronized (mLock) { return active(mAttempt); } }
}
