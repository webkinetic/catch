# Catch — Voice Capture Inbox

A spoken thought → correctly-filed task/event/note. See the original build
brief for full product/architecture rationale.

## Status: usable end-to-end loop with onboarding + tutorial

Built so far:
- Gradle project (version catalog, AGP 8.6, Kotlin 2.0, KSP, Hilt)
- Hilt-wired `CatchApplication`, driving WorkManager via `HiltWorkerFactory`
- Room database (`CatchDatabase`) with `CaptureEntity`, its `CaptureState`
  state machine, `CaptureDao`, and type converters
- `CaptureStructurer` — the AI structuring layer, running on **Gemini**
  (`gemini-2.5-flash`, free tier via a Google AI Studio key), behind an
  interface so the provider can change later without touching call sites
- **Capture trigger + on-device STT** (`capture/`): QS tile
  (`CaptureTileService`) + transparent trampoline (`CaptureActivity`) that
  listens via `SpeechRecognizer`, writes `CAPTURED` to Room the instant
  recognition ends, dismisses. Never awaits a network call (hard rules #1/#4).
  Works from the lock screen.
- **`CaptureRepository` + `StructureCaptureWorker`** — WorkManager glue with
  a network constraint + exponential backoff; advances
  `CAPTURED → STRUCTURING → AWAITING_CONFIRM`/`FAILED`
- **Onboarding + tutorial** (`ui/onboarding/`), wired via Compose Navigation
  (`ui/CatchApp.kt` — single Activity, per the brief's architecture rule):
  - `TutorialScreen` — a 5-page swipeable walkthrough (what Catch does, how
    to add the QS tile, the fallback button, what the state pills mean, and
    an explicit callout that filing isn't built yet)
  - `OnboardingScreen` — paste-your-own Gemini key, with a link straight to
    Google AI Studio and a "do it later" skip. Reachable again anytime from
    the inbox's ⚙ icon; the tutorial is reachable again from the **?** icon
  - First launch with no key goes Tutorial → Onboarding → Inbox; every
    later launch goes straight to Inbox
- **Inbox screen** (`ui/InboxScreen.kt`) — lists every capture live from
  Room with a state pill, confidence, and a FAB that also triggers capture

Not built yet: the confirm screen (captures currently sit at
`AWAITING_CONFIRM` with no way to act on them), external destinations, undo.
Per the build brief, phases land one at a time.

**On the API key:** get one at [aistudio.google.com/apikey](https://aistudio.google.com/apikey)
— the in-app onboarding screen links straight there. Google has issued keys
in more than one format (`AIzaSy…` and `AQ.Ab8R…` both seen) — either works.
Paste it on-device, in the app — it never needs to pass through chat, a
commit, or any file in this repo.

## Getting the APK

This repo builds automatically on every push via GitHub Actions
(`.github/workflows/build.yml` — no local Android Studio needed for this).
The latest build is always at:

**https://github.com/webkinetic/catch/releases/tag/latest-build**

Open that link on the phone that'll install it, download `app-debug.apk`,
and enable "install unknown apps" for whichever app you downloaded it
through when Android prompts.

## Opening this project locally (optional)

Only needed if you want to edit/run it from Android Studio instead of
relying on the CI build above.

1. Install Android Studio (Ladybug or newer): https://developer.android.com/studio
2. File → Open → select this folder
3. Let Gradle sync. The Gradle wrapper jar isn't checked in (this repo was
   scaffolded on a machine without a local JDK) — Android Studio will offer
   to regenerate it on first sync; accept that prompt.
4. Run on a device/emulator running API 29+.

## Next phase

The confirm screen is the logical next step — per the brief, "this is the
app, spend disproportionate time here." Without it, `AWAITING_CONFIRM`
captures just sit in the inbox with no way to file or discard them.
