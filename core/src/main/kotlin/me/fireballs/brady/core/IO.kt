package me.fireballs.brady.core

suspend fun logExceptions(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
