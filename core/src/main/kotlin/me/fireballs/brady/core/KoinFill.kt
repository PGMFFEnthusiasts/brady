package me.fireballs.brady.core

import org.koin.core.module.Module
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf

inline fun <reified R> Module.serviceOf(crossinline primary: () -> R) {
    singleOf<R>(primary) { createdAtStart() }
}
