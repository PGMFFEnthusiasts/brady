package me.fireballs.brady.broxy.utils

import io.valkey.Jedis
import io.valkey.JedisPooled
import java.net.URI

fun valkeyUrlOrLocalDefault(): String = System.getenv("BRADY_VALKEY") ?: System.getenv("BRADY_REDIS")

fun newValkeyPooledClient(url: String = valkeyUrlOrLocalDefault()): JedisPooled = JedisPooled(URI(url))

fun newValkeyClient(url: String = valkeyUrlOrLocalDefault()): Jedis = Jedis(URI(url))
