<!-- SPDX-License-Identifier: GPL-3.0-or-later OR CC-BY-SA-4.0 -->
# Sphere 4.2.2 — features and setup

Sphere is SysopBP's independent App Manager fork. **4.2.2 (454)** collects the current
interface, installer and backend modifications. AppSphere remains in some existing
preference and workflow names; the installed app is named **Sphere**.

## AMOLED icon pack and identity

The gold-globe artwork from the uploaded `AppSphere_Icon_Pack.zip` was adapted into
an AMOLED edition with pure-black surroundings and no outer rounded-card frame.
The approved master is `arts/sphere/amoled-master.png`. Legacy icons at five densities,
adaptive launcher artwork, the authentication splash and store artwork are generated
by `scripts/generate_sphere_icons.py`. Android themed icons use a separate vector globe
instead of the opaque image. Both the initial Android splash and authentication screen
use the Sphere artwork.

The floating bar has labelled icons for Home, Apps, Installer, Running and Settings.
Installed apps retain their own icons. Theme accents are user-selectable; the artwork's
gold does not force every interface control to use gold.

## Dashboard and floating navigation

- **Home:** connection summary, memory/storage metrics, search, filters and tool shortcuts.
- **Apps:** the inherited installed-app list, sorting, filtering and multi-selection.
- **Installer:** the integrated Installer Control screen.
- **Running:** the original running-applications screen.
- **Settings:** grouped settings with single-pane and tablet layouts.

The bar hides on downward scrolling and returns on upward scrolling, at the top,
after tab changes and after selection ends. Bottom padding keeps it clear of list
content. Long-press **Apps** for filters or **Home** to refresh status and metrics.
Refresh buttons read current metrics; they are not continuous heartbeat monitors.
App cards have compact spacing, truncated metadata and clearer UID spacing.
App-details headers and tabs were refreshed while retaining the underlying functionality.

## Themes, wallpaper and feedback

Open **Settings → Appearance → Theme engine**.

| Setting | Behaviour |
| --- | --- |
| Material | Solid Material surfaces using semantic theme colours. |
| True glass | Frosted dashboard, navigation and grouped settings surfaces. |
| Soft contrast | Solid, lightly accent-tinted surfaces; replaces the earlier MIUIX-inspired preset label. |
| Accent | System, Violet, Blue, Mint or Amber, with day/night foreground colours. |
| Backdrop blur | 0–40 dp; available with supported live glass rendering. |
| Surface opacity | 40–90%; higher values obscure more of the background. |
| Reduce transparency | Uses solid surfaces for readability and lower rendering cost. |
| Apply/reset | Applies the theme to activities; reset restores glass/system accent and keeps the wallpaper. |

A live preview shows the chosen settings. Blur requires Android 12+ and hardware
acceleration. Battery Saver, reduced transparency, older Android versions or unavailable
hardware rendering use solid fallbacks. Backdrops use bounded reusable buffers and
follow scrolling content; text, icons, selection and ripples stay sharp. This is native
rendering, not a MIUIX framework port.

**Custom background** opens an image picker with persistent read access. Enable the
wallpaper and adjust **dimming (0–100%)** and **image brightness (0–200%)** while viewing
the preview. Brightness affects the image, not the phone's display setting. Decoding is
bounded and performed in the background. The main page and settings include wallpaper
views; unrelated inherited tool screens should not be assumed to share every surface.

**Motion and feedback** saves navigation animation and haptic choices. The inherited
night-mode, pure-black, system-font and layout-direction controls remain available.

## Integrated Installer Control

Sphere includes a native settings interface for an InxLocker-derived engine; no separately
installed InxLocker companion is needed. Open **Installer** or the corresponding settings
entry to pick an APK and choose its installer. Missing/incompatible targets offer system
resolution; the fallback explicitly bypasses global routing.

Installer Control also provides installer/uninstaller selection, forced target activities,
explicit-source installer components, optional uninstall/session-confirmation routing,
launcher visibility, logging and reset. System-wide hooks require a compatible modern
libxposed/LSPosed service, module activation and scope. Root alone does not activate them.
The status reflects the connected service; it does not guarantee that every OEM hook
works. **Hot reload** requires API 102 capability; otherwise restart scoped processes.
Path-access and ColorOS controls are experimental and off by default.
See [Installer Control setup](appsphere-installer-control.md).

## Power and background activity

Open **Settings → Power and background activity** for battery-optimization status,
Android app battery settings, persistent authenticated sessions and requested versus
detected backend mode. Disabling persistent sessions stops the session-monitoring
service; privileged jobs retain their own lifecycle. The service cancels its background
tasks and screen-lock checks when stopped, avoids background activity relaunch and
uses the special-use foreground-service type.

These controls do not establish a measured battery saving. Verify appearance, power
use and background behaviour on the phone.

## Root/ADB and startup repairs

Sphere retains App Manager's privileged and unprivileged modes. Fork changes add startup
log checkpoints, prevent the investigated auto-mode root/ADB blocking path, shorten
service-bind waits and improve component diagnostics/cleanup. Both required binders
are checked before service capability is published.

Local-server JARs are staged in private storage with absolute launcher paths. Extraction
handles compressed assets/missing directories, atomically refreshes stale copies and
supports missing external cache. The Android 17 floating-navigation drawable startup
crash was also repaired. Actual privileged operation still depends on the device and
backend; these changes do not add TokenX's separate Root/System routing architecture.

## Packages and updating

| Build | Package |
| --- | --- |
| Release | `io.github.sysopbp.sphere` |
| Debug | `io.github.sysopbp.sphere.debug` |

Minimum Android: **8/API 26**. Target: **Android 17/API 37**. Java source namespaces
remain unchanged. These package IDs are separate from the original App Manager IDs;
export settings/backups before moving from an old installation. Existing installations
with the same Sphere package can update normally with a compatible signing key.

The Android 17 workflow builds pull-request previews, verifies package/version/label
and uploads **Sphere-Android17-debug**. SDK provisioning also covers tests, lint and
CodeQL. The update popup shows the current cumulative Sphere release notes;
**About → Version/changelog** retains the full upstream history.
See [build instructions](../BUILDING.rst) and [the fork changelog](../CHANGELOG.md).

## Credits

[App Manager](https://github.com/MuntashirAkon/AppManager): Muntashir Al-Islam
(@MuntashirAkon) and contributors. Sphere modifications: SysopBP.
[InxLocker](https://github.com/Chimioo/InxLocker): Chimioo and contributors; the integrated
engine retains GPL-3.0 notices and its licence in `installer-routing/LICENSE`.
Existing copyright and third-party credits remain in the repository and About screen.


### Eclipse Core icons — 4.2.3

The selected Eclipse Core design replaces the gold globe: a dark sphere, silver crescent and wine-red app tiles. Launcher, splash and themed launcher assets match this identity. 24 two-tone vector icons are wired into navigation, dashboard shortcuts, main settings, appearance, theme, power and installer controls. The dock preserves the silver/wine-red palette while selected labels retain theme accents. Third-party package artwork is unchanged.
