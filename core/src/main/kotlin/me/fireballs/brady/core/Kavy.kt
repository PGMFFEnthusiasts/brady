package me.fireballs.brady.core

import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.types.MetaNode
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * This loads from cache call it as many times as you want
 */
fun Player.stringGet(key: String) = LuckPermsProvider
    .get().userManager
    .getUser(uniqueId)
    ?.cachedData
    ?.metaData
    ?.getMetaValue(key)

/**
 * YOU SHOULD ONLY CALL THIS IF YOU WANT TO CHANGE A VALUE OTHERWISE IT IS A
 * TRANSACTION TRIGGERED. ONLY DO THIS IF YOU KNOW THE VALUE CHANGED.
 */
fun Player.stringSet(key: String, value: String): CompletableFuture<Void>? {
    val user = LuckPermsProvider
        .get().userManager
        .getUser(uniqueId) ?: return null
    user.data().add(MetaNode.builder(key, value).build())

    return LuckPermsProvider.get().userManager.saveUser(user)
}

/**
 * This loads from cache call it as many times as you want
 */
fun Player.boolGet(key: String, defaultValue: Boolean = false) =
    stringGet(key)?.toBooleanStrictOrNull() ?: defaultValue

/**
 * YOU SHOULD ONLY CALL THIS IF YOU WANT TO CHANGE A VALUE OTHERWISE IT IS A
 * TRANSACTION TRIGGERED. ONLY DO THIS IF YOU KNOW THE VALUE CHANGED.
 */
fun Player.boolSet(key: String, value: Boolean) = stringSet(key, value.toString())
