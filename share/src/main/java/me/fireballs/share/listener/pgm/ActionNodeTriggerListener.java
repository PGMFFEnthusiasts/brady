package me.fireballs.share.listener.pgm;

import me.fireballs.share.football.CompletedFootballThrow;
import me.fireballs.share.football.FootballCompletedThrowListener;
import me.fireballs.share.util.FootballDebugChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.event.ActionNodeTriggerEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ActionNodeTriggerListener implements Listener {
    private final String flagPickupActionId;
    private final String flagReceiveActionId;
    private final String ballThrownActionId;
    private final String flagStealActionId;
    private final String carrierDiedActionId;
    private final String roundIncrementActionId;
    private final String resetFlagActionId;
    private final Set<String> relevantActions;
    private final List<FootballCompletedThrowListener> observers = new ArrayList<>();

    private MatchPlayer thrower;
    private Location throwLocation;
    private MatchPlayer catcher;
    private Location catchLocation;
    private Location lossOfControlLocation;

    public ActionNodeTriggerListener(
        final String flagPickupActionId,
        final String flagReceiveActionId,
        final String ballThrownActionId,
        final String flagStealActionId,
        final String carrierDiedActionId,
        final String roundIncrementActionId,
        final String resetFlagActionId
    ) {
        this.flagPickupActionId = flagPickupActionId;
        this.flagReceiveActionId = flagReceiveActionId;
        this.ballThrownActionId = ballThrownActionId;
        this.flagStealActionId = flagStealActionId;
        this.carrierDiedActionId = carrierDiedActionId;
        this.roundIncrementActionId = roundIncrementActionId;
        this.resetFlagActionId = resetFlagActionId;
        this.relevantActions = Set.of(
            flagPickupActionId, flagReceiveActionId, ballThrownActionId, flagStealActionId, carrierDiedActionId,
            roundIncrementActionId, resetFlagActionId
        );
    }

    public void addObserver(final FootballCompletedThrowListener observer) {
        observers.add(observer);
    }

    @EventHandler
    public void onActionNodeTrigger(final ActionNodeTriggerEvent event) {
        if (!relevantActions.contains(event.nodeId)) return;
        if (event.nodeId.equals(resetFlagActionId)) { // whenever the flag is reset we should have a clean state
            FootballDebugChannel.sendMessage(Component.text("Flag reset action detected"));
            resetState();
            return;
        }
        if (!(event.scope instanceof MatchPlayer matchPlayer)) return;
        if (thrower == null) {
            // when a potential thrower obtains the ball
            if (isReceiveControlAction(event.nodeId)) {
                FootballDebugChannel.sendMessage(Component.text("(Potential) thrower identified"));
                thrower = matchPlayer;
            }
            return;
        }

        if (catcher == null && throwLocation == null) { // thrower got da ball
            if (event.nodeId.equals(ballThrownActionId)) { // thrower makes a move
                FootballDebugChannel.sendMessage(Component.text("Thrower made a throw"));
                throwLocation = thrower.getLocation();
                return;
            } else if (isLossOfControlAction(event.nodeId)) { // thrower is bad at the game
                FootballDebugChannel.sendMessage(Component.text("Thrower is bad at the game"));
                thrower = null;
                return;
            } else if (event.nodeId.equals(flagPickupActionId)) { // i dont rlly know why i have to do this
                return;
            }
        } else if (throwLocation != null && catcher == null) { // throw state
            if (event.nodeId.equals(flagReceiveActionId)) { // pass completion, scope is the catcher
                FootballDebugChannel.sendMessage(Component.text("Thrower completed the pass!"));
                catcher = matchPlayer;
                catchLocation = matchPlayer.getLocation();
                return;
            } else if (event.nodeId.equals(flagPickupActionId)) { // i dont rlly know why i have to do this
                return;
            }
        } else { // catcher en route
            if (isLossOfControlAction(event.nodeId)) {
                FootballDebugChannel.sendMessage(Component.text("Catcher lost ball, marking throw as complete"));
                lossOfControlLocation = matchPlayer.getLocation();
                emitThrow();
                resetState();
                if (event.nodeId.equals(ballThrownActionId)) {
                    FootballDebugChannel.sendMessage(Component.text("Thrower made a throw"));
                    thrower = matchPlayer;
                    throwLocation = thrower.getLocation();
                }
                return;
            } else if (event.nodeId.equals(flagPickupActionId)) { // i dont rlly know why i have to do this
                return;
            }
        }


        FootballDebugChannel.sendMessage(
            Component.text(
                "Resetting state, unknown case (node ID was " + event.nodeId + ")"
            )
        );
        // reset state in any unhandled case
        resetState();
    }

    @EventHandler
    public void onMatchEndEvent(final MatchFinishEvent event) {
        if (catcher != null) {
            lossOfControlLocation = catcher.getLocation();
            emitThrow();
            resetState();
        }
    }

    @EventHandler
    public void onMatchQuitEvent(final PlayerLeaveMatchEvent event) {
        if (event.getPlayer().equals(catcher)) {
            lossOfControlLocation = catcher.getLocation();
            emitThrow();
            resetState();
        } else if (event.getPlayer().equals(thrower)) {
            resetState();
        }
    }

    private void emitThrow() {
        final CompletedFootballThrow completedThrow = new CompletedFootballThrow(
            thrower, throwLocation,
            catcher, catchLocation, lossOfControlLocation
        );
        for (final FootballCompletedThrowListener observer : observers) {
            observer.onThrowCompletion(completedThrow);
        }
    }

    private void resetState() {
        thrower = null;
        throwLocation = null;
        catcher = null;
        catchLocation = null;
        lossOfControlLocation = null;
    }

    private boolean isReceiveControlAction(final String actionNodeId) {
        return (
            actionNodeId.equals(flagPickupActionId) ||
            actionNodeId.equals(flagReceiveActionId) ||
            actionNodeId.equals(flagStealActionId)
        );
    }

    private boolean isLossOfControlAction(final String actionNodeId) {
        return (
            actionNodeId.equals(ballThrownActionId) ||
                actionNodeId.equals(carrierDiedActionId) ||
                actionNodeId.equals(roundIncrementActionId)
        );
    }
}
