package com.example.core.model

import com.example.core.model.adapters.DateSerializer
import com.example.core.model.adapters.IPSerializer
import kotlinx.serialization.Serializable
import java.net.InetAddress
import java.time.OffsetDateTime

@Serializable
data class BannedIP(
    @Serializable(with = IPSerializer::class)
    val ip: InetAddress,
    @Serializable(with = DateSerializer::class)
    override val created: OffsetDateTime,
    override val source: String,
    override val expires: String,
    override val reason: String
) : Banned