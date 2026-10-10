# InxLocker engine in Sphere

Derived from https://github.com/Chimioo/InxLocker at b783e91447005b9d2d6922670bd18986cb7e9d0a. Copyright belongs to Chimioo and the upstream contributors. Licensed under GPL-3.0 (see LICENSE).

The upstream hook engine, intent policy, redirection and hot-reload code are retained. Compose-specific preferences and UI are replaced by event-driven SharedPreferences and Sphere's native settings. Debug intent logging is off by default. No monitoring service or polling timer is required for installer routing.

Enable Sphere in a modern LSPosed manager supporting libxposed API 101 or newer, scope System Framework and the source apps, then restart scoped processes. API 102 additionally supports hot reload. Settings are shared through libxposed's remote-preferences service. A missing service is shown as inactive, never as working global routing. Experimental path-permission and ColorOS controls are disabled by default.
