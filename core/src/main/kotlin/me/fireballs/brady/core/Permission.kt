package me.fireballs.brady.core

import org.bukkit.command.CommandSender

fun testPerm(target: CommandSender, permission: String?): Boolean {
    if (testPermSilent(target, permission)) return true
    target.send("&c⚠ No permission to do this.".cc())
    uhOh.play(target)
    return false
}

fun testPermSilent(target: CommandSender, permission: String?): Boolean {
    if (permission.isNullOrEmpty()) return true
    for (p in permission.split(';').dropLastWhile { it.isEmpty() }.toTypedArray()) {
        if (target.hasPermission(p)) return true
    }
    return false
}
