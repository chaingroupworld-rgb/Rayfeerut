package com.example.data.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @param:Json(name = "contents") val contents: List<GeminiContent>,
    @param:Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @param:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @param:Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @param:Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @param:Json(name = "temperature") val temperature: Float = 0.2f,
    @param:Json(name = "responseMimeType") val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @param:Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @param:Json(name = "content") val content: GeminiContent?
)

data class ParsedTaskResult(
    val title: String,
    val courseCode: String?,
    val dueDateOffsetHours: Long,
    val priority: String,
    val notes: String?
)
