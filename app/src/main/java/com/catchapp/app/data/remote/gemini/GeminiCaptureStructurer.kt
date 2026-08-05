package com.catchapp.app.data.remote.gemini

import com.catchapp.app.data.local.CaptureKind
import com.catchapp.app.data.remote.ApiKeyStore
import com.catchapp.app.domain.CaptureStructurer
import com.catchapp.app.domain.StructureRequest
import com.catchapp.app.domain.StructuredCapture
import com.catchapp.app.domain.StructuringException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * gemini-2.5-flash: cheap/fast structured-extraction model, free tier
 * available via a Google AI Studio key. Bump to a Pro-tier model only if a
 * future feature needs deeper reasoning than "classify + extract fields".
 */
private const val MODEL = "gemini-2.5-flash"
private const val ENDPOINT =
    "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
private const val FUNCTION_NAME = "file_capture"

class GeminiCaptureStructurer @Inject constructor(
    private val httpClient: HttpClient,
    private val apiKeyStore: ApiKeyStore
) : CaptureStructurer {

    override suspend fun structure(request: StructureRequest): Result<StructuredCapture> {
        val apiKey = apiKeyStore.getKey()
            ?: return Result.failure(StructuringException.MissingApiKey)

        return try {
            val response = httpClient.post(ENDPOINT) {
                // Header, not a ?key= query param — keeps the key out of any
                // proxy/access logs that record request URLs (hard rule #2).
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(buildRequestBody(request))
            }.body<GenerateContentResponse>()

            val args = response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull { it.functionCall != null }
                ?.functionCall
                ?.args
                ?: return Result.failure(StructuringException.NoFunctionCallReturned)

            Result.success(args.toStructuredCapture())
        } catch (e: Exception) {
            Result.failure(StructuringException.NetworkError(e))
        }
    }

    private fun buildRequestBody(request: StructureRequest): GenerateContentRequest =
        GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = buildSystemPrompt(request)))),
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = request.transcript)))
            ),
            tools = listOf(Tool(functionDeclarations = listOf(fileCaptureDeclaration))),
            toolConfig = ToolConfig(
                functionCallingConfig = FunctionCallingConfig(
                    mode = "ANY",
                    allowedFunctionNames = listOf(FUNCTION_NAME)
                )
            )
        )

    private fun buildSystemPrompt(request: StructureRequest): String = buildString {
        appendLine("You are the structuring engine for Catch, a voice-capture inbox.")
        appendLine("Turn the user's raw spoken transcript into a single $FUNCTION_NAME call.")
        appendLine()
        appendLine("Current datetime: ${request.now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}")
        appendLine("IANA timezone: ${request.now.zone.id}")
        appendLine("Typical working hours: ${request.workingHoursStart}-${request.workingHoursEnd}")
        if (request.existingProjects.isNotEmpty()) {
            appendLine("Existing projects: ${request.existingProjects.joinToString(", ")}")
        }
        if (request.recentTags.isNotEmpty()) {
            appendLine("Recently used tags: ${request.recentTags.joinToString(", ")}")
        }
        if (request.recentPeople.isNotEmpty()) {
            appendLine("Recently mentioned people: ${request.recentPeople.joinToString(", ")}")
        }
        appendLine()
        appendLine("Resolve relative dates (\"Thursday\", \"tomorrow morning\") against the datetime and zone above.")
        appendLine("project must be one of the existing projects above, or omitted — never invent one.")
        appendLine("confidence below 0.6 means the transcript was ambiguous; say so honestly, don't guess high.")
    }

    private val fileCaptureDeclaration = FunctionDeclaration(
        name = FUNCTION_NAME,
        description = "File the user's captured thought into the right destination.",
        parameters = FunctionParameters(
            properties = mapOf(
                "kind" to PropertySchema(
                    type = "STRING",
                    enum = listOf("task", "event", "note", "idea", "contact_followup")
                ),
                "title" to PropertySchema(
                    type = "STRING",
                    description = "Terse, imperative. Max 8 words."
                ),
                "body" to PropertySchema(
                    type = "STRING",
                    description = "Cleaned-up detail. Omit if the title says it all."
                ),
                "due_iso" to PropertySchema(
                    type = "STRING",
                    description = "ISO 8601. Omit if no time was implied."
                ),
                "project" to PropertySchema(
                    type = "STRING",
                    description = "Must be one of the user's existing projects, or omitted."
                ),
                "tags" to PropertySchema(type = "ARRAY", items = PropertySchema(type = "STRING")),
                "people" to PropertySchema(type = "ARRAY", items = PropertySchema(type = "STRING")),
                "confidence" to PropertySchema(
                    type = "NUMBER",
                    description = "0-1. Below 0.6 means show the raw transcript prominently."
                )
            ),
            required = listOf("kind", "title", "confidence")
        )
    )

    private fun FileCaptureArgs.toStructuredCapture(): StructuredCapture = StructuredCapture(
        kind = runCatching { CaptureKind.valueOf(kind.uppercase()) }.getOrDefault(CaptureKind.NOTE),
        title = title,
        body = body,
        dueIso = dueIso,
        project = project,
        tags = tags,
        people = people,
        confidence = confidence
    )
}
