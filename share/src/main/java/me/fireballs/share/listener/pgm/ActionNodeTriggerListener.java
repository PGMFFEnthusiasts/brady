package me.fireballs.share.listener.pgm;

import me.fireballs.share.football.CompletedFootballPass;
import me.fireballs.share.football.FootballListener;
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
    private final List<FootballListener> observers = new ArrayList<>();

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

    public void addObserver(final FootballListener observer) {
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
        if (isReceiveControlAction(event.nodeId)) {
            for (final FootballListener observer : observers) {
                observer.onCarrierChange(matchPlayer);
            }
        }

        if (event.nodeId.equals(roundIncrementActionId)) { // touchdown, record then reset
            final boolean isTouchdownPass = thrower != null && catcher != null;
            if (isTouchdownPass) {
                lossOfControlLocation = catcher.getLocation();
                emitPass();
            }
            for (final FootballListener observer : observers) {
                observer.onTouchdown(matchPlayer);
                if (isTouchdownPass) {
                    observer.onTouchdownPass(thrower);
                }
            }
            resetState();
        } else if (event.nodeId.equals(flagStealActionId)) { // strip, change thrower then reset
            final boolean wasPassed = thrower != null && catcher != null;
            if (wasPassed) {
                lossOfControlLocation = catcher.getLocation();
                emitPass();
            }
            for (final FootballListener observer : observers) {
                observer.onBallSteal(matchPlayer);
            }
            resetState();
            thrower = matchPlayer;
        } else if (thrower == null) { // no thrower assigned
            if (isReceiveControlAction(event.nodeId)) { // when some1 gets the ball
                FootballDebugChannel.sendMessage(Component.text("(Potential) thrower identified"));
                thrower = matchPlayer;
                for (final FootballListener observer : observers) {
                    observer.onBallPickup(matchPlayer);
                }
            }
        } else if (throwLocation == null) { // some1 got da ball
            if (event.nodeId.equals(ballThrownActionId)) { // thrower makes a move
                FootballDebugChannel.sendMessage(Component.text("Thrower made a throw"));
                for (final FootballListener observer : observers) {
                    observer.onThrow(thrower);
                }
                throwLocation = thrower.getLocation();
            } else if (isLossOfControlAction(event.nodeId)) { // thrower is bad at the game
                FootballDebugChannel.sendMessage(Component.text("Thrower is bad at the game"));
                thrower = null;
            }
        } else if (throwLocation != null && catcher == null) { // ball mid air post-throw
            if (event.nodeId.equals(flagReceiveActionId)) { // pass completion, scope is the catcher
                FootballDebugChannel.sendMessage(Component.text("Thrower completed the pass!"));
                catcher = matchPlayer;
                catchLocation = matchPlayer.getLocation();
                for (final FootballListener observer : observers) {
                    observer.onCatch(catcher);
                    observer.onPass(thrower, catcher);
                }
            }
        } else { // catcher en route
            if (isLossOfControlAction(event.nodeId)) {
                FootballDebugChannel.sendMessage(Component.text("Catcher lost ball, marking throw as complete"));
                lossOfControlLocation = matchPlayer.getLocation();
                emitPass();
                resetState();
                if (event.nodeId.equals(ballThrownActionId)) {
                    FootballDebugChannel.sendMessage(Component.text("Thrower made a throw"));
                    thrower = matchPlayer;
                    for (final FootballListener observer : observers) {
                        observer.onThrow(thrower);
                    }
                    throwLocation = thrower.getLocation();
                }
            }
        }
    }

    @EventHandler
    public void onMatchEndEvent(final MatchFinishEvent event) {
        if (catcher != null) {
            lossOfControlLocation = catcher.getLocation();
            emitPass();
            resetState();
        }
    }

    @EventHandler
    public void onMatchQuitEvent(final PlayerLeaveMatchEvent event) {
        if (event.getPlayer().equals(catcher)) {
            lossOfControlLocation = catcher.getLocation();
            emitPass();
            resetState();
        } else if (event.getPlayer().equals(thrower)) {
            resetState();
        }
    }

    private void emitPass() {
        if (thrower == null || throwLocation == null || catcher == null
            || catchLocation == null || lossOfControlLocation == null) return;
        final CompletedFootballPass completedThrow = new CompletedFootballPass(
            thrower, throwLocation,
            catcher, catchLocation, lossOfControlLocation
        );
        for (final FootballListener observer : observers) {
            observer.onPassPossessionCompletion(completedThrow);
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
