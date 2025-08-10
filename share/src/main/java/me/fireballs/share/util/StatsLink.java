package me.fireballs.share.util;

import org.jetbrains.annotations.NotNull;

public record StatsLink(
        String url,
        String source
) {
    @Override
    public @NotNull String toString() {
        return "StatsLink{" +
                "url='" + url + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
