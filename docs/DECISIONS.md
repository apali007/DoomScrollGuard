# Product decisions

Why DoomScroll Guard works the way it does. Read this before changing behaviour or building
another platform, because several "obvious" improvements were tried or considered and rejected.

## Goal
Reduce compulsive scrolling for people who **want** to scroll less. It's a commitment device:
it helps someone who has decided to change in their weaker moments. It can't force anyone, and
the user can always switch it off.

## Principles
1. **Friction, not blocking.** Hard blockers get uninstalled. Small, escalating friction breaks
   the autopilot while leaving the user in control.
2. **Escalate.** A single fixed popup gets tuned out within days. Each step demands more
   attention: nudge → pause → written intention.
3. **Leaving must stick.** Otherwise every step is one tap from being bypassed.
4. **No shame.** Calm, warm wording. "Is this how you want to spend your time?", never guilt.
5. **Privacy first.** No internet permission, never reads screen content, all data on device.
6. **Simple over clever.** The user asked for fewer, clearer features over smart automation.

## Decisions and their reasons

### The opening question (always on, no toggle)
"What brings you to {app}?" each time a watched app is opened fresh. It's the feature with the
strongest evidence: a 2023 *PNAS* study (Grüning et al.) of "one sec", which adds a short pause
before social apps open, found about **57% fewer opening attempts** over six weeks. The user made
it always on. "Just browsing" halves the time to the first nudge, an honest admission that
earns closer attention.

### Breaks after leaving
On day one of real use, the user bypassed everything: tap "Leave" (which only goes Home),
reopen the app, keep scrolling. **Android (14+) doesn't let one app close another**, so leaving
now starts a break during which watched apps show a countdown instead of the feed. Pressing
Home or Recents on a pause/intention screen counts as leaving too, or it would be a loophole.
Leaving after a mere *nudge* does not start a break; nudges stay gentle.

### Strictness levels instead of many settings
The user asked for "loose / moderate / strict" style levels. There are four presets, each
stricter in every dimension (enforced by `StrictnessTest`). Strict and Hardcore numbers were
set directly by the user:
- Strict: 5 min nudge, 3 min reminders, 5 min break skippable after 1 min.
- Hardcore: 3 min nudge, 2 min reminders, 5 min break skippable after 2 min.

Hardcore's swipe trigger was raised from 20 to 45 so the nudge doesn't fire long before 3 min.

### Three fixes from a practicality review
1. **Longer reset on stricter levels** (2/5/10/15 min). Previously, leaving for 2 min after a
   nudge reset everything, so the pause/intention/break could be dodged forever.
2. **Breaks hold the session.** The reset countdown starts when the break *ends*, so coming
   back continues the ladder instead of a clean slate.
3. **Daily goal has teeth.** Many short sessions never reached a nudge. Once past the goal,
   every new visit is nudged after 1 minute.

### Removed features
- **Demo mode** (very short timings for testing): removed; the "See it" previews cover demos.
- **Adaptive timing** (reminders sooner if the user often taps Continue): removed at the user's
  request. It made timing unpredictable; levels now mean exactly what they say.
- **Bedtime mode** (2× faster at night): removed at the user's request, for simplicity.
- **Default-watched apps** (Instagram/YouTube/Facebook pre-selected): removed. Users pick
  their own apps; the user doesn't use those apps, and pre-selection confused the list.

### Smaller choices
- **Nudge card in the centre** of the screen (user preference), not the top.
- **"How it works" section** with live timings for the current level and a "See it" preview
  for each screen, because the user said "we don't know" when reminders come.
- **Watching a browser is all-or-nothing.** Watching specific websites would require reading
  the address bar, breaking the "never reads your screen" promise.

## Ideas considered and parked
Build only if real-world testing shows a need:
- **Settings lock:** lowering strictness takes effect only after 24 h, if users lower it in
  weak moments.
- **Pause protection:** a snooze for intentional use of a watched app.
- **"Your reasons" list:** show past intention texts back to the user.
- **Home-screen widget**, **data reset/export**, **stats refresh at midnight**, **translations**.

## Evaluation plan (current)
Two-week personal test on one level (Strict suggested), without changing settings:
1. Record a baseline from Android **Digital Wellbeing** (time in the apps you scroll, before
   install).
2. Watch every app you scroll in, so usage doesn't just move elsewhere.
3. Note moments of annoyance, missed triggers and bypasses.
4. Compare after 2 weeks: about 30% less time or better is a strong result. Repeat with 3–5
   friends.

## Platform decisions
- **Android: native Kotlin + Jetpack Compose.** The core relies on the Accessibility service and
  overlays, which need native code.
- **iOS: native Swift + SwiftUI on Apple's Screen Time API,** not cross-platform. The hard parts
  are platform-specific on both sides and the two apps behave differently. Shared code would be
  mostly "if Android / else iOS". See [`IOS_SPEC.md`](IOS_SPEC.md).
- **Recommended order:** finish the Android test first, then build iOS around the features that
  proved most effective.
