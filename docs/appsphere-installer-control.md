# AppSphere Installer Control — experimental foundation

This screen is an **opt-in installer-management UI**, not an InxLocker port and not an LSPosed module.

## Implemented
- AppSphere Home → Installer Control · Experimental
- Discover APK-view handlers visible to Android's package manager.
- Save a preferred installer package in private AppSphere preferences.
- Open Android's default-app settings.
- Reset the preference to system default.
- Attempt to open an externally installed InxLocker app if its package matches the configured lookup.

## Not implemented / do not assume
- Saved installer preference **does not** redirect APK requests.
- No PackageInstaller session interception, uninstall interception, or forced component dispatch.
- No LSPosed hook registration, scope management, or hot reload.
- No claim of Android 17 compatibility or successful APK build.

## Next implementation requirements
1. Audit InxLocker's hook entrypoints, target packages, Android version guards, and license before porting.
2. Implement a separately switchable hook adapter with fail-open behavior, loop protection, and installer-component validation.
3. Provide a one-tap recovery setting and avoid hooking the system framework until verified.
4. Add automated tests for missing installers, disabled targets, malformed intents, session install flows, and uninstall requests.
5. Verify compilation and installation with GitHub Actions and a real test device.

Source inspiration: https://github.com/Chimioo/InxLocker (GPL-3.0).
Attribution: Chimioo and InxLocker contributors. Retain upstream notices if code is incorporated.
