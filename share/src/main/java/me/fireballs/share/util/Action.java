package me.fireballs.share.util;

import java.util.Arrays;
import java.util.regex.Pattern;

public enum Action {
    PICKUPS("^⚠ .*?(\\w+) picked up the Flag!"),
    THROWS("^⚠ .*?(\\w+) threw the Football!"),
    PASSES(""),
    CATCHES("^⚠ .*?(\\w+) caught the Football!"),
    STRIPS("^⚠ .*?(\\w+) stripped the Flag!"),
    TOUCHDOWNS("^ » .*?(\\w+) takes it to the end zone!", "^ » .*?(\\w+) scores a touchdown!"),
    TOUCHDOWN_PASSES("");
    private final Pattern[] patterns;

    Action(String... regexes) {
        this.patterns = Arrays.stream(regexes)
                .map(Pattern::compile)
                .toArray(Pattern[]::new);
    }

    public Pattern[] getPatterns() {
        return patterns;
    }
}