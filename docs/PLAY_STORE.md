# Google Play submission kit

Everything you need to publish, in the order Play Console asks for it.
Upload file: `app/build/outputs/bundle/release/app-release.aab` (build with `./gradlew bundleRelease`).
The app targets Android 16 (API 36), which meets Play's target-SDK rule as of late 2026. Play
raises the rule every year (usually by 31 August); bump `targetSdk` when Play Console warns.

> **Heads-up on review:** Google reviews every app that uses the Accessibility API closely.
> Apps that aren't accessibility tools must explain their use, show a prominent in-app disclosure,
> and get consent first. Doom Breaker does all of this (onboarding's "Your privacy" step), but
> approval isn't guaranteed. Expect questions or a rejection on the first try. Read the current
> policy before submitting: Play Console Help → "Use of the AccessibilityService API".

---

## 1. Before you start
- [x] Google Play developer account (one-time $25): https://play.google.com/console (personal account; identity verification in progress)
- [x] Privacy policy hosted at https://sites.google.com/view/doombreakerprivacy/home (Google Sites, owned by the developer account). Keep it in sync with `docs/PRIVACY_POLICY.md`.
- [x] **Back up `release.jks` + `keystore.properties`.** (Done: copy in the owner's Google Drive.) Opt in to Play App Signing (recommended)
      so Google holds the app-signing key and yours becomes the upload key.

## 2. Store listing

**App name** (30 chars max)
> Doom Breaker: Stop Doomscrolling

**Short description** (80 chars max)
> Gentle reminders that help you stop doomscrolling and take your time back.

**Full description**
> Lost an hour to your feed again? Doom Breaker notices when you've been scrolling too long
> and helps you stop, without blocking anything.
>
> HOW IT WORKS
> 👋 A quick "What brings you here?" when you open a feed
> ⏳ A gentle nudge after a few minutes of scrolling
> 🫁 A full-screen breathing pause if you keep going
> 🧭 Then you write down what you're looking for before you continue
> 🌿 Leave, and a short break keeps you from jumping straight back in
>
> You're always in control. You can keep scrolling, it just takes a moment of thought. That
> moment is often all it takes to break the autopilot.
>
> FEATURES
> • Works with Instagram, YouTube, TikTok, Facebook, X, Reddit, Snapchat, or any app you choose
> • Daily goal, streaks, and a 7-day chart of your feed time
> • Four strictness levels:
>   🌱 Gentle: first nudge after 15 minutes
>   ⚖️ Balanced: after 10 minutes
>   🔒 Strict: after 5 minutes
>   🔥 Hardcore: after 3 minutes
> • A daily goal with teeth: once you pass it, every visit gets a nudge after 1 minute
> • "How it works" shows exactly when each reminder appears, with a live preview of each
> • Evening recap of your day
>
> PRIVATE BY DESIGN
> Doom Breaker uses Android's Accessibility Service only to see which app is open and that
> you're scrolling. It cannot read anything on your screen. It has no internet access, so nothing
> ever leaves your phone. No accounts, no ads, no tracking.

**Category:** Health & Fitness (alternative: Productivity)
**Tags:** digital wellbeing, screen time, focus

**Graphics checklist**
- [x] App icon 512×512 PNG: `docs/store/icon-512.png` (the grass logo)
- [x] Feature graphic 1024×500 PNG: `docs/store/feature-graphic-1024x500.png` (source: `feature-graphic.svg`; render at exactly 1024×500, e.g. with headless Chrome)
- [x] Phone screenshots (6, 1080×1920 with captions): `docs/store/screenshots/play/01–06.png`.
      Upload them in this order. To add more later (the nudge, pause and break "See it"
      previews are the strongest), drop raw screenshots in `screenshots/originals/` (gitignored)
      and frame them the same way.

## 3. App content

**Privacy policy URL:** https://sites.google.com/view/doombreakerprivacy/home

**Ads:** No ads.

**App access:** All functionality available without login. Tell reviewers:
> No login required. To test: complete onboarding and enable Doom Breaker in Settings →
> Accessibility. Every reminder screen can be seen instantly: Home → "How it works" → "See it".
> For the live flow, choose Hardcore, add YouTube under "Apps to watch", open it and keep
> scrolling: a nudge appears after 3 minutes (or 45 swipes), a full-screen pause 2 minutes
> later, then the intention screen. Tapping "Leave" starts a 5-minute break.

**Content rating:** Complete the questionnaire. No violence, user content, gambling, etc.
Expected rating: Everyone.

**Target audience:** 18+ (simplest; avoids Families policy requirements). 13+ is also reasonable.

**Data safety form:**
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all user data encrypted in transit? | Not applicable (no data transmitted) |
| Do you provide a way for users to request data deletion? | Data is stored only on device; uninstalling or clearing storage deletes it |

> Note: Google defines "collect" as transmitting data off the device. Doom Breaker has no
> internet permission, so the answer is No.

**Accessibility API declaration** (Play Console → App content → Accessibility API):
- Is your app an accessibility tool? **No**
- Description of use:
> Doom Breaker is a digital-wellbeing app that helps users reduce compulsive scrolling.
> The AccessibilityService listens only for TYPE_WINDOW_STATE_CHANGED (to know which app is in
> the foreground) and TYPE_VIEW_SCROLLED (to count scrolling in apps the user selected). It is
> configured with canRetrieveWindowContent=false and cannot read screen content. When the user
> exceeds their chosen limits, the service displays a reminder overlay
> (TYPE_ACCESSIBILITY_OVERLAY) and can return the user to the home screen if they choose
> "Leave". After leaving, reopening a watched app during a short break shows a countdown
> overlay; the user can always switch the service off. The app has no INTERNET permission; no data leaves the device. Users see a prominent
> disclosure and must tap "I agree" during onboarding before being sent to enable the service.
- **Video:** Play requires a short video showing the disclosure and the feature. Record the
  phone screen (Quick Settings → Screen record): onboarding privacy step → "I agree" → enabling
  the service → scrolling YouTube until the nudge appears (Hardcore makes this quick) → Home →
  "How it works" → "See it" for the pause, intention and break screens. Upload to YouTube as
  unlisted and paste the link.

**Health apps declaration** (if asked because of the Health & Fitness category): not a medical
app; no health data collected.

## 4. Release
1. Start with **Internal testing**: add your own and friends' Google accounts as testers (up to 100).
2. Upload the `.aab`, write release notes ("First release"), roll out.
3. New personal developer accounts must run a **closed test with at least 12 testers for 14 days**
   before they can apply for production. Recruit friends early (see `BETA_TESTING.md`).
4. Then apply for production access and promote the release.
