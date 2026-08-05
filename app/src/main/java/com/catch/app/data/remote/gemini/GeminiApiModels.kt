package com.catch.app.data.remote.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request/response shapes for the Gemini `generateContent` REST endpoint
 * (https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent).
 *
 * These mirror Google's proto3 JSON mapping, which is lowerCamelCase — do
 * not "fix" these to snake_case, the API will silently ignore unrecognised
 * fields rather than error, and the request will come back schema-less.
 */
@Serializable
data class GenerateContentRequest(
    val systemInstruction: Content? = null,
    val contents: List<Content>,
    val tools: List<Tool>,
    val toolConfig: ToolConfig
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val functionCall: FunctionCall? = null
)

@Serializable
data class FunctionCall(
    val name: String,
    val args: FileCaptureArgs
)

@Serializable
data class Tool(
    val functionDeclarations: List<FunctionDeclaration>
)

@Serializable
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: FunctionParameters
)

@Serializable
data class FunctionParameters(
    val type: String = "OBJECT",
    val properties: Map<String, PropertySchema>,
    val required: List<String>
)

/** Google's OpenAPI-subset schema type — values are uppercase: STRING, NUMBER, ARRAY, OBJECT, etc. */
@Serializable
data class PropertySchema(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null,
    val items: PropertySchema? = null
)

@Serializable
data class ToolConfig(
    val functionCallingConfig: FunctionCallingConfig
)

/** mode = "ANY" forces a function call every time — Gemini's equivalent of Claude's forced tool_choice. */
@Serializable
data class FunctionCallingConfig(
    val mode: String = "ANY",
    val allowedFunctionNames: List<String>
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

/**
 * The `file_capture` function's args, as Gemini returns them. Field names
 * here are whatever we declared in [FunctionDeclaration.parameters] below —
 * `due_iso` stays snake_case deliberately, to match the original tool schema
 * 1:1 rather than for any wire-format reason.
 */
@Serializable
data class FileCaptureArgs(
    val kind: String,
    val title: String,
    val body: String? = null,
    @SerialName("due_iso") val dueIso: String? = null,
    val project: String? = null,
    val tags: List<String> = emptyList(),
    val people: List<String> = emptyList(),
    val confidence: Float
)
