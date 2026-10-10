<!-- SPDX-License-Identifier: GPL-3.0-or-later OR CC-BY-SA-4.0 -->
# Sphere appearance and installer status

Current baseline: **4.2.2 (454)**. This supersedes the early MIUIX/InxLocker foundation plan.

## Implemented

- AMOLED edition of the uploaded gold globe, five-density icons, black launcher background,
  themed vector globe and Sphere splash branding.
- Five labelled navigation icons with scrolling hide/show and long-press shortcuts.
- Wallpaper selection, persistent access, dimming/brightness controls and live preview.
- Shared Material, True glass and Soft contrast theme engine with accents and day/night colours.
- Bounded backdrop rendering, Android 12+ blur, opacity, reduced transparency and solid fallbacks.
- Grouped settings, single/tablet panes, navigation animation and haptic controls.
- Integrated InxLocker engine with native installer/uninstaller and activity/source controls.
- Opt-in session/uninstall routing, module-service status, API 102 hot reload, reset and logging.
- Power/session controls, lifecycle cleanup and Android 17 navigation drawable crash repair.
- Separate Sphere package IDs and current cumulative release notes.

The earlier MIUIX preset is now named **Soft contrast**. This remains native styling,
not a dependency on the full MIUIX framework. Full InxLocker UI parity is not implied.

## Device verification

- Launcher masks/themed icons, first splash, large fonts, landscape and tablet layout.
- Wallpaper persistence and visual dimming/brightness after recreation and reboot.
- True glass scrolling and solid fallbacks during Battery Saver or reduced transparency.
- Root/ADB startup, session shutdown, auto-lock and measured battery use on Samsung Android 17.
- Module activation/scope, handler availability, session/uninstall routing and hot reload.
- Experimental Android 14+ path access and OEM-specific ColorOS methods only where supported.

Use the [feature guide](APPSPHERE_FEATURES.md) and
[Installer Control setup](appsphere-installer-control.md) for current capabilities.

## Attribution

Sphere derives from [App Manager](https://github.com/MuntashirAkon/AppManager) by
Muntashir Al-Islam and contributors. The integrated engine derives from
[InxLocker](https://github.com/Chimioo/InxLocker) by Chimioo and contributors;
its GPL-3.0 licence and source notices are retained. Sphere modifications are by SysopBP.
