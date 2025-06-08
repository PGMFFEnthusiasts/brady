package me.fireballs.share.manager;

import me.fireballs.share.util.Action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatManager {
    private final Map<Action, Map<UUID, Integer>> ACTION_MAP = new HashMap<>();

    public StatManager() {
        Arrays.stream(Action.values()).forEach(action -> ACTION_MAP.put(action, new HashMap<>()));
    }

    public void incrementStat(UUID uuid, Action action) {
        ACTION_MAP.get(action).merge(uuid, 1, Integer::sum);
    }

    public int getStat(UUID uuid, Action action) {
        return ACTION_MAP.get(action).getOrDefault(uuid, 0);
    }

    public void clearStats() {
        ACTION_MAP.values().forEach(Map::clear);
    }
}
