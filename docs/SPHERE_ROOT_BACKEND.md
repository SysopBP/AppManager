# Sphere privileged backend lifecycle rebuild

The native launcher and authenticated transport keep their existing protocol. The service-connection lifecycle has been rewritten around one callback and completion signal per attempt.

## Startup

1. Prepare the bundled server JARs and launch through the selected Root or ADB route.
2. Connect with a bounded socket timeout and authenticate using the configured token.
3. Preserve the launch route while cleaning up old service connections.
4. Register the protected Binder receiver before starting the privileged process.
5. Accept the manager Binder and bind AMService, then FileSystemService.
6. Verify both Binders and the remote UID before publishing backend readiness.

Root startup also recognizes UID 0 from a manually started authenticated server. Merely granting root to the application no longer determines the JAR path for an unrelated active transport.

## Recovery

Cancellation releases waiting workers. A failed bind cancels its pending manager request and clears the corresponding launch-in-progress flag. Each new attempt has a distinct callback, so an old Binder death cannot clear a replacement connection. Socket handshake failures close their sockets. The local-server singleton rechecks its instance under the initialization lock.

Binder proxy calls restore their original calling identity even when a forwarded transaction throws.

## Diagnostics

Logcat tags `SphereRootBinding`, `RootServiceManager`, and `SphereRootTransport` report BIND_START, BOUND, BIND_FAILED, ROOT_MANAGER_HANDOFF, SERVER_AUTHENTICATED, and BACKEND_READY. Authentication tokens are not included in the new diagnostic messages.

## Validation

The APK workflow runs native launcher checks and the privileged-backend unit tests before packaging. Regression tests cover cancellation, fresh binds after failure, null Binder handoff, and stale process-death callbacks. Existing transport and server broadcast tests remain enabled.

A successful CI run establishes build and unit-test results. On-device validation is still required for KernelSU Binder policy, Samsung Android 17 broadcasts, Root mode changes, app installation, force-stop/relaunch, and reboot recovery.
