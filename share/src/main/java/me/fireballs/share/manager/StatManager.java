package me.fireballs.share.manager;

import me.fireballs.share.util.FootballStatistic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

public class StatManager {
    private final Map<FootballStatistic, Map<UUID, Double>> ACTION_MAP = new HashMap<>();

    public StatManager() {
        Arrays.stream(FootballStatistic.values()).forEach(footballStatistic -> ACTION_MAP.put(footballStatistic, new HashMap<>()));
    }

    public void incrementStat(UUID uuid, FootballStatistic footballStatistic) {
        mergeStat(uuid, footballStatistic, 1.0, Double::sum);
    }

    public void incrementStat(UUID uuid, FootballStatistic footballStatistic, double quantity) {
        mergeStat(uuid, footballStatistic, quantity, Double::sum);
    }

    public void mergeStat(
        UUID uuid, FootballStatistic footballStatistic,
        final Double value,
        BiFunction<? super Double, ? super Double, ? extends Double> remappingFunction
    ) {
        ACTION_MAP.get(footballStatistic).merge(uuid, value, remappingFunction);
    }

    public double getStat(UUID uuid, FootballStatistic footballStatistic) {
        return ACTION_MAP.get(footballStatistic).getOrDefault(uuid, 0.0);
    }

    public void clearStats() {
        ACTION_MAP.values().forEach(Map::clear);
    }
}
