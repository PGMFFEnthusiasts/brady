package me.fireballs.cps.manager;

import com.github.retrooper.packetevents.protocol.player.User;
import me.fireballs.cps.data.ClientData;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDataManager {
    private final Map<User, ClientData> dataMap = new ConcurrentHashMap<>();

    public Optional<ClientData> getData(User user) {
        return Optional.ofNullable(dataMap.get(user));
    }

    public void add(User user) {
        dataMap.put(user, new ClientData());
    }

    public void remove(User user) {
        dataMap.remove(user);
    }
}
