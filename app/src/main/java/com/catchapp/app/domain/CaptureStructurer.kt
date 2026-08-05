package com.catchapp.app.domain

/**
 * Turns a raw transcript into a structured capture. Sits behind an interface
 * on purpose — today it's backed by Gemini (GeminiCaptureStructurer), but a
 * proxy backend or a different model could replace it later without touching
 * any call site (WorkManager workers only ever see this interface).
 */
interface CaptureStructurer {
    suspend fun structure(request: StructureRequest): Result<StructuredCapture>

    /**
     * A cheap, direct check that [apiKey] actually works — takes the key as
     * a parameter (rather than reading ApiKeyStore) so the onboarding screen
     * can test what's currently typed before it's ever saved.
     */
    suspend fun verifyKey(apiKey: String): Result<Unit>
}
