# DoomScroll Guard for iOS: build spec

This repo is the **Android** app (Kotlin). It's the reference for *what* the iOS app should do,
not code to port. This document says what to build on iOS, what to adapt, and what to drop
because iOS doesn't allow it.

> ⚠️ **Read this first:** iOS can't do everything the Android app does. Android watches scrolling
> through the Accessibility service and draws its own screens over other apps. iOS allows neither.
> The iOS app must be built on Apple's **Screen Time API**. Apple changes these APIs between iOS
> versions, so **check everything below against the current Apple documentation** before you
> commit to a design.

---

## 1. What the app is

An app that notices when someone has been doomscrolling and helps them stop, using **friction
that escalates**, not hard blocks (people uninstall hard blockers).

The core loop:

1. **Opening question:** "What brings you to Instagram?"
2. **Nudge:** "You've been scrolling 10 min."
3. **Pause:** a full-screen "Take a breath", with "Continue" locked for a few seconds.
4. **Intention:** to keep going, write a few words about why.
5. **Break:** choosing to leave locks the watched apps for a few minutes, so they can't just
   reopen them.

The user picks a **strictness level** that controls all the timings. Everything stays on the
device: no accounts, no servers, no analytics.

Product reasoning is in [`DECISIONS.md`](DECISIONS.md). Please read it; it explains features
that may look odd at first.

---

## 2. Apple setup you need (before writing code)

| Need | Notes |
|---|---|
| Mac with Xcode (latest) | |
| **A real iPhone** | Screen Time features don't work properly in the Simulator. |
| Apple Developer Program ($99/yr) | |
| **Family Controls entitlement** (`com.apple.developer.family-controls`) | Works in development builds. **To ship on the App Store / TestFlight you must request the distribution entitlement from Apple** via their request form. Approval can take weeks; **apply early**. |
| iOS 16+ | Needed for `.individual` authorization (a person limiting their own use). |
| App Group | So the app and its extensions can share settings and state. |

### Apple frameworks you'll use
- **FamilyControls:** authorization (`AuthorizationCenter.requestAuthorization(for: .individual)`)
  and the app picker (`FamilyActivityPicker` → `FamilyActivitySelection`).
- **ManagedSettings:** applying **shields** (Apple's block screen) to the chosen apps.
- **DeviceActivity:** monitoring usage time and firing events at thresholds
  (`DeviceActivityCenter`, `DeviceActivityMonitor` extension).
- **ManagedSettingsUI:** `ShieldConfigurationDataSource` extension to customise the shield's text,
  icon, colours and buttons, plus a `ShieldActionDelegate` extension to handle button taps.
- **DeviceActivityReport** extension: showing usage stats (Apple keeps raw usage data inside this
  extension).
- **UserNotifications:** local notifications (nudge, recap).
- **AppIntents:** for the opening question via a Shortcuts automation (see 5.1).

### Constraints to design around
- App identities are **opaque tokens**; the main app can't read app names or bundle IDs from
  the picker selection. Use `Label(token)` in SwiftUI to display them.
- Extensions have **very small memory limits**. Keep monitor and shield extensions tiny.
- DeviceActivity thresholds measure **cumulative usage within the monitored interval**, not
  "continuous session time". See section 4.
- A shield's buttons can't directly open your app. The common workaround is to post a local
  notification that the user taps.
- Developers widely report DeviceActivity events firing late or inconsistently. **Test on
  device, early.**

---

## 3. Strictness levels (same numbers on both platforms)

Source of truth: [`Strictness.kt`](../app/src/main/java/com/doomscrollguard/engine/Strictness.kt).

| | 🌱 Gentle | ⚖️ Balanced (default) | 🔒 Strict | 🔥 Hardcore |
|---|---|---|---|---|
| First nudge after | 15 min | 10 min | 5 min | 3 min |
| Pause, then intention, every | 5 min | 5 min | 3 min | 2 min |
| Starts over after away for | 2 min | 5 min | 10 min | 15 min |
| Pause: "Continue" locked for | 5 s | 10 s | 20 s | 30 s |
| Intention: min words / lock | 2 words / 10 s | 3 words / 15 s | 5 words / 30 s | 7 words / 45 s |
| Break after leaving | 2 min | 5 min | 5 min | 5 min |
| "Open anyway" on break after | 10 s | 30 s | 1 min | 2 min |

Android also triggers the nudge on swipe counts (200 / 150 / 75 / 45). **iOS can't see swipes;
use time only.**

**Daily goal:** default 30 min (user picks 15–120 in steps of 15). Once today's total in watched
apps passes the goal, **every new visit gets its first nudge after 1 minute**, whatever the level.

---

## 4. The session model: how to translate it to iOS

**Android:** a *session* is continuous time in any watched app. Leaving for longer than "starts
over after" resets it. The ladder goes nudge → pause → intention → intention… within a session.
A **break** "holds" the session: the reset countdown only starts when the break ends, so coming
back continues at the same step.

**iOS can't see** when the user leaves an app, so approximate:

- Treat a **session as "usage since monitoring last started"**. Start a `DeviceActivity`
  monitoring interval with events at the ladder thresholds, for example on Balanced:
  `nudge @ 10 min`, `pause @ 15 min`, `intention @ 20 min`, `intention @ 25 min`, …
- **Restart monitoring** (a fresh session) when:
  - the day starts, and
  - after enough idle time. A reasonable approximation: schedule a check, or restart when the
    opening question fires after the level's "starts over" time since the last event.
- **When a break ends,** don't reset the ladder. Re-register events so the next one is the next
  ladder step, measured from now.

It's fine if iOS sessions are coarser than Android's. **What matters:** escalation, breaks that
stick, and not being trivially bypassable.

---

## 5. Feature by feature

Legend: ✅ same as Android · 🔁 adapt · ❌ drop

### 5.1 Opening question 🔁 *(highest priority: the feature with the strongest evidence)*
**Android:** when a watched app is opened fresh, a full-screen overlay asks "What brings you to
{app}?" with three choices:
- **Something specific:** normal timing.
- **Just browsing:** the first nudge comes **twice as fast** (×0.5).
- **Never mind, close it:** goes home.

**iOS:** use a **Shortcuts personal automation**, the approach used by apps like *one sec*:
- The user creates an automation "When {App} is opened → run *DoomScroll Guard: Check in*", an
  App Intent your app provides. Guide them through it step by step in onboarding, with
  screenshots.
- The intent opens your app on the question screen.
  - "Something specific" / "Just browsing": write the choice to the App Group, then send the user
    back to the app. Use a deep link (`instagram://` etc.) where one exists; otherwise ask them to
    switch back.
  - **Loop guard:** reopening the target app fires the automation again. Store "allowed until
    {now + 2 min}" in the App Group and have the intent return immediately while that's set.
- "Never mind": stay in your app or show a calm screen.

### 5.2 Nudge 🔁
**Android:** a card in the centre of the screen, shown for 8 s, that the user can dismiss:
> **⏳ 12 min on Instagram**
> 184 swipes · 14 m of feed. Still enjoying this, or just scrolling?
> *(if past the goal, in red)* You're past your 30 min daily goal (today: 47 min).

**iOS:** a **local notification** from the `DeviceActivityMonitor` extension when the nudge
threshold is reached:
> **12 min on Instagram**: still enjoying this, or just scrolling?

Drop swipes and distance; iOS can't measure them. Count "left within 60 s of a nudge" if you can
infer it; otherwise skip that stat.

### 5.3 Pause 🔁
**Android:** a full-screen dark screen with a breathing circle (4 s in, 4 s out):
> **Take a breath.**
> 12 min on Instagram
> Is this how you want to spend your time right now?
> **[Leave Instagram]** · Continue scrolling *(locked for N seconds, counting down)*

**iOS:** apply a **shield** to the watched apps. Customise it in `ShieldConfigurationDataSource`:
- title "Take a breath."
- subtitle "Is this how you want to spend your time right now?"
- primary button **"Leave"**, secondary button **"Continue"**

The shield can't animate or count down, so implement the lock in `ShieldActionDelegate`:
- **Leave:** start a break (5.5), respond `.close`.
- **Continue:** if less than the level's lock time has passed since the shield appeared, respond
  `.none` (it stays). Change the subtitle to "Take a few more seconds…" if you can refresh it.
  Otherwise remove the shield and respond `.defer`.

### 5.4 Intention 🔁
**Android:** a full screen.
> **🧭 17 min on Instagram**
> What are you looking for right now?
> Write it down to keep going. If you can't name it, maybe it's time to stop.
> [text box: "e.g. replying to my friend's message"]
> **[I'm done, leave Instagram]** · Continue *(needs ≥ N words and a lock of N seconds)*

**iOS:** a shield can't hold a text field.
- Shield: title "What are you looking for?", primary **"I'm done"** (starts a break), secondary
  **"Write my reason"**.
- "Write my reason" posts a local notification. Tapping it opens your app on the intention
  screen, a SwiftUI version of the Android screen.
- After a valid reason (word count + lock), remove the shield and tell the user to go back to
  the app.
- Store the reason in the app's local log, like Android does.

### 5.5 Break ✅ *(core: this is what makes leaving stick)*
**Android:** after **leaving from a pause or intention** (by button or by Home/Recents), all watched
apps are locked for the level's break time. Reopening one shows:
> 🌿 **Break time**
> You chose to step away. Instagram will be back in **4:32**
> Stretch, drink some water, look out a window. The feed will still be there.
> **[OK, go home]** · Open anyway *(after the skip time)*

**iOS:** this maps well to shields:
- On leave, keep the shield on and start a `DeviceActivity` schedule for the break length. In
  `intervalDidEnd`, remove the shield.
- Shield text: "Break time 🌿", subtitle "Back at 3:45 PM". **Use an end time, not a
  countdown;** shields can't update live.
- Secondary button **"Open anyway":** honour it only after the level's skip time
  (`.none` before that, `.defer` + remove shield after).
- After a break, **don't reset the ladder** (section 4).

### 5.6 Strictness levels ✅
A picker in settings and onboarding, with the same four levels, numbers and copy (section 3).
Each level shows a one-line summary, for example: *"Nudge after 5 min, reminders every 3 min.
Leaving starts a 5 min break (skippable after 1 min)."*

### 5.7 "How it works" section ✅
The home screen lists every step with **the timings of the user's current level** and a
**"See it"** preview button for each screen. Users asked for this, so keep it.

### 5.8 Daily goal ✅
A slider from 15 to 120 min (step 15), default 30. Add a `DeviceActivityEvent` at the goal
threshold on a daily schedule. When it fires, set an "over goal" flag; new sessions then use a
1-minute nudge.

### 5.9 Stats 🔁
**Android shows:**
- today's total vs. goal (progress bar, red when over)
- a per-app breakdown
- "You stepped away X of Y times you were reminded 👏"
- a 7-day bar chart (green = within goal)
- a streak of days within goal

**iOS:** Apple restricts raw usage data. Build the charts in a **DeviceActivityReport
extension**. Count interventions and "stepped away" yourself (you control those), and keep them
in the App Group.

### 5.10 Daily recap 🔁
A 9 PM local notification:
> Today: 47 min in feeds · you stepped away 4 of 6 times 👏

If exact minutes aren't available outside the report extension, use your own intervention counts
or a generic message.

### 5.11 Protection-off alert 🔁
If Screen Time authorization is revoked (`AuthorizationCenter.authorizationStatus`), show a
banner in the app and send a reminder notification at most once a day.

### Dropped on iOS ❌
- Swipe counts and distance scrolled (no API).
- Overlays drawn over other apps; use shields and notifications instead.
- Android-only setup: Accessibility, "Allow restricted settings", battery-optimisation help.

---

## 6. Onboarding (iOS)

1. **Welcome:** "Take your time back."
2. **How it works:** the steps from section 1.
3. **Privacy:** "We use Apple's Screen Time to see how long you spend in the apps you choose. We
   can't see what's on your screen. Nothing leaves your phone." Then request Family Controls
   authorization.
4. **Pick apps** with `FamilyActivityPicker`.
5. **Opening question setup:** a step-by-step Shortcuts automation guide. Allow skipping.
6. **Strictness + daily goal**, then ask for notification permission.

Default **no apps selected**. Users choose their own; don't pre-select Instagram etc.

---

## 7. Look and feel

| Token | Value | Used for |
|---|---|---|
| Calm | `#5B8DEF` | primary, breathing circle |
| CalmDeep | `#0E1726` | full-screen backgrounds, shield background |
| Leaf | `#3DBE8B` | "Leave" buttons, within-goal |
| OverGoal | `#E5735C` | over-goal warnings |

Tone: calm, warm and **never shaming**. The user is always in control; the app adds a moment of
thought, not punishment. The icon is in `app/src/main/res/drawable/ic_launcher_foreground.xml`:
a phone with a feed and a green pause badge.

---

## 8. Privacy requirements (non-negotiable)

- No accounts, no servers, no analytics or crash SDKs that send data.
- All data on device (App Group storage).
- The App Store privacy label should be **"Data Not Collected"**.
- Mirror [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md), replacing the Android Accessibility wording
  with Screen Time.

---

## 9. Suggested build order and "done" checks

**Milestone 1: blocking works**
- [ ] Authorization + app picker + App Group set up
- [ ] A DeviceActivity event at a threshold sends a notification (nudge) on a real device
- [ ] A shield appears on chosen apps with custom text

**Milestone 2: the ladder and breaks**
- [ ] Pause shield with Leave/Continue and the lock
- [ ] Break: leaving shields the apps for the break length, "Open anyway" only after skip time,
      shield removed at the end
- [ ] Coming back after a break continues the ladder
- [ ] All four strictness levels with the numbers in section 3

**Milestone 3: reflection**
- [ ] Intention flow (shield → notification → app screen → reason saved → unshield)
- [ ] Opening question via Shortcuts automation, with the loop guard
- [ ] Daily goal → 1-minute nudges once over

**Milestone 4: polish**
- [ ] Onboarding, "How it works" with previews, stats/report extension, 9 PM recap
- [ ] Authorization-revoked alert
- [ ] Privacy policy + App Store submission (with the distribution entitlement)

**Acceptance test (real iPhone, Balanced):** pick one app, use it continuously:
- a nudge notification at about 10 min
- the pause shield at about 15 min, where "Continue" doesn't work for 10 s
- choosing **Leave**, then reopening the app, shows the break shield
- "Open anyway" works only after 30 s
- after the break, the next step is the intention, not a fresh nudge

---

## 10. Questions? Where to look in the Android code

| Topic | File |
|---|---|
| Session and escalation rules | `app/src/main/java/com/doomscrollguard/engine/SessionEngine.kt` |
| Strictness numbers, break timer | `engine/Strictness.kt` |
| How it all connects (break start, goal, opening question) | `service/DoomScrollService.kt` |
| Screen layouts and wording | `ui/overlay/*.kt` (Nudge, Pause, Intention, Break, MindfulOpen) |
| Home, "How it works", settings | `ui/home/*.kt`, `ui/MainActivity.kt` |
| Onboarding | `ui/onboarding/OnboardingScreen.kt` |
| Behaviour examples as tests | `app/src/test/java/com/doomscrollguard/engine/*Test.kt` |

The unit tests are the most precise description of the rules; read them as a spec.
