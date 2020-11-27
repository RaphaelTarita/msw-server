package com.example.core.versions.model

import kotlinx.serialization.Serializable

@Serializable
enum class VersionType(val typeName: String) {
    OLD_ALPHA("old_alpha"),
    OLD_BETA("old_beta"),
    RELEASE("release"),
    SNAPSHOT("snapshot"),
    ALL("__all__")
}