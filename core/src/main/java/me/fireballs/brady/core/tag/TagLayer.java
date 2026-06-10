package me.fireballs.brady.core.tag;

import org.bukkit.entity.Player;

import java.util.function.BiPredicate;
import java.util.function.Function;

public record TagLayer(
        String id,
        int priority,
        Function<Player, String> text,
        BiPredicate<Player, Player> visibleTo
) {}
