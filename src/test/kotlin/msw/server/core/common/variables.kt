package msw.server.core.common

import kotlin.random.Random

val fixedSeedRandom = Random(1234)
val alphanum = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toSet()
val extraChars = "()[]{}+-*/.,:;".toSet()