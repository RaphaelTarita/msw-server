package com.example.core.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.internal.NamedValueDecoder

@ExperimentalSerializationApi
@InternalSerializationApi
class ConvertEngine(private val map: Map<String, String>, descriptor: SerialDescriptor) : NamedValueDecoder() {
    companion object {
        private fun String.internalToBoolean(): Boolean {
            val numeric = toLongOrNull()
            return when {
                numeric != null -> numeric > 0
                equals("true", true) -> true
                equals("false", true) -> false
                else -> throw IllegalArgumentException("$this cannot be interpreted as Boolean")
            }
        }
    }
    
    private var currentIndex = 0
    private val isCollection = descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP
    private val size = if (isCollection) Int.MAX_VALUE else descriptor.elementsCount

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return ConvertEngine(map, descriptor).also { copyTagsTo(it) }
    }

    override fun decodeTaggedValue(tag: String): String {
        return map.getValue(tag)
    }

    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int {
        val taggedValue = map.getValue(tag)
        return taggedValue.toIntOrNull() ?: enumDescriptor.getElementIndex(taggedValue)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (currentIndex < size) {
            val name = descriptor.getTag(currentIndex++)
            if (map.keys.any { it.startsWith(name) }) return currentIndex - 1
            if (isCollection) {
                // if map does not contain key we look for, then indices in collection have ended
                break
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeTaggedBoolean(tag: String): Boolean {
        return decodeTaggedValue(tag).internalToBoolean()
    }

    override fun decodeTaggedByte(tag: String): Byte {
        return decodeTaggedValue(tag).toByte()
    }

    override fun decodeTaggedShort(tag: String): Short {
        return decodeTaggedValue(tag).toShort()
    }

    override fun decodeTaggedInt(tag: String): Int {
        return decodeTaggedValue(tag).toInt()
    }

    override fun decodeTaggedLong(tag: String): Long {
        return decodeTaggedValue(tag).toLong()
    }

    override fun decodeTaggedFloat(tag: String): Float {
        return decodeTaggedValue(tag).toFloat()
    }

    override fun decodeTaggedDouble(tag: String): Double {
        return decodeTaggedValue(tag).toDouble()
    }

    override fun decodeTaggedChar(tag: String): Char {
        return decodeTaggedValue(tag).single()
    }
}