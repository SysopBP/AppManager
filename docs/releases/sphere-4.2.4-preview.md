# Sphere 4.2.4 — Eclipse Core Preview

Sphere combines App Manager's package-management tools with a refreshed dashboard, customizable backgrounds, glass themes and integrated installer controls. This prerelease introduces the selected Eclipse Core identity and fixes stale artwork after updating.

**Version:** 4.2.4 (456)  
**Tag:** `v4.2.4-preview.1`  
**Package:** `io.github.sysopbp.sphere.debug`  
**Build type:** debug preview  
**Source:** [`52ced6ed9487a2fd7215056be850977672e7af11`](https://github.com/SysopBP/AppManager/commit/52ced6ed9487a2fd7215056be850977672e7af11)  
**Verified APK run:** [38053334533](https://github.com/SysopBP/AppManager/actions/runs/38053334533)

## Downloads

- `Sphere-4.2.4-Eclipse-Core-preview.apk` — installable APK, extracted from the verified workflow artifact without rebuilding or modifying it.
- `Sphere-4.2.4-Setup-Guide.md` — features, first-launch setup, operating modes and update requirements.
- `Sphere-Installer-Control-Guide.md` — installer selection and optional LSPosed/libxposed routing setup.
- `Sphere-4.2.4-Release-Notes.md` — a copy of these notes.
- `SHA256SUMS.txt` — SHA-256 checksums for the APK and guides.

No KernelSU module is required or bundled for this release. Optional system-wide installer hooks are integrated into Sphere and need a compatible framework.

## Eclipse Core branding and icons

- New launcher and splash artwork: an almost-black sphere with a silver crescent and three wine-red app-management tiles in orbit.
- Adaptive, round and five-density launcher resources, plus a simplified monochrome Android themed icon.
- 24 matching two-tone vector assets. Navigation, dashboard shortcuts, main settings, appearance, themes, power and installer controls use the new icon family.
- Navigation preserves silver/wine-red artwork instead of tinting it a single color. Selected labels and backgrounds still show the active tab.
- Day/night icon colors maintain contrast. Third-party applications keep their own package icons.
- **4.2.4 refresh fix:** dedicated Eclipse Core resource names and explicit launcher-alias icon references replace the reused launcher identifiers.
- **4.2.4 app-info fix:** Sphere versions its own disk icon cache and loads its own icon from the current APK rather than relying on a potentially stale package-manager drawable.

## Interface and customization

- Home dashboard with device/backend summaries, memory/storage metrics and tool shortcuts.
- Floating Home, Apps, Installer, Running and Settings navigation; hides on downward scrolling and returns on upward scrolling or at the top.
- Compact app cards, refreshed app-details headers/tabs, and grouped settings with tablet layouts.
- Wallpaper picker with persistent image access, preview, 0–100% dimming and 0–200% image brightness. Image brightness does not adjust the display brightness.
- Material, True glass and Soft contrast presets, with System, Violet, Blue, Mint and Amber accents.
- Blur and surface-opacity controls, live preview and reduced transparency. Theme reset keeps the wallpaper.
- Live blur requires Android 12+ and supported hardware rendering. Battery Saver, reduced transparency and unsupported rendering use solid surfaces.

## Installer Control

The integrated InxLocker-derived engine supports installer/uninstaller selection, target activity overrides, source-app rules and opt-in session/uninstall routing. Ordinary APK selection inside Sphere is available independently of global hooks.

For system-wide routing, use a compatible modern LSPosed/libxposed service, activate Sphere as a module and configure its scope. Root access or choosing an installer alone does not activate hooks. Hot reload requires API 102 support; otherwise restart the scoped processes. Path-access and ColorOS compatibility controls remain experimental and off by default. OEM behavior requires device testing.

## Stability and sessions

This preview retains the earlier Root/ADB startup and Binder cleanup repairs, private local-server extraction, and the Android 17 navigation-drawable crash fix. Power settings include persistent-session control, Android battery-settings shortcuts and requested versus detected operating-mode status. Stopped session monitoring cancels background tasks and avoids background activity relaunch.

Privileged operations depend on the selected backend and device. Sphere retains App Manager's operating modes; this release does not add TokenX's separate Root/System routing architecture.

## Install or update

1. Download and install `Sphere-4.2.4-Eclipse-Core-preview.apk`.
2. Existing `io.github.sysopbp.sphere.debug` installations can update with a compatible signing key. The APK uses the same package as the verified 4.2.3/4.2.4 debug builds.
3. Original App Manager and the Sphere release package are separate installations. Export settings/backups before migrating between packages.
4. Open Sphere and select the operating mode suitable for your device.
5. Confirm **About → 4.2.4 (456)**, then check the launcher and app-info icon.

Minimum Android is **8 / API 26**; target is **Android 17 / API 37**. Do not uninstall to resolve a signing conflict until your settings and backups are exported.

## Validation and preview limitations

For the pinned source commit, the Android 17 APK build, unit tests, lint, icon-generation workflow and CodeQL all passed. The finished APK was inspected: application, splash activity and launcher alias reference `eclipse_core_launcher`; packaged launcher pixels match the selected artwork.

The earlier 4.2.3 build still displayed the old gold icon on the test phone after cache clearing. This preview contains the corrective resource/cache changes; on-device confirmation of that correction is still pending. Appearance, battery behavior and OEM installer hooks are not established by CI checks. Some inherited tool screens retain their existing layouts. This is a debug prerelease, not a production release-signed APK.

Please report the phone model, Android/One UI version, Sphere version, selected backend and reproduction steps. For icon reports, include both the launcher and Sphere app-info screenshots.

## Credits and licensing

- **App Manager:** [Muntashir Al-Islam (@MuntashirAkon) and contributors](https://github.com/MuntashirAkon/AppManager).
- **Sphere fork and modifications:** [SysopBP](https://github.com/SysopBP).
- **Integrated Installer Control engine:** [Chimioo and InxLocker contributors](https://github.com/Chimioo/InxLocker), with GPL-3.0 attribution and license retained in `installer-routing/LICENSE`.

Original copyright, third-party credits and applicable open-source licenses remain intact. Sphere is an independent fork and is not endorsed by upstream developers.
