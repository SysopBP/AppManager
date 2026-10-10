<!-- SPDX-License-Identifier: GPL-3.0-or-later OR CC-BY-SA-4.0 -->

<p align="center">
  <img src="arts/sphere/amoled-master.png" alt="Sphere AMOLED gold globe" height="150dp">
</p>

<h1 align="center">Sphere</h1>

**Sphere (AppSphere) — a SysopBP fork of App Manager**

AppSphere is based on [App Manager by Muntashir Al-Islam and contributors](https://github.com/MuntashirAkon/AppManager). Original authorship, copyright notices, and applicable open-source licenses are retained. This independent fork is not endorsed by the original developers.


<p align=center>
  <a href="docs/APPSPHERE_FEATURES.md">Sphere guide</a> ·
  <a href="CHANGELOG.md">What's New</a> ·
  <a href="https://github.com/SysopBP/AppManager/actions/workflows/appsphere-debug.yml">Debug APK builds</a> ·
  <a href="https://muntashirakon.github.io/AppManager">Upstream manual</a> ·
  <a href="https://github.com/SysopBP/AppManager/releases">Fork releases</a> ·
  <a href="https://t.me/AppManagerChannel">Telegram Channel</a>
</p>

---

## Sphere 4.2.2 (454)

- AMOLED edition of the uploaded gold-globe icon pack with pure-black artwork and a themed vector icon.
- Five labelled dock icons, scrolling hide/show and a Home dashboard with backend/device metrics.
- Compact app cards, refreshed detail headers/tabs and grouped settings with tablet support.
- Custom wallpaper with preview, dimming and image-brightness controls.
- Material, True glass and Soft contrast presets, five accents, blur/opacity and reduced transparency.
- Integrated InxLocker installer/uninstaller controls, optional scoped session/uninstall routing,
  module status, launcher visibility, logging and supported hot reload.
- Power/session controls and root/ADB startup, Binder cleanup and local-server extraction repairs.
- Android 17 build workflow, package/branding checks and current cumulative What's New notes.

Minimum Android is **8/API 26**; the app targets **Android 17/API 37**.
Release package: `io.github.sysopbp.sphere`. Debug package: `io.github.sysopbp.sphere.debug`.
These are separate from the original App Manager IDs. Export settings/backups before migrating.

Download **Sphere-Android17-debug** from a successful
[workflow run](https://github.com/SysopBP/AppManager/actions/workflows/appsphere-debug.yml).
System-wide installer routing requires compatible libxposed/LSPosed activation and scope;
root alone is insufficient. Live glass requires Android 12+ and supported hardware and
uses solid fallbacks for Battery Saver/reduced transparency. Actual appearance, power use
and OEM routing support require phone verification.

Read the [feature/setup guide](docs/APPSPHERE_FEATURES.md),
[installer guide](docs/appsphere-installer-control.md),
[implementation status](docs/APPSPHERE_MIUIX_INXLOCKER_ROADMAP.md) and
[complete fork changelog](CHANGELOG.md).

## Inherited App Manager features

### General features
- Fully reproducible, copylefted libre software (GPLv3+)
- Material 3 with dynamic colours
- Display as much information as possible in the main page
- List activities, broadcast receivers, services, providers, app ops, permissions, signatures, shared libraries, etc. of an application
- Launch activities and services
- Create shortcuts of activities
- [Intercept activities](https://muntashirakon.github.io/AppManager/#sec:interceptor-page)
- Scan for trackers and libraries in apps and list (all or only) tracking classes (and their code dump)
- View/save the manifest of an app
- Display app usage, data usage (mobile and Wi-Fi), and app storage info (requires “Usage Access” permission)
- Install/uninstall APK files (including APKS, APKM and XAPK with OBB files)
- Share APK files
- Back up/restore APK files
- Batch operations
- Single-click operations
- Logcat viewer, manager and exporter
- [Profiles](https://muntashirakon.github.io/AppManager/#sec:profiles-page)
- Debloater
- Code editor
- File manager
- Simple terminal emulator
- Open an app in Aurora Store or in your favourite F-Droid client
- Sign APK files with custom signatures before installing
- Backup encryption: OpenPGP via OpenKeychain, RSA, ECC (hybrid encryption with AES) and AES.
- Track foreground UI components

### Root/ADB-only features

- Revoke runtime (AKA dangerous) and development permissions
- Change the mode of an app op
- Display/kill/force-stop running apps or processes
- Clear app data or app cache
- View/change net policy
- Control battery optimization
- Freeze/unfreeze apps

### Root-only features

- Block any activities, broadcast receivers, services, or providers of an app with native import/export as well as Watt and Blocker import support
- View/edit/delete shared preferences of any app
- Back up/restore apps with data, rules and extras (such as permissions, battery optimization, SSAID, etc.)
- View system configurations including blacklisted or whitelisted apps, permissions, etc.
- View/change SSAID.

…and many more! This single app combines the features of 5 or 6 apps any tech-savvy person needs!

### Upstream roadmap (not Sphere release features)
- Finder: Find app components, permissions etc. in all apps
- Basic APK editing
- Routine operations
- Enable/disable app actions such as launch on boot
- Crash monitor
- Systemless disabling/uninstalling of the system apps
- Import app list exported by App Manager
- More advance terminal emulator
- Database viewer and editor, etc.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80" />](https://f-droid.org/packages/io.github.muntashirakon.AppManager)

## Translations

Help translate [the app strings](https://hosted.weblate.org/engage/app-manager/) and
[the docs](https://hosted.weblate.org/projects/app-manager/docs/) at Hosted Weblate.


[![Translation status](https://hosted.weblate.org/widgets/app-manager/-/multi-auto.svg)](https://hosted.weblate.org/engage/app-manager/)


## Upstream mirrors

[Codeberg](https://codeberg.org/muntashir/AppManager) ·
[GitLab](https://gitlab.com/muntashir/AppManager) ·
[Riseup](https://0xacab.org/muntashir/AppManager) ·
[sourcehut](https://git.sr.ht/~muntashir/AppManager)

## Upstream screenshots

These retained images show App Manager, not the current Sphere interface.

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" height="500dp" /><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" height="500dp" /><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" height="500dp" /><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" height="500dp" /><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" height="500dp" /><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" height="500dp" /><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" height="500dp" /><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.png" height="500dp" /><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/9.png" height="500dp" />

## Build Instructions
See [BUILDING.rst](BUILDING.rst)

## Contributing

See [CONTRIBUTING.rst](CONTRIBUTING.rst)

## Upstream donation and funding

As of September 2024, App Manager is not accepting any financial support until further notice. But
you may still be able to send gifts (e.g., gift cards, subscriptions, food and drink, flowers, or
even cash). Please contact the maintainer at muntashirakon [at] riseup [dot] net for further
assistance.

In addition, the maintainers and contributors of this project DO NOT consent to the creation, sale,
or promotion of tokens, cryptocurrencies, NFTs, or any other financial instruments that claim to
represent this project, its code, or its community. Any such attempts are unauthorized and not
affiliated with this project in any way.

## Credits and Libraries

A list of credits and libraries is available in **About**. App Manager: Muntashir Al-Islam
(@MuntashirAkon) and contributors. InxLocker: Chimioo and contributors, with integrated
GPL-3.0 source notices and licence retained. Sphere modifications: SysopBP.
