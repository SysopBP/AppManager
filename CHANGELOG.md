<!-- SPDX-License-Identifier: GPL-3.0-or-later OR CC-BY-SA-4.0 -->
# Sphere / AppSphere changelog

## 4.2.4 (456) — 10 October 2026

- Give Eclipse Core dedicated adaptive and legacy launcher resources and an explicit launcher alias icon.
- Version Sphere’s own image cache and resolve its icon from the current APK.


## 4.2.3 (455) — 10 October 2026

- Replace launcher and splash with the selected Eclipse Core artwork.
- Add 24 silver/wine-red vector icons to navigation, dashboard and settings controls.
- Preserve two-tone navigation colors and update Android themed icon.
- Update README, feature guide, store metadata and in-app What’s New.


## 4.2.2 (454) — 10 October 2026

This consolidates the fork modifications, including earlier AppSphere work.
Original App Manager release history remains available in About.

### Branding and navigation

- Adapt the uploaded gold globe to AMOLED artwork with pure-black surroundings.
- Generate five-density launcher assets, adaptive safe spacing and a separate themed vector globe.
- Use Sphere art on the initial Android splash and authentication screen; refresh documentation identity.
- Name the app Sphere in both build types and read its actual version in About.
- Use separate `io.github.sysopbp.sphere` / `.debug` packages; minimum Android 8/API 26.
- Add Home, Apps, Installer, Running and Settings dock icons with scroll hide/show.
- Add dashboard backend/memory/storage summaries, search/tools and long-press filters/refresh.
- Improve dock clearance, compact app cards, metadata/UID spacing and app-details headers/tabs.

### Wallpaper and themes

- Add image selection, persistent read access, dimming and image-brightness sliders with preview.
- Add Material, True glass and Soft contrast presets and System/Violet/Blue/Mint/Amber accents.
- Use semantic day/night colours, blur/opacity controls, reduced transparency and theme reset.
- Render bounded reusable backdrops with draw-order-aware sampling and pre-draw refresh.
- Keep foreground text/icons/ripples sharp; use solid fallbacks when glass is unavailable.
- Add grouped theme-aware settings and single/tablet panes; retain motion/haptic controls.

### Installer integration

- Integrate the GPL-3.0 InxLocker engine and modern libxposed remote-preference service.
- Add installer/uninstaller choices, forced activities and explicit-source installer components.
- Add opt-in session/uninstall routing, module status, launcher visibility, logging and API 102 hot reload.
- Disable privileged routing switches without a connected module service; exclude Sphere's own processes.
- Add event-driven settings/cache updates and clear caches on reset.
- Keep in-app APK picking and read grants; explicitly bypass global routing for system fallback.
- Leave path-access/ColorOS compatibility opt-in and experimental.

### Stability and power

- Fix Android 17 startup from an unthemed navigation drawable.
- Add persistent-session and battery settings with requested versus detected backend status.
- Cancel session-service timers/pending lock checks on stop and avoid background activity relaunch.
- Use the special-use session foreground-service type and bounded background wallpaper decoding.
- Retain earlier root/ADB startup log checkpoints, failed-bind handling, shorter waits and cleanup.
- Validate required binders before advertising service capability.
- Stage local-server JARs privately with absolute paths, compressed-asset handling,
  atomic stale-copy refresh and missing-external-cache support.

### Build and documentation

- Target Android 17/API 37; provision the preview SDK paths for APK/tests/lint/CodeQL.
- Support modern libxposed dependencies and record desugaring while preserving hidden API compilation.
- Build pull-request preview APKs and verify packaged ID, label and version before artifact upload.
- Update README, feature/setup guide, installer guide, roadmap, build guide, module guide,
  English app metadata, About description/credits and in-app What's New.
- Show the current cumulative Sphere notes in the update popup; retain historical notes in About.

Device-side appearance, power use and framework/OEM routing compatibility require phone testing.
Credits: App Manager by Muntashir Al-Islam and contributors; InxLocker by Chimioo and
contributors; Sphere modifications by SysopBP. Original licences and notices are retained.
