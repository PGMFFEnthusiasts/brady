package me.fireballs.brady.core

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.event.ActionNodeTriggerEvent

private val idFlagReset = "reset-flag"
private val idFlagPickup = "flag-pickup-event"
private val idFlagReceive = "flag-receive-event"
private val idSnowballThrow = "snowball-thrown"
private val idFlagSteal = "flag-steal-event"
private val idFlagCarrierDied = "carrier-died-event"
private val idRoundIncrement = "increment-round"

class StandardBradyListener : Listener, KoinComponent {
    private val core by inject<Core>()

    init {
        core.registerEvents(this)
    }
//
//    private var carrier;
//    private var throwLocation;
//    private var catchLocation;

    @EventHandler
    private fun actionTrigger(event: ActionNodeTriggerEvent) {
        log("actions-all", "fired '${event.nodeId}' by ${event.scope}")

        when (event.nodeId) {
            idFlagReset -> {

            }
        }
    }
}

interface BradyAction


