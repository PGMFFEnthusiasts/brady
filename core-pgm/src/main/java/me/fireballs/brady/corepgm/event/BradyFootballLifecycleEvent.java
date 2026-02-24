package me.fireballs.brady.corepgm.event;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;

public class BradyFootballLifecycleEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final FootballLifecycleAction action;
    private final MatchPlayer actor;
    private final MatchPlayer thrower;
    private final MatchPlayer catcher;
    private final Location throwLocation;
    private final Location catchLocation;
    private final Location lossOfControlLocation;

    public BradyFootballLifecycleEvent(
        final FootballLifecycleAction action,
        final MatchPlayer actor,
        final MatchPlayer thrower,
        final MatchPlayer catcher,
        final Location throwLocation,
        final Location catchLocation,
        final Location lossOfControlLocation
    ) {
        this.action = action;
        this.actor = actor;
        this.thrower = thrower;
        this.catcher = catcher;
        this.throwLocation = throwLocation == null ? null : throwLocation.clone();
        this.catchLocation = catchLocation == null ? null : catchLocation.clone();
        this.lossOfControlLocation = lossOfControlLocation == null ? null : lossOfControlLocation.clone();
    }

    public BradyFootballLifecycleEvent(
        final FootballLifecycleAction action,
        final MatchPlayer actor,
        final MatchPlayer thrower,
        final MatchPlayer catcher
    ) {
        this(action, actor, thrower, catcher, null, null, null);
    }

    public BradyFootballLifecycleEvent(final FootballLifecycleAction action, final MatchPlayer actor) {
        this(action, actor, null, null, null, null, null);
    }

    public FootballLifecycleAction getAction() {
        return action;
    }

    public MatchPlayer getActor() {
        return actor;
    }

    public MatchPlayer getThrower() {
        return thrower;
    }

    public MatchPlayer getCatcher() {
        return catcher;
    }

    public Location getThrowLocation() {
        return throwLocation == null ? null : throwLocation.clone();
    }

    public Location getCatchLocation() {
        return catchLocation == null ? null : catchLocation.clone();
    }

    public Location getLossOfControlLocation() {
        return lossOfControlLocation == null ? null : lossOfControlLocation.clone();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
