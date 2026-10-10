// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.misc;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.Context;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class ScreenLockCheckerTest {
 @Test public void closedCheckerCannotInvokeSessionLockCallback() {
  Context context = RuntimeEnvironment.getApplication();
  KeyguardManager keyguard = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
  shadowOf(keyguard).setIsKeyguardSecure(true);
  shadowOf(keyguard).setIsKeyguardLocked(true);
  AtomicInteger calls = new AtomicInteger();
  ScreenLockChecker checker = new ScreenLockChecker(context, calls::incrementAndGet);
  checker.checkLock();
  assertEquals(1, calls.get());
  checker.close();
  checker.close();
  checker.checkLock();
  assertEquals(1, calls.get());
 }
}
