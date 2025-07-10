package me.fireballs.brady.broxy.utils

suspend fun logExceptions(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
