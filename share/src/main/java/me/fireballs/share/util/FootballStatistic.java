package me.fireballs.share.util;

import java.util.Arrays;
import java.util.regex.Pattern;

public enum FootballStatistic {
    PICKUPS("^⚠ .*?(\\w+) picked up the Flag!"),
    THROWS("^⚠ .*?(\\w+) threw the Football!"),
    PASSES(""),
    CATCHES("^⚠ .*?(\\w+) caught the Football!"),
    STRIPS("^⚠ .*?(\\w+) stripped the Flag!"),
    TOUCHDOWNS("^ » .*?(\\w+) takes it to the end zone!", "^ » .*?(\\w+) scores a touchdown!"),
    TOUCHDOWN_PASSES(""),
    MAX_PASSING_BLOCKS,
    MAX_RECEIVING_BLOCKS;
    private final Pattern[] chatPatterns;

    FootballStatistic(String... regexes) {
        this.chatPatterns = Arrays.stream(regexes)
                .map(Pattern::compile)
                .toArray(Pattern[]::new);
    }

    public Pattern[] getChatPatterns() {
        return chatPatterns;
    }
}