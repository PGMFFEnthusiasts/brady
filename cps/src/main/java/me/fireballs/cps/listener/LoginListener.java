package me.fireballs.cps.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import me.fireballs.cps.profile.ProfileManager;

public record LoginListener(ProfileManager profileManager) implements PacketListener {

    @Override
    public void onUserLogin(UserLoginEvent event) {
        int entityId = event.getUser().getEntityId();
        profileManager.addProfile(entityId);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        int entityId = event.getUser().getEntityId();
        profileManager.removeProfile(entityId);
    }
}
