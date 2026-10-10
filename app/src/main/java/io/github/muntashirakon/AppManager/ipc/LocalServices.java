// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.ipc;

import android.os.Process;
import android.os.RemoteException;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.muntashirakon.AppManager.BuildConfig;
import io.github.muntashirakon.AppManager.IAMService;
import io.github.muntashirakon.AppManager.misc.NoOps;
import io.github.muntashirakon.AppManager.permission.PermissionOverrideManager;
import io.github.muntashirakon.AppManager.settings.Ops;
import io.github.muntashirakon.AppManager.utils.ThreadUtils;
import io.github.muntashirakon.io.FileSystemManager;

public class LocalServices {
    private static final Object sBindLock = new Object();
    private static final MutableLiveData<Boolean> sState = new MutableLiveData<>(false);

    @NonNull
    public static LiveData<Boolean> state() {
        return sState;
    }

    @NonNull
    private static final ServiceConnectionWrapper sFileSystemServiceConnectionWrapper
            = new ServiceConnectionWrapper(BuildConfig.APPLICATION_ID, FileSystemService.class.getName(),
            LocalServices::onServiceBinderDied);

    @WorkerThread
    public static void bindServicesIfNotAlready() throws RemoteException {
        // Must be one atomic operation.
        synchronized (sBindLock) {
            if (!alive()) {
                bindServices();
            }
        }
    }

    @WorkerThread
    public static void bindServices() throws RemoteException {
        synchronized (sBindLock) {
            unbindServicesIfRunning();
            try {
                bindAmService();
                bindFileSystemManager();
                // Verify both binders before publishing the capability.
                if (!getAmService().asBinder().pingBinder()
                        || !sFileSystemServiceConnectionWrapper.isBinderActive()) {
                    throw new RemoteException("Required service binder is not running.");
                }
                // Update UID only after both services are valid.
                Ops.setWorkingUid(getAmService().getUid());
                // A reconnect can follow a phone restart that cleared volatile firewall rules.
                PermissionOverrideManager.reconcileAll();
                android.util.Log.i("SphereRootBinding", "BACKEND_READY uid=" + getAmService().getUid());
                sState.postValue(true);
            } catch (RemoteException | RuntimeException e) {
                stopServices();
                throw e;
            }
        }
    }

    public static boolean alive() {
        return sAMServiceConnectionWrapper.isBinderActive()
                && sFileSystemServiceConnectionWrapper.isBinderActive();
    }

    private static void onServiceBinderDied() {
        ThreadUtils.postOnBackgroundThread(() -> {
            synchronized (sBindLock) {
                // A replacement bind may have completed before this death callback ran.
                if (!alive()) stopServices();
            }
        });
    }

    @WorkerThread
    @NoOps(used = true)
    private static void bindFileSystemManager() throws RemoteException {
        sFileSystemServiceConnectionWrapper.bindService();
    }

    @AnyThread
    @NonNull
    @NoOps
    public static FileSystemManager getFileSystemManager() throws RemoteException {
        synchronized (sFileSystemServiceConnectionWrapper) {
            try {
                return FileSystemManager.getRemote(sFileSystemServiceConnectionWrapper.getService());
            } finally {
                sFileSystemServiceConnectionWrapper.notifyAll();
            }
        }
    }

    @NonNull
    private static final ServiceConnectionWrapper sAMServiceConnectionWrapper
            = new ServiceConnectionWrapper(BuildConfig.APPLICATION_ID, AMService.class.getName(),
            LocalServices::onServiceBinderDied);

    @WorkerThread
    @NoOps(used = true)
    private static void bindAmService() throws RemoteException {
        sAMServiceConnectionWrapper.bindService();
    }

    @AnyThread
    @NonNull
    @NoOps
    public static IAMService getAmService() throws RemoteException {
        synchronized (sAMServiceConnectionWrapper) {
            try {
                return IAMService.Stub.asInterface(sAMServiceConnectionWrapper.getService());
            } finally {
                sAMServiceConnectionWrapper.notifyAll();
            }
        }
    }

    @WorkerThread
    @NoOps
    public static void stopServices() {
        synchronized (sAMServiceConnectionWrapper) {
            sAMServiceConnectionWrapper.stopDaemon();
        }
        synchronized (sFileSystemServiceConnectionWrapper) {
            sFileSystemServiceConnectionWrapper.stopDaemon();
        }
        Ops.setWorkingUid(Process.myUid());
        Ops.invalidateRuntimeBackend();
        sState.postValue(false);
    }

    @MainThread
    public static void unbindServices() {
        unbindServices(true);
    }

    @MainThread
    private static void unbindServices(boolean invalidateBackend) {
        synchronized (sAMServiceConnectionWrapper) {
            sAMServiceConnectionWrapper.unbindService();
        }
        synchronized (sFileSystemServiceConnectionWrapper) {
            sFileSystemServiceConnectionWrapper.unbindService();
        }
        if (invalidateBackend) {
            Ops.setWorkingUid(Process.myUid());
            Ops.invalidateRuntimeBackend();
        }
        sState.postValue(false);
    }

    @WorkerThread
    private static void unbindServicesIfRunning() throws RemoteException {
        // Basically unregister the services so that we can open another connection
        CountDownLatch unbindWatcher = new CountDownLatch(1);
        ThreadUtils.postOnMainThread(() -> {
            try {
                // Keep the selected launch route while replacing service connections.
                // RootService and RootServiceManager consult Ops during startup.
                unbindServices(false);
            } finally {
                unbindWatcher.countDown();
            }
        });
        try {
            if (!unbindWatcher.await(12, TimeUnit.SECONDS)) {
                throw new RemoteException("UI thread did not complete privileged service cleanup");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Privileged service cleanup interrupted");
        }
    }
}
