# AppSphere — MIUIX themes and Installer Control

## Scope
Implement a selectable multi-theme system modeled after TokenX, and integrate installer-routing capabilities inspired by [Chimioo/InxLocker](https://github.com/Chimioo/InxLocker).

## Theme variants
- One UI 9 Glass (default): translucent cards, compact floating navigation, rounded controls.
- MIUIX: Xiaomi-inspired cards, switches, dialogs and settings layout.
- Material You: Android dynamic colors and Material components.
- AMOLED Black: true black surfaces and AppSphere gold accent.

Keep theme selection persistent; use safe activity recreation where immediate live restyling is not supported. Apply themes across dashboard, application details, Installer Control, settings and dialogs. Preserve accessibility, contrast and upstream functionality.

## Installer Control
1. Display selected installer and discover installed eligible handlers.
2. Allow installation intent redirection and separate uninstall intent redirection.
3. Support confirmation interception for session-based installs where hooks support it.
4. Support an optional explicit target Activity (forced component routing).
5. Show LSPosed activation, module scope and hook availability; do not claim hooks are active based on Root alone.
6. Provide per-feature switches, status, diagnostics and reset to system defaults.
7. Expose Android 14+ permission-path compatibility only as experimental, opt-in functionality.
8. Never bypass user confirmation or silently install/uninstall packages.

## Architecture
Keep the Xposed hook implementation as a separate module from AppSphere's ordinary UI, with a versioned configuration interface and safe defaults. AppSphere must continue to function when LSPosed or hooks are unavailable. Avoid modifying system_server unless explicitly needed and tested. Test Android 17 Samsung compatibility.

## Attribution
InxLocker by Chimioo: GPLv3; retain original copyright/license notices and attribution when reusing code. AppSphere derives from App Manager; preserve existing upstream license and credits. MIUIX is a descriptive style option, not an assertion of affiliation with Xiaomi.

## Verification
Compile debug APK; test theme persistence and recreation; confirm original package operations; verify redirect behavior for APK intents, uninstall intents and session confirmation; verify missing/disabled Xposed module behavior; validate on Android 17.
