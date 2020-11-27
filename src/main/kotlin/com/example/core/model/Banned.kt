package com.example.core.model

import java.time.OffsetDateTime

interface Banned {
    val created: OffsetDateTime
    val source: String
    val expires: String
    val reason: String
}