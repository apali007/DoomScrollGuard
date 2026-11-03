# Google Play submission kit

Everything you need to publish, in the order Play Console asks for it.
Upload file: `app/build/outputs/bundle/release/app-release.aab` (build with `./gradlew bundleRelease`).

> **Heads-up on review:** Google reviews every app that uses the Accessibility API closely.
> Apps that aren't accessibility tools must explain their use, show a prominent in-app disclosure,
> and get consent first. DoomScroll Guard does all of this (onboarding's "Your privacy" step), but
> approval isn't guaranteed. Expect questions or a rejection on the first try. Read the current
> policy before submitting: Play Console Help → "Use of the AccessibilityService API".

---

## 1. Before you start
- [ ] Google Play developer account (one-time $25): https://play.google.com/console
- [ ] Host the privacy policy at a public URL. Easiest option: paste `docs/PRIVACY_POLICY.md`
      into a GitHub Pages site, Google Sites page, or Notion public page. Fill in your contact email first.
- [ ] **Back up `release.jks` + `keystore.properties`.** Opt in to Play App Signing (recommended)
      so Google holds the app-signing key and yours becomes the upload key.

## 2. Store listing

**App name** (30 chars max)
> DoomScroll Guard: Scroll Less

**Short description** (80 chars max)
> Gentle reminders that help you stop doomscrolling and take your time back.

**Full description**
> Lost an hour to your feed again? DoomScroll Guard notices when you've been scrolling too long
> and helps you stop, without blocking anything.
>
> HOW IT WORKS
> ⏳ A gentle nudge after 10 minutes of scrolling
> 🫁 A full-screen breathing pause if you keep going
> 🧭 Then you write down what you're looking for before you continue
>
> You're always in control. You can keep scrolling, it just takes a moment of thought. That
> moment is often all it takes to break the autopilot.
>
> FEATURES
> • Works with Instagram, YouTube, TikTok, Facebook, X, Reddit, Snapchat, or any app you choose
> • Daily goal, streaks, and a 7-day chart of your feed time
> • Four strictness levels, from Gentle to Hardcore (reminders after just 1 minute)
> • Leaving actually sticks: a short break locks your feeds so you can't just reopen them
> • Evening recap of your day
>
> PRIVATE BY DESIGN
> DoomScroll Guard uses Android's Accessibility Service only to see which app is open and that
> you're scrolling. It cannot read anything on your screen. It has no internet access, so nothing
> ever leaves your phone. No accounts, no ads, no tracking.

**Category:** Health & Fitness (alternative: Productivity)
**Tags:** digital wellbeing, screen time, focus

**Graphics checklist**
- [ ] App icon 512×512 PNG (export from the adaptive icon in Android Studio:
      right-click `res` → New → Image Asset, or screenshot the launcher icon)
- [ ] Feature graphic 1024×500 PNG
- [ ] 2–8 phone screenshots. Suggested: onboarding welcome, home stats, nudge card over a feed,
      breathing pause, intention screen, break countdown, strictness levels. (Use the in-app **Preview** buttons to
      capture the overlay screens without needing a real feed.)

## 3. App content

**Privacy policy URL:** _(your hosted URL)_

**Ads:** No ads.

**App access:** All functionality available without login. Tell reviewers:
> No login required. To test: complete onboarding and enable DoomScroll Guard in Settings →
> Accessibility. Every reminder screen can be seen instantly via the "Preview" buttons on the
> home screen. For the live flow, open YouTube and keep scrolling: a nudge appears after
> 150 swipes or 10 minutes, a full-screen pause 5 minutes later, then the intention screen.

**Content rating:** Complete the questionnaire. No violence, user content, gambling, etc.
Expected rating: Everyone.

**Target audience:** 18+ (simplest; avoids Families policy requirements). 13+ is also reasonable.

**Data safety form:**
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all user data encrypted in transit? | Not applicable (no data transmitted) |
| Do you provide a way for users to request data deletion? | Data is stored only on device; uninstalling or clearing storage deletes it |

> Note: Google defines "collect" as transmitting data off the device. DoomScroll Guard has no
> internet permission, so the answer is No.

**Accessibility API declaration** (Play Console → App content → Accessibility API):
- Is your app an accessibility tool? **No**
- Description of use:
> DoomScroll Guard is a digital-wellbeing app that helps users reduce compulsive scrolling.
> The AccessibilityService listens only for TYPE_WINDOW_STATE_CHANGED (to know which app is in
> the foreground) and TYPE_VIEW_SCROLLED (to count scrolling in apps the user selected). It is
> configured with canRetrieveWindowContent=false and cannot read screen content. When the user
> exceeds their chosen limits, the service displays a reminder overlay
> (TYPE_ACCESSIBILITY_OVERLAY) and can return the user to the home screen if they choose
> "Leave". The app has no INTERNET permission; no data leaves the device. Users see a prominent
> disclosure and must tap "I agree" during onboarding before being sent to enable the service.
- **Video:** Play requires a short video showing the disclosure and the feature. Record the
  phone screen (Quick Settings → Screen record): onboarding privacy step → "I agree" → enabling
  the service → scrolling YouTube until the nudge appears → Preview buttons for the pause and
  intention screens (or keep scrolling and trim the video). Upload to YouTube as
  unlisted and paste the link.

**Health apps declaration** (if asked because of the Health & Fitness category): not a medical
app; no health data collected.

## 4. Release
1. Start with **Internal testing**: add your own and friends' Google accounts as testers (up to 100).
2. Upload the `.aab`, write release notes ("First release"), roll out.
3. New personal developer accounts must run a **closed test with at least 12 testers for 14 days**
   before they can apply for production. Recruit friends early (see `BETA_TESTING.md`).
4. Then apply for production access and promote the release.
