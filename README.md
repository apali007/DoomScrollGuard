# DoomScroll Guard

> 📱 **Building the iOS version?** Read [docs/IOS_SPEC.md](docs/IOS_SPEC.md) first.

An Android app that notices when you've been doomscrolling and gently helps you stop.

Instead of hard blocks (which people uninstall), it escalates:

| Level | When (defaults) | What happens |
|---|---|---|
| ⏳ Nudge | 10 min or 150 swipes | Card in the middle of the screen: time, swipes, distance scrolled |
| 🫁 Pause | +5 min | Full-screen breathing pause; "Continue" locked for 10 s |
| 🧭 Intention | +5 min, then every 5 min | Write 3+ words about why you're continuing (15 s lock) |

**Leaving sticks:** Android doesn't let one app close another, so leaving from a pause or
intention screen (via "Leave", Home or Recents) starts a **break**: reopening any watched app
shows a countdown instead of the feed.

**Strictness levels:**

| | 🌱 Gentle | ⚖️ Balanced (default) | 🔒 Strict | 🔥 Hardcore |
|---|---|---|---|---|
| First nudge | 15 min / 200 swipes | 10 min / 150 swipes | 5 min / 75 swipes | 3 min / 45 swipes |
| Next reminders | every 5 min | every 5 min | every 3 min | every 2 min |
| Starts over after away for | 2 min | 5 min | 10 min | 15 min |
| Pause lock | 5 s | 10 s | 20 s | 30 s |
| Intention | 2 words, 10 s | 3 words, 15 s | 5 words, 30 s | 7 words, 45 s |
| Break after leaving | 2 min (skip after 10 s) | 5 min (skip after 30 s) | 5 min (skip after 1 min) | 5 min (skip after 2 min) |

**Breaks keep your place:** coming back after a break continues the escalation instead of
starting over. **Daily goal:** once you're past it, every new visit gets a nudge after 1 minute.

The first table shows Balanced timings. Plus: a "what brings you here?" question every time you open a watched app fresh, daily goal and
streaks, a 7-day chart, and a 9 PM recap.

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
| `engine/Strictness.kt` | Gentle / Balanced / Strict / Hardcore presets, break timer |
| `service/DoomScrollService.kt` | Accessibility service: foreground tracking, scrolls, logging |
| `service/OverlayController.kt` | Draws intervention screens over other apps |
| `data/` | DataStore settings, Room stats |
| `ui/` | Compose UI: onboarding, home/stats, overlay screens |
| `work/` | Daily recap + "protection is off" health check |

## Docs
- [**iOS build spec**](docs/IOS_SPEC.md): start here if you're building the iPhone version
- [Product decisions](docs/DECISIONS.md): why the app works the way it does
- [Privacy policy](docs/PRIVACY_POLICY.md)
- [Play Store submission kit](docs/PLAY_STORE.md)
- [Beta testing guide](docs/BETA_TESTING.md)
