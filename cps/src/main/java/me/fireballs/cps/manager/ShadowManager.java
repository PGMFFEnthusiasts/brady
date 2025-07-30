package me.fireballs.cps.manager;

import me.fireballs.cps.data.ShadowData;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ShadowManager {
    private final Map<Integer, ShadowData> dataMap = new ConcurrentHashMap<>();

    public Optional<ShadowData> getData(Integer id) {
        return Optional.ofNullable(dataMap.get(id));
    }

    public void add(Integer id) {
        dataMap.put(id, new ShadowData());
    }

    public void remove(Integer id) {
        dataMap.remove(id);
    }
}
