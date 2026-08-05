# Catch — Voice Capture Inbox

A spoken thought → correctly-filed task/event/note. See the original build
brief for full product/architecture rationale.

## Status: the loop works end-to-end, minus confirmation

Built so far:
- Gradle project (version catalog, AGP 8.6, Kotlin 2.0, KSP, Hilt)
- Hilt-wired `CatchApplication`, driving WorkManager via `HiltWorkerFactory`
- Room database (`CatchDatabase`) with `CaptureEntity`, its `CaptureState`
  state machine, `CaptureDao`, and type converters
- `CaptureStructurer` — the AI structuring layer, running on **Gemini**
  (`gemini-2.5-flash`, free tier via a Google AI Studio key), behind an
  interface so the provider can change later without touching call sites:
  - `domain/` — `CaptureStructurer`, `StructureRequest`, `StructuredCapture`,
    `StructuringException`
  - `data/remote/gemini/` — real `generateContent` request/response DTOs and
    `GeminiCaptureStructurer`, using forced function calling
    (`toolConfig.functionCallingConfig.mode = "ANY"`) so the model always
    returns a `file_capture` call, never prose
  - `data/remote/ApiKeyStore.kt` — encrypted on-device key storage
    (Android Keystore via `androidx.security.crypto`)
- **Capture trigger + on-device STT** (`capture/`):
  - `CaptureTileService` — Quick Settings tile, the brief's top-value trigger
  - `CaptureActivity` — transparent trampoline; listens via
    `SpeechRecognizer` (on-device where available), writes the transcript to
    Room as `CAPTURED` the instant recognition ends, and dismisses —
    nothing here ever awaits a network call (hard rules #1 and #4)
  - Works from the lock screen (`showWhenLocked`/`turnScreenOn`)
- **`CaptureRepository` + `StructureCaptureWorker`** — the WorkManager glue:
  enqueued with a network constraint + exponential backoff, so a `CAPTURED`
  row genuinely sits and waits for connectivity rather than failing offline;
  advances `CAPTURED → STRUCTURING → AWAITING_CONFIRM`/`FAILED`
- **Inbox screen** (`ui/InboxScreen.kt`) — replaces the placeholder; lists
  every capture live from Room with a state pill, confidence, and a FAB that
  also triggers capture (for testing without the QS tile added yet)

Not built yet: the confirm screen (captures currently sit at
`AWAITING_CONFIRM` with no way to act on them), onboarding/API-key-entry
screen (so `ApiKeyStore` is empty until one exists — captures will fail with
"No Gemini API key set." until then), external destinations, undo. Per the
build brief, phases land one at a time.

**On the API key:** get one at [aistudio.google.com/apikey](https://aistudio.google.com/apikey).
Google has issued keys in more than one format (`AIzaSy…` and `AQ.Ab8R…` both
seen) — either works, `ApiKeyStore` just stores whatever string you give it.
Once the onboarding screen exists, paste it there, on-device — it should
never need to pass through chat, a commit, or any file in this repo.

## Opening this project

You need **Android Studio** (bundles a JDK and the Android SDK) — this repo
was scaffolded on a machine that doesn't have either installed yet.

1. Install Android Studio (Ladybug or newer): https://developer.android.com/studio
2. File → Open → select this folder
3. Let Gradle sync. The Gradle wrapper jar isn't checked in yet (couldn't be
   generated without a local JDK) — Android Studio will offer to regenerate
   it on first sync; accept that prompt.
4. Run on a device/emulator running API 29+.

## Next phase

The confirm screen is the logical next step — per the brief, "this is the
app, spend disproportionate time here." Without it, `AWAITING_CONFIRM`
captures just sit in the inbox with no way to file or discard them.
