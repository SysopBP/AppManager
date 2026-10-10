<!-- SPDX-License-Identifier: GPL-3.0-or-later OR CC-BY-SA-4.0 -->
# Sphere Installer Control — integrated InxLocker engine

Updated for **Sphere 4.2.2 (454)**. The earlier independent experimental dialog has been
replaced with native settings backed by integrated InxLocker-derived code. A separately
installed InxLocker app is no longer required.

## APKs selected inside Sphere

Open **Installer → Install APK from storage**. Choose an APK and its preferred installer.
Sphere preserves URI read access and ClipData. If the selected installer is unavailable,
the offered system-resolution fallback includes a routing-bypass marker so global hooks
do not immediately redirect it again. Signing and other inherited installation options
remain available under APK installation options.

## Enable system-wide routing

1. Install Sphere and use a modern LSPosed/libxposed implementation supporting API 101+.
2. Enable Sphere as a module and select the required scope, including System Framework
   and the intended source apps as applicable to your framework/device.
3. Restart the affected scoped processes. Use **Hot reload** only when API 102 capability
   is available; the control is disabled when unsupported.
4. In Sphere's Installer Control, confirm that the module service is connected, select
   an installer and enable routing. A saved installer or root grant alone is not activation.

Settings are shared through libxposed remote preferences. Missing service is displayed
as inactive; relevant routing controls are disabled. Service connection proves the
configuration channel is available, not that every method is supported on an OEM build.

## Controls

| Control | Purpose |
| --- | --- |
| Installer/uninstaller | Select handlers visible to Android; optionally use the installer for uninstall requests. |
| Forced activity | Override automatic activity selection for the chosen installer. |
| Explicit installer activities | Allow redirection when requests explicitly name selected installer components. |
| Uninstall routing | Opt-in forwarding of delete/uninstall intents; the destination must support them. |
| Session confirmations | Opt-in forwarding of CONFIRM_INSTALL/CONFIRM_PERMISSIONS requests. |
| Session path access | Experimental Android 14+ permission-path compatibility during session-info generation. |
| ColorOS compatibility | Experimental hooks for supported OPPO/Realme/OnePlus methods. |
| Logging | Detailed routing diagnostics; off by default. |
| Hide launcher | Hide Sphere's alias; settings remain accessible from the LSPosed module page. |
| Hot reload | API 102 reload capability for running scoped processes. |
| Reset | Clear routing settings/caches, turn off routing and restore launcher visibility. |

Routing is opt-in and depends on the source intent, handler support and framework scope.
It does not imply universal interception or bypassing installer confirmation. Compatibility
options are off by default and require device verification, especially on Samsung Android 17.

## Implementation and attribution

The `installer-routing` module is derived from
[Chimioo/InxLocker](https://github.com/Chimioo/InxLocker), revision
`b783e91447005b9d2d6922670bd18986cb7e9d0a`, with GPL-3.0 notices retained.
Compose-specific UI is replaced by Sphere native preferences. Remote-preference updates
are event-driven; installer routing adds no monitoring service or polling timer.
Sphere's release/debug processes are excluded from routing to avoid self-interception.

App Manager remains credited to Muntashir Al-Islam and contributors; Sphere modifications
are by SysopBP. See the [feature guide](APPSPHERE_FEATURES.md),
[roadmap](APPSPHERE_MIUIX_INXLOCKER_ROADMAP.md) and [changelog](../CHANGELOG.md).
