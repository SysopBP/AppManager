// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.ipc;

import static org.junit.Assert.*;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class ServiceConnectionWrapperTest {
    private final ComponentName name = new ComponentName("test.sphere", "test.Service");

    private Object awaitAttempt(ServiceConnectionWrapper wrapper) throws Exception {
        Field f = ServiceConnectionWrapper.class.getDeclaredField("mAttempt");
        f.setAccessible(true);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        Object attempt;
        while ((attempt = f.get(wrapper)) == null && System.nanoTime() < deadline) Thread.yield();
        assertNotNull("Worker must create an attempt", attempt);
        return attempt;
    }

    @Test public void cancellationReleasesWaitingWorkerAndAllowsFreshBind() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        ServiceConnectionWrapper wrapper = new ServiceConnectionWrapper(name);
        try {
            Future<IBinder> first = pool.submit(wrapper::bindService);
            Object old = awaitAttempt(wrapper);
            wrapper.stopDaemon();
            try {
                first.get(2, TimeUnit.SECONDS);
                fail("Cancelled bind must fail");
            } catch (ExecutionException e) { assertTrue(e.getCause() instanceof RemoteException); }
            Future<IBinder> second = pool.submit(wrapper::bindService);
            Object fresh = awaitAttempt(wrapper);
            assertNotSame(old, fresh);
            Binder binder = new Binder();
            ((ServiceConnection) fresh).onServiceConnected(name, binder);
            assertSame(binder, second.get(2, TimeUnit.SECONDS));
            // An old process death must not clear the replacement connection.
            Method lost = old.getClass().getDeclaredMethod("lost", String.class);
            lost.setAccessible(true);
            lost.invoke(old, "Old process died");
            assertSame(binder, wrapper.getService());
        } finally { wrapper.stopDaemon(); pool.shutdownNow(); }
    }

    @Test public void nullHandoffFailsPromptlyInsteadOfPublishingActiveService() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        ServiceConnectionWrapper wrapper = new ServiceConnectionWrapper(name);
        try {
            Future<IBinder> result = pool.submit(wrapper::bindService);
            ((ServiceConnection) awaitAttempt(wrapper)).onNullBinding(name);
            try {
                result.get(2, TimeUnit.SECONDS);
                fail("Null Binder must fail");
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof RemoteException);
                assertTrue(e.getCause().getMessage().contains("null Binder"));
            }
            assertFalse(wrapper.isBinderActive());
        } finally { wrapper.stopDaemon(); pool.shutdownNow(); }
    }
}
