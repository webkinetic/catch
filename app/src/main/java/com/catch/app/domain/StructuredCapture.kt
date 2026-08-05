package com.catch.app.domain

import com.catch.app.data.local.CaptureKind

/**
 * The model's proposed structuring of a transcript. This is a *suggestion*,
 * never auto-filed — the confirm screen (a later phase) is what turns this
 * into a written CaptureEntity update. When [confidence] < 0.6 the confirm
 * screen must lead with the raw transcript, not this.
 */
data class StructuredCapture(
    val kind: CaptureKind,
    val title: String,
    val body: String?,
    val dueIso: String?,
    val project: String?,
    val tags: List<String>,
    val people: List<String>,
    val confidence: Float
)
