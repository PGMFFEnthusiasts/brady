package me.fireballs.share.util;

import java.util.regex.Pattern;

public enum Action {
    PICKUPS("^⚠ .*?(\\w+) picked up the Flag!"),
    THROWS("^⚠ .*?(\\w+) threw the Football!"),
    PASSES(""),
    CATCHES("^⚠ .*?(\\w+) caught the Football!"),
    STRIPS("^⚠ .*?(\\w+) stripped the Flag!"),
    TOUCHDOWNS("^ » .*?(\\w+) takes it to the end zone!"),
    TOUCHDOWN_PASSES("");
    private final Pattern pattern;

    Action(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public Pattern getPattern() {
        return pattern;
    }
}