package me.fireballs.share.manager;

import com.google.common.collect.MapMaker;
import me.fireballs.share.data.ClientData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClientDataManager {
    private final Map<UUID, ClientData> dataMap = new MapMaker().makeMap();

    public Optional<ClientData> getData(UUID uuid) {
        return Optional.ofNullable(dataMap.get(uuid));
    }

    public void add(UUID uuid) {
        dataMap.put(uuid, new ClientData());
    }

    public void remove(UUID uuid) {
        dataMap.remove(uuid);
    }
}
