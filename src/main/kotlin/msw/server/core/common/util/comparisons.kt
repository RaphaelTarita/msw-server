package msw.server.core.common.util

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.StringFormat

fun semanticEquivalence(
    lop: String,
    rop: String,
    format: StringFormat,
    semantics: DeserializationStrategy<*>
): Boolean {
    return format.decodeFromString(semantics, lop) == format.decodeFromString(semantics, rop)
}

sealed interface NullableCompare<out T> {
    @JvmInline
    value class RESULT(val res: Int) : NullableCompare<Nothing>
    data class CONTINUE<T>(val o1: T, val o2: T) : NullableCompare<T>
}

fun <T : Any> compareNullable(n1: T?, n2: T?): NullableCompare<T> {
    return if (n1 == null) {
        if (n2 == null) NullableCompare.RESULT(0)
        else NullableCompare.RESULT(-1)
    } else {
        if (n2 == null) NullableCompare.RESULT(1)
        else NullableCompare.CONTINUE(n1, n2)
    }
}

fun <T : Any, U> comparatorForNested(innerComparator: Comparator<U>, selector: (T) -> U): Comparator<in T?> {
    return Comparator { o1, o2 ->
        when (val ncomp = compareNullable(o1, o2)) {
            is NullableCompare.RESULT -> ncomp.res
            is NullableCompare.CONTINUE -> innerComparator.compare(selector(ncomp.o1), selector(ncomp.o2))
        }
    }
}