package me.fireballs.brady.core

import io.valkey.Jedis
import io.valkey.JedisPooled
import java.net.URI

fun valkeyUrlOrNull(): String? = System.getenv("BRADY_VALKEY") ?: System.getenv("BRADY_REDIS")

fun newValkeyPooledClient(url: String? = valkeyUrlOrNull()): JedisPooled? {
    url ?: return null
    return JedisPooled(URI(url))
}

fun newValkeyClient(url: String? = valkeyUrlOrNull()): Jedis? {
    url ?: return null
    return Jedis(URI(url))
}
