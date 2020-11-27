package com.example.core.model.props

import kotlinx.serialization.Serializable

@Serializable
data class RCon(
    val password: String = "",
    val port: Int = 25575
)