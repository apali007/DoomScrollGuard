# DoomScroll Guard

An Android app that notices when you've been doomscrolling and gently helps you stop.

Instead of hard blocks (which people uninstall), it escalates:

| Level | When (defaults) | What happens |
|---|---|---|
| ⏳ Nudge | 10 min or 150 swipes | Small card at the top: time, swipes, distance scrolled |
| 🫁 Pause | +5 min | Full-screen breathing pause; "Continue" locked for 10 s |
| 🧭 Intention | +5 min, then every 5 min | Write 3+ words about why you're continuing (15 s lock) |

**Leaving sticks:** Android doesn't let one app close another, so leaving from a pause or
intention screen (via "Leave", Home or Recents) starts a **break**: reopening any watched app
shows a countdown instead of the feed.

**Strictness levels:**

| | 🌱 Gentle | ⚖️ Balanced (default) | 🔒 Strict | 🔥 Hardcore |
|---|---|---|---|---|
| First nudge | 15 min / 200 swipes | 10 min / 150 swipes | 5 min / 75 swipes | 1 min / 20 swipes |
| Next reminders | every 5 min | every 5 min | every 3 min | every 1 min |
| Pause lock | 5 s | 10 s | 20 s | 30 s |
| Intention | 2 words, 10 s | 3 words, 15 s | 5 words, 30 s | 7 words, 45 s |
| Break after leaving | 2 min (skip after 10 s) | 5 min (skip after 30 s) | 15 min (no skip) | 30 min (no skip) |

The level table above shows Balanced timings. Plus: daily goal and streaks, 7-day chart, bedtime mode (2× stricter at night), a 9 PM recap,
adaptive timing (reminders come sooner if you keep tapping "Continue"), and an optional
"what brings you here?" prompt when opening a feed.

**Privacy:** the accessibility service runs with `canRetrieveWindowContent="false"`. It only sees
*which app is open* and *that a scroll happened*. The app has no `INTERNET` permission, so all
data stays on the device.

## Build

Requirements: Android Studio (or JDK 17 + Android SDK 35).

```bash
./gradlew testDebugUnitTest      # unit tests (engine, settings, stats)
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # signed, minified APK (needs keystore.properties)
./gradlew bundleRelease          # .aab for Google Play
```

### Release signing
`keystore.properties` and `release.jks` sit in the project root and are **gitignored**.
**Back up both files somewhere safe** (e.g. a password manager). If you lose them you can't
publish updates to the same Play Store listing.

## Install on a phone

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then open the app and follow onboarding. On Android 13+, sideloaded apps need
**App info → ⋮ → Allow restricted settings** before the Accessibility switch can be turned on.
The app shows these steps.

> ⚠️ Don't `adb shell am force-stop com.doomscrollguard`: Android switches the accessibility
> service off when an app is force-stopped. (The app will notify you if this happens.)

## Code map

| Path | What |
|---|---|
| `engine/SessionEngine.kt` | Pure-Kotlin rules: sessions, escalation, cooldowns (unit tested) |
| `engine/GestureAggregator.kt` | Collapses scroll-event bursts into swipes |
| `engine/Adaptive.kt` | Adjusts timing from recent pause outcomes |
| `service/DoomScrollService.kt` | Accessibility service: foreground tracking, scrolls, logging |
| `service/OverlayController.kt` | Draws intervention screens over other apps |
| `data/` | DataStore settings, Room stats |
| `ui/` | Compose UI: onboarding, home/stats, overlay screens |
| `work/` | Daily recap + "protection is off" health check |

## Docs
- [Privacy policy](docs/PRIVACY_POLICY.md)
- [Play Store submission kit](docs/PLAY_STORE.md)
- [Beta testing guide](docs/BETA_TESTING.md)
