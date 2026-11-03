# Beta testing with friends

The fastest way to learn whether DoomScroll Guard actually helps is to have real people use it
for a week.

## Option A: share the APK directly (today, no Play account needed)

1. Build: `./gradlew assembleRelease`
2. Send `app/build/outputs/apk/release/app-release.apk` (about 1.3 MB) via Google Drive, WhatsApp,
   email, etc.
3. Send them these steps:

> **Installing DoomScroll Guard**
> 1. Open the APK on your Android phone. If asked, allow installing from this source.
> 2. Open DoomScroll Guard and follow the setup.
> 3. When you reach "Turn on protection": if the switch in Accessibility settings is greyed out,
>    go back, tap **Open app info → ⋮ (top right) → Allow restricted settings**, then try again.
> 4. Samsung / Xiaomi / OnePlus etc.: follow the "Keep protection running" card so your phone
>    doesn't switch it off.
> 5. Use your phone normally for a week.

## Option B: Play Store internal / closed testing
See `PLAY_STORE.md`. Testers install from a Play link, with no "unknown sources" or
restricted-settings hassle. Personal developer accounts need 12+ testers in a closed test for
14 days before going public, so this doubles as that requirement.

## What to ask testers after a week

1. Roughly how much time did you spend in feeds before vs. now? (They can check the 7-day chart.)
2. When the **nudge** appeared, did you usually stop? Why / why not?
3. Did the **pause** or **intention** screen ever annoy you enough to want to uninstall?
4. Was a reminder ever shown at a bad moment (messaging, watching something on purpose)?
5. Did protection ever turn itself off? (Phone brand/model?)
6. Which feature helped most: nudge, pause, intention, break, stats, recap? Which strictness level did you use?
7. Would you keep using it? Would you recommend it?

Also ask them to screenshot the home screen stats (Today + Last 7 days). Those numbers show
the actual impact.

## Before sending to friends
The release APK is signed with a **different key** than the debug build on your own phone, so
installing it over the debug app fails. To switch your phone to the release build: uninstall
the debug app first (this deletes your stats), then install the release APK and redo setup.
