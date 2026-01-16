package me.fireballs.brady.core

import com.google.common.collect.MapMaker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent

private val flagMap = MapMaker()
    .concurrencyLevel(4)
    .makeMap<String, FeatureFlag<*>>()

fun attemptToSetFlag(key: String, value: String): Boolean {
    val flag = flagMap[key.lowercase()] ?: return false
    return flag.set(value) == null
}

abstract class FeatureFlag<T>(
    val key: String,
    var state: T,
) {
    init {
        flagMap[key.lowercase()] = this
    }

    abstract fun render(): Component
    abstract fun set(value: String): Component?
    abstract fun suggest(): List<String>
    abstract fun type(): String
}

class FeatureFlagBool(key: String, initial: Boolean = false) : FeatureFlag<Boolean>(key, initial) {
    private val truthy = "&atrue".cc()
    private val falsy = "&cfalse".cc()
    private val unparsable = "&cMust provide a boolean-ish value".cc()

    override fun render() = if (state) truthy else falsy
    override fun set(value: String): Component? {
        val lc = value.lowercase()
        if (lc == "true" || lc == "t" || lc == "y" || lc == "yes" || lc == "1") {
            state = true
            return null
        }

        if (lc == "false" || lc == "f" || lc == "n" || lc == "no" || lc == "0") {
            state = false
            return null
        }

        return unparsable
    }

    override fun suggest() = listOf("true", "false")
    override fun type() = "bool"
}

class FeatureFlagsSubscriber : KoinComponent {
    init {
        command("flag", permission = "brady.admin") {
            executor {
                sender.send("&7⚑ Feature Flags".cc())
                sender.send("/flag list &7- list all the flags and their values".cc())
                sender.send("/flag show <flagKey> &7- show more detail about a flag".cc())
                sender.send("/flag set <flagKey> <value> &7- set the value of a flag".cc())
            }

            subcommand("list") {
                executor {
                    sender.send("&7Showing &e${flagMap.count()}&7 flags:".cc())
                    for (flag in flagMap.values) {
                        sender.send(
                            ("${flag.key}&7: ".cc() + flag.render() + "&7 (${flag.type()})".cc())
                                .hover("Click to show more detail".c())
                                .command("/flag show ${flag.key}")
                        )
                    }
                }
            }

            subcommand("show") {
                executor {
                    val flagName = capture("Requires flag name") { subArgs[0] }
                    val flag = flagMap[flagName.lowercase()] ?: err("Flag not found")
                    sender.send(
                        ("${flag.key}&7: ".cc() + flag.render() + "&7 (${flag.type()})".cc())
                            .hover("Click to quick-set".c())
                            .fill("/flag set ${flag.key} ")
                    )

                    val suggest = flag.suggest()
                    if (suggest.isEmpty()) return@executor

                    sender.send(
                        Component.join(
                            JoinConfiguration.spaces(),
                            suggest.map {
                                "&7[&f${it}&7]".cc()
                                    .command("/flag set ${flag.key} $it")
                                    .hover("Click to quick-set the flag to this value".c())
                            }
                        )
                    )
                }

                tabCompleter = {
                    if (subArgs.size < 2) flagMap.values.map { it.key }
                    else emptyList()
                }
            }

            subcommand("set") {
                executor {
                    val flagName = capture("Requires flag name") { subArgs[0] }
                    val flag = flagMap[flagName.lowercase()] ?: err("Flag not found")
                    val newValue = capture("Requires new value") { subArgs[1] }

                    val prev = flag.render()
                    val errorMessage = flag.set(newValue)
                    if (errorMessage != null) err(errorMessage)

                    val changedMessage = "&7⚑ ".cc() + sender.component() + "&7 set &f${flag.key}&7 from ".cc() + prev + "&7 ➡ ".cc() + flag.render() + ".&7".cc()
                    Bukkit.getOnlinePlayers()
                        .filter { it.hasPermission("brady.admin") }
                        .forEach { it.send(changedMessage.forWhom(it)) }
                }

                tabCompleter = {
                    when (subArgs.size) {
                        0, 1 -> flagMap.values.map { it.key }
                        2 -> flagMap[subArgs[0].lowercase()]?.suggest() ?: listOf()
                        else -> listOf()
                    }
                }
            }
        }
    }
}
