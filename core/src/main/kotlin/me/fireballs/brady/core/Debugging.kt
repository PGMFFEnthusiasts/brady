package me.fireballs.brady.core

import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val knownChannelSet = mutableSetOf<String>()
private val consumers = mutableMapOf<CommandSender, LogConsumer>()

private class LogConsumer(
    val sender: CommandSender,
) {
    var allSubscribed = false
    val subscribedChannels = mutableSetOf<String>()

    fun publish(channel: String, event: Component) {
        if (!allSubscribed && !subscribedChannels.contains(channel)) return
        sender.send("&8(${channel}) ".cc() + event)
    }
}

fun log(channel: String, event: String) {
    log(channel, event.c())
}

private val allowedChannelCharacters = Regex("[^0-9a-z_-]")

fun log(channel: String, event: Component) {
    val normalizedChannelName = channel.lowercase().replace(allowedChannelCharacters, "")
    knownChannelSet.add(normalizedChannelName)
    consumers.forEach { it.value.publish(normalizedChannelName, event) }
}

class DebuggingSubscriber : Listener, KoinComponent {
    private val core by inject<Core>()

    init {
        core.registerEvents(this)

        command("d", "brady.debug") {
            executor {
                sender.send()
                sender.send("&6Brady's debugging commands:".cc())
                sender.send("&6- /d sub [channel] (subscribes to a channel / all channels)".cc())
                sender.send("&6- /d unsub [channel] (unsubscribes to a channel / all channels)".cc())
                sender.send("&6- /d show (shows subscription statuses)".cc())
                sender.send()
            }

            subcommand("sub", aliases = arrayOf("s")) {
                tabCompleter {
                    knownChannelSet.filter { it.startsWith(subArgs.last(), true) }
                }

                executor {
                    if (subArgs.isEmpty()) {
                        sender.send("&6Subscribing to all channels".cc())
                        val consumer = consumers.getOrPut(sender) { LogConsumer(sender) }
                        consumer.allSubscribed = true
                        return@executor
                    }

                    sender.send("&6Subscribing to:".cc())
                    subArgs.forEach { sender.send("&6- $it".cc()) }

                    val consumer = consumers.getOrPut(sender) { LogConsumer(sender) }
                    consumer.subscribedChannels.addAll(subArgs)
                }
            }

            subcommand("unsub", aliases = arrayOf("u", "usub")) {
                tabCompleter {
                    consumers[sender]?.subscribedChannels?.filter { it.startsWith(subArgs.last(), true) }
                        ?: emptyList()
                }

                executor {
                    if (subArgs.isEmpty()) {
                        sender.send("&6Unsubscribing to all channels".cc())
                        val consumer = consumers.getOrPut(sender) { LogConsumer(sender) }
                        consumer.allSubscribed = false
                        consumer.subscribedChannels.clear()
                        return@executor
                    }

                    val consumer = consumers.getOrPut(sender) { LogConsumer(sender) }

                    if (consumer.allSubscribed) {
                        sender.send("&eWARNING: You are currently subscribed to all channels, so you can't unsubscribe from a specific one.".cc())
                        sender.send("&eWARNING: Unsubcribe from all channels by running ".cc() + "&9/d unsub".cc().command("/d unsub"))
                        return@executor
                    }

                    consumer.subscribedChannels.removeAll(subArgs.toSet())

                    sender.send("&6Unsubscribing to:".cc())
                    subArgs.forEach { sender.send("&6- $it".cc()) }
                }
            }

            subcommand("show") {
                executor {
                    val channels = consumers[sender]?.subscribedChannels
                    sender.send("&6Subscribed to:".cc())
                    channels?.forEach { sender.send("&6- $it".cc()) }
                }
            }
        }
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        consumers.remove(event.player)
    }
}


