package me.fireballs.brady.tools;

import kotlin.Lazy;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.channels.AdminChannel;
import tc.oc.pgm.channels.PrivateMessageChannel;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.match.ObserverParty;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.named.Named;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static me.fireballs.brady.core.PluginExtensionsKt.registerEvents;
import static net.kyori.adventure.text.Component.text;

public class Ready implements Listener {
    private static final Set<String> READY_MESSAGES = Set.of("r", "ready");
    private static final Set<String> NOT_READY_MESSAGES = Set.of("not r", "not ready", "unready");
    private static final Set<String> READY_QUESTIONS = Set.of("r?", "ready?");

    private boolean listening = true;
    private boolean sentHint = false;

    private final Comparator<MatchPlayer> playerComparator = Comparator
            .comparing((MatchPlayer p) -> p.getParty().getNameLegacy())
            .thenComparing(Named::getNameLegacy);

    private final Set<MatchPlayer> ready = new TreeSet<>(playerComparator);
    private final Set<MatchPlayer> notReady = new TreeSet<>(playerComparator);

    public Ready() {
        Lazy<Tools> plugin = KoinJavaComponent.inject(Tools.class);
        registerEvents(plugin.getValue(), this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChat(ChannelMessageEvent<?> event) {
        if (!listening) return;
        if (event.getChannel() instanceof AdminChannel || event.getChannel() instanceof PrivateMessageChannel) return;

        MatchPlayer player = event.getSender();
        String message = event.getMessage().toLowerCase();

        var found = false;
        if (READY_MESSAGES.contains(message) && player.getCompetitor() != null) {
            setReady(player, true);
            appendStatus(event, NamedTextColor.GREEN);
            found = true;
        } else if (NOT_READY_MESSAGES.contains(message) && player.getCompetitor() != null) {
            setReady(player, false);
            appendStatus(event, NamedTextColor.RED);
            found = true;
        } else if (READY_QUESTIONS.contains(message)) {
            appendStatus(event, NamedTextColor.AQUA);
            found = true;
        }

        if (!found) return;

        var remainingViewers = new HashSet<>(event.getSender().getMatch().getPlayers());
        remainingViewers.removeAll(event.getViewers());

        for (MatchPlayer remainingViewer : remainingViewers) {
            var component = PGM.get().getChatManager().getGlobalChannel()
                    .formatMessage(null, event.getSender(), event.getComponent());
            remainingViewer.sendMessage(component);
        }
    }

    @EventHandler
    public void onJoinLeave(PlayerPartyChangeEvent event) {
        MatchPlayer player = event.getPlayer();

        if (event.getOldParty() == null || event.getNewParty() == null) {
            clear(player);
        } else if (event.getOldParty() instanceof ObserverParty) {
            setReady(player, false);
        } else if (event.getNewParty() instanceof ObserverParty) {
            clear(player);
        }
    }

    @EventHandler
    public void onStart(MatchStartEvent event) {
        clearAll();
        listening = false;
    }

    @EventHandler
    public void onCycle(MatchLoadEvent event) {
        clearAll();
        listening = true;
        sentHint = false;
    }

    private void setReady(MatchPlayer player, boolean isReady) {
        var wasReady = notReady.isEmpty();

        if (isReady) {
            ready.add(player);
            notReady.remove(player);
        } else {
            ready.remove(player);
            notReady.add(player);
        }

        if (wasReady && !notReady.isEmpty() && listening) player.getMatch().getCountdown().cancelAll();
        if (!wasReady && notReady.isEmpty() && listening) player.getMatch().moduleRequire(StartMatchModule.class)
                .forceStartCountdown(Duration.ofSeconds(30), Duration.ZERO);
    }

    private void clear(MatchPlayer player) {
        ready.remove(player);
        notReady.remove(player);
    }

    private void clearAll() {
        ready.clear();
        notReady.clear();
    }

    private void appendStatus(ChannelMessageEvent<?> event, NamedTextColor color) {
        boolean appendHint = !sentHint;
        sentHint = true;

        event.setComponent(
                event.getComponent()
                        .append(text(" " + ready.size(), color))
                        .append(text("/", NamedTextColor.GRAY))
                        .append(text(ready.size() + notReady.size(), NamedTextColor.AQUA))
                        .append(text(appendHint ? " [hover]" : "", NamedTextColor.GRAY)
                                .decorate(TextDecoration.ITALIC))
                        .hoverEvent(getHover(ready, notReady))
        );
    }

    private HoverEvent<?> getHover(Set<MatchPlayer> ready, Set<MatchPlayer> notReady) {
        TextComponent.Builder hover = text();

        hover.append(text("Ready", NamedTextColor.GREEN));
        appendPlayers(hover, ready);

        hover.appendNewline().appendNewline();

        hover.append(text("Not Ready", NamedTextColor.RED));
        appendPlayers(hover, notReady);

        return HoverEvent.showText(hover.build());
    }


    private void appendPlayers(TextComponent.Builder builder, Set<MatchPlayer> players) {
        for (MatchPlayer player : players) {
            builder.appendNewline();

            NamedTextColor teamColor = NamedTextColor.AQUA;
            if (player.getCompetitor() instanceof Team team) {
                teamColor = team.getTextColor();
            }

            builder.append(text(" ⦿", teamColor));
            builder.append(text(" " + player.getNameLegacy(), NamedTextColor.AQUA));
        }
    }
}
