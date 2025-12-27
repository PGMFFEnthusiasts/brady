package me.fireballs.share.command;

import me.fireballs.share.effects.PersistentEffectRegistrationSubscriber;
import me.fireballs.share.effects.PotionEffectApplication;
import me.fireballs.share.util.FootballDebugChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static me.fireballs.brady.core.PermissionKt.testPerm;
import static me.fireballs.brady.core.PlayerExtensionsKt.send;

/**
 * Adds persistent effects to ppl during the game
 */
public class PersistentEffectsCommand implements CommandExecutor, TabExecutor {
    public static final String[] COMMAND_ALIASES = {"pe", "persistenteffects"};
    public static final String[] VALID_SUBCOMMANDS = {"give", "list", "remove"};
    private static final Component USAGE_MESSAGE =
        Component.text(
                "/" + COMMAND_ALIASES[0] + " list"
        ).color(NamedTextColor.RED).appendNewline().append(
            Component.text(
            "/" + COMMAND_ALIASES[0] + " give <effect> [<amplifier>]"
            ).color(NamedTextColor.RED)
        ).appendNewline().append(
            Component.text(
                "/" + COMMAND_ALIASES[0] + " remove <effect>"
            ).color(NamedTextColor.RED)
        );
    private final Map<PotionEffectType, Integer> outstandingEffects;
    private final List<PersistentEffectRegistrationSubscriber> effectRegistrationSubscribers;

    public PersistentEffectsCommand(
        Map<PotionEffectType, Integer> outstandingEffects,
        List<PersistentEffectRegistrationSubscriber> effectRegistrationSubscribers
    ) {
        this.outstandingEffects = outstandingEffects;
        this.effectRegistrationSubscribers = effectRegistrationSubscribers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (!testPerm(sender, "tb.effects")) return false;
        if (args.length == 0 || !Arrays.asList(VALID_SUBCOMMANDS).contains(args[0].toLowerCase())) {
            send(player, USAGE_MESSAGE);
            return true;
        }
        final String action = args[0];
        if ("list".equalsIgnoreCase(args[0])) {
            send(player,getActiveEffectsMessage());
            return true;
        }

        if (args.length < 2) {
            send(player, USAGE_MESSAGE);
            return true;
        }

        if (PotionEffectType.getByName(args[1]) == null) {
            send(player, Component.text("Invalid potion effect type").color(NamedTextColor.RED));
            return true;
        }

        final PotionEffectType potionEffectType = PotionEffectType.getByName(args[1]);
        if ("remove".equalsIgnoreCase(action)) {
            removePotionEffect(potionEffectType);
            send(player, Component.text("Removed the effect").color(NamedTextColor.GREEN));
        } else {
            final int amplifier = args.length >= 3 && StringUtils.isNumeric(args[2]) ?
                Integer.parseInt(args[2]) : 1;
            addOrReplacePotionEffect(new PotionEffectApplication(potionEffectType, amplifier));
            send(player, Component.text("Added the effect").color(NamedTextColor.GREEN));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }
        if (args.length == 1) return processedCompletions(args[0], List.of("give", "list", "remove"));
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return processedCompletions(
                args[1],
                Arrays.stream(PotionEffectType.values())
                    .filter(Objects::nonNull)
                    .map(
                        pet -> pet.getName().toLowerCase()
                    ).collect(Collectors.toList())
            );
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return processedCompletions(
                args[1],
                outstandingEffects.keySet()
                    .stream()
                    .map(pet -> pet.getName().toLowerCase().trim()).collect(Collectors.toList())
            );
        }
        return Collections.emptyList();
    }

    private List<String> processedCompletions(final String prefix, final List<String> sourceCompletions) {
        return sourceCompletions.stream()
            .filter(completion -> completion.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }

    private void addOrReplacePotionEffect(PotionEffectApplication potionEffectApplication) {
        outstandingEffects.put(potionEffectApplication.effectType(), potionEffectApplication.potency());
        effectRegistrationSubscribers.forEach(ers -> ers.onEffectAdd(potionEffectApplication));
    }

    private void removePotionEffect(PotionEffectType potionEffectType) {
        final boolean removed = outstandingEffects.remove(potionEffectType) != null;
        if (removed) {
            effectRegistrationSubscribers.forEach(ers -> ers.onEffectRemove(potionEffectType));
        }
    }

    private Component getActiveEffectsMessage() {
        return outstandingEffects.entrySet().stream().map(entry ->
            Component.text(
                "- " + entry.getKey().getName() + " " + entry.getValue()
            ).color(NamedTextColor.AQUA)
        ).reduce(
            (c1, c2) -> c1.appendNewline().append(c2))
                .orElse(Component.text("No active effects").color(NamedTextColor.RED)
        );
    }
}