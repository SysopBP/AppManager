package io.github.muntashirakon.AppManager.ipc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class RootServiceManagerTest {
    @Test
    public void cancelledLaunchAllowsFreshAttempt() throws Exception {
        Constructor<RootServiceManager> ctor = RootServiceManager.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        RootServiceManager manager = ctor.newInstance();
        Field flags = RootServiceManager.class.getDeclaredField("mFlags");
        flags.setAccessible(true);
        Field pending = RootServiceManager.class.getDeclaredField("mPendingTasks");
        pending.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ServiceConnection, Object> tasks = (Map<ServiceConnection, Object>) pending.get(manager);
        ServiceConnection connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {}
            public void onServiceDisconnected(ComponentName name) {}
        };
        // Both launch bits are set, but no process has delivered a Binder.
        flags.setInt(manager, 3);
        tasks.put(connection, null);
        manager.unbind(connection);
        assertTrue(tasks.isEmpty());
        assertEquals(0, flags.getInt(manager) & 3);
    }

    @Test
    public void pathsAreQuotedAsOneShellArgument() {
        assertEquals("'/path with space/main.jar'", RootServiceManager.shellQuote("/path with space/main.jar"));
        assertEquals("'a'\"'\"'b'", RootServiceManager.shellQuote("a'b"));
    }
}
