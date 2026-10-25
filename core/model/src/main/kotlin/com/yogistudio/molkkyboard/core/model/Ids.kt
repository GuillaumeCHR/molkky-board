package com.yogistudio.molkkyboard.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class PlayerId(
    val value: String,
)

@Serializable
@JvmInline
value class TeamId(
    val value: String,
)
