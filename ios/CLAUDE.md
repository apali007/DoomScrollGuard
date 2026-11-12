# DoomScroll Guard: iOS

Native **Swift + SwiftUI** app built on Apple's **Screen Time API** (FamilyControls,
ManagedSettings, DeviceActivity). It's the iOS version of the Android app in the repo root.
Same product and rules, adapted to what iOS allows.

**The spec is `../docs/IOS_SPEC.md`. Read it before any work.** It says what to keep, adapt
and drop on iOS. Also follow the root `../CLAUDE.md` (privacy, tone, workflow) and
`../docs/DECISIONS.md` (why things are the way they are).

## Status
Not started: no Xcode project yet. **First task: the 1-week proof of concept**, on a real
iPhone:
1. Choose apps → get a notification after X minutes of use.
2. Pause shield with Leave / Continue, where Continue is locked for N seconds.
3. Break: leaving shields the apps, and "Open anyway" works only after the skip time.

Report what works and what doesn't before building further.

## Project layout (create it like this)
```
ios/
├── CLAUDE.md                     ← this file
├── DoomScrollGuard.xcodeproj
├── Core/                         ← local Swift package: pure rules, no Apple Screen Time imports
│   ├── Package.swift
│   ├── Sources/Core/             ← Strictness, ladder/session rules, break timer, shared models
│   └── Tests/CoreTests/          ← mirror ../app/src/test/.../engine/*Test.kt
├── DoomScrollGuard/              ← main app target (SwiftUI): onboarding, home, settings, intention screen
├── Monitor/                      ← DeviceActivityMonitor extension: thresholds → notifications/shields
├── ShieldConfiguration/          ← ShieldConfigurationDataSource extension: shield text, colours, buttons
├── ShieldAction/                 ← ShieldActionDelegate extension: Leave / Continue / Open anyway
└── Report/                       ← DeviceActivityReport extension: usage stats and charts
```

**Why a separate `Core` package:** the rules can be unit-tested on a Mac with `swift test`, with
no iPhone needed. Keep all timing and escalation logic there, and keep extensions thin.

## Identifiers
Confirm these with the product owner before creating the project; they're hard to change later.
- Bundle ID: `com.doomscrollguard.ios` (extensions: `.monitor`, `.shieldconfig`, `.shieldaction`,
  `.report`)
- App Group: `group.com.doomscrollguard.ios`. Shared settings and state between the app and all
  extensions go here.
- Apple Developer team: **the product owner's account.** The developer is added as a team member.
- Entitlements: Family Controls on the app and every extension that needs it, plus App Groups.

## Commands
```bash
cd ios/Core && swift test                                   # rules: fast, on the Mac
xcodebuild -project ios/DoomScrollGuard.xcodeproj -scheme DoomScrollGuard \
  -destination 'generic/platform=iOS' build                 # compile check
```
**Screen Time features must be tested on a real iPhone.** The Simulator doesn't run them
reliably. Say in each PR which device and iOS version it was tested on.

## Rules
1. **Match Android's numbers exactly.** Port `../app/src/main/java/com/doomscrollguard/engine/Strictness.kt`
   into `Core`. Port the Android unit tests (`../app/src/test/.../engine/*Test.kt`) as Swift tests
   too; they are the precise spec of the rules. If a rule can't apply on iOS (e.g. swipe counts),
   say so in a code comment pointing to the relevant IOS_SPEC section.
2. **Time only.** iOS can't see swipes or scrolling. Don't try to work around this.
3. **No third-party dependencies or network calls.** No analytics, crash reporting or ads. The
   App Store privacy label must be "Data Not Collected".
4. **Extensions are tiny.** They have very small memory limits: no heavy frameworks, large
   assets or SwiftUI-heavy work in Monitor/ShieldAction. Read and write small values via the
   App Group.
5. **Verify Apple API behaviour** against current Apple documentation and on device. The spec was
   written from the Android app and may need adjusting. If you deviate from the spec, explain why
   in the PR and update `../docs/IOS_SPEC.md` (and `../docs/DECISIONS.md` if it's a product
   decision).
6. **Same wording and tone** as Android: copy text from `../app/src/main/java/com/doomscrollguard/ui/overlay/*.kt`.
   Colours: Calm `#5B8DEF`, CalmDeep `#0E1726`, Leaf `#3DBE8B`, OverGoal `#E5735C`.
7. **Don't modify Android code** (`../app/`) from iOS work. Open an issue instead.
8. **Never commit** certificates, provisioning profiles, `.p12` / `.mobileprovision` files or
   anything from `xcuserdata/`.

## Workflow
- Branch names: `ios/<topic>` (e.g. `ios/break-shield`). One focused PR per GitHub Issue.
- PR description: what changed, why, how it was tested (device + iOS version), and screenshots
  of any new screen or shield.
- Before opening a PR: `swift test` passes in `Core`, and the app builds.
