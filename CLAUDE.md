# DoomScroll Guard

A phone app that notices when someone is doomscrolling and helps them stop with **escalating
friction**, not hard blocks:

> opening question → nudge → pause → written intention → break

The user picks a strictness level (Gentle / Balanced / Strict / Hardcore). Everything stays on
the device.

- **Android:** native Kotlin + Jetpack Compose, in the repo root (`app/`). Built and in daily use.
- **iOS:** native Swift + SwiftUI on Apple's Screen Time API, in `ios/`. In progress.

## Read before working
| Doc | When |
|---|---|
| `docs/DECISIONS.md` | **Always**, before changing behaviour: why features exist or were removed. Don't re-add removed features. |
| `docs/IOS_SPEC.md` | Any iOS work. Maps every Android feature to iOS (keep / adapt / drop). |
| `README.md` | Feature summary and strictness table. |
| `docs/PRIVACY_POLICY.md` | Anything touching data, permissions or networking. |

## Rules (both platforms)
1. **Privacy is non-negotiable.**
   - No internet access, no servers, no accounts, no analytics or crash SDKs.
   - Never read screen content: Android runs with `canRetrieveWindowContent=false`, and iOS only
     uses Screen Time tokens.
   - All data stays on the device.
2. **One source of truth for numbers.** Strictness timings live in
   `app/src/main/java/com/doomscrollguard/engine/Strictness.kt`. If they change, update
   `README.md` and `docs/IOS_SPEC.md` (section 3) **in the same change**, and iOS must match.
3. **Tone:** calm, warm, never shaming. The user is always in control; we add a moment of
   thought, not punishment.
4. **Simple over clever.** The product owner prefers fewer, clearer features. Removed on purpose:
   adaptive timing, bedtime mode, demo mode, pre-selected default apps. Ask before adding
   settings or toggles.
5. **Decisions go in `docs/DECISIONS.md`.** When a product decision is made in a session, record
   it there, so the other person's Claude knows too.
6. **Never commit secrets:** `release.jks`, `keystore.properties`, Apple certificates or
   provisioning profiles, API keys. They are gitignored; keep it that way.

## Collaboration workflow
- **Roles:** the product owner decides what the app does, owns Android and reviews and merges.
  The iOS developer owns `ios/`.
- **Tasks are GitHub Issues.** Reference the issue in the branch and the PR.
- **Never push to `main`.** Create a branch (`ios/<topic>`, `android/<topic>`, `docs/<topic>`),
  open a pull request and get it reviewed before merging.
- **Keep PRs small and focused,** one feature or fix each.
- **Stay in your lane.** iOS work doesn't modify Android code, and vice versa. If a change affects
  both platforms (e.g. a new rule), update the docs and open an issue for the other platform.
- **Before opening a PR:** build passes, tests pass, and the PR description says what changed,
  why, and how it was tested (on which device).

## Android
```bash
./gradlew testDebugUnitTest   # unit tests: the rules are specified by engine/*Test.kt
./gradlew assembleDebug       # debug APK
./gradlew assembleRelease     # signed release APK (needs keystore.properties, local only)
```
- Key code:
  - `engine/` holds the pure rules (unit tested).
  - `service/DoomScrollService.kt` is the accessibility service.
  - `service/OverlayController.kt` draws screens over other apps.
  - `ui/` holds the Compose screens.
- **Never run `adb shell am force-stop com.doomscrollguard`.** Android switches the accessibility
  service off when the app is force-stopped.
- The product owner's phone runs the **release-signed** build. Update it with
  `adb install -r app/build/outputs/apk/release/app-release.apk`. A debug build won't install
  over it (signature mismatch) and uninstalling would wipe their stats.
- Bump `versionCode`/`versionName` in `app/build.gradle.kts` for each build you share.

## iOS
- **Start with `ios/CLAUDE.md`** (layout, identifiers, commands, iOS rules), then `docs/IOS_SPEC.md`.
- Needs a real iPhone; Screen Time doesn't work properly in the Simulator.
- The Apple Developer account belongs to the product owner; the developer is added to the team.
- Verify Screen Time API behaviour against current Apple documentation. The spec was written
  from the Android app and hasn't been validated on device yet.
