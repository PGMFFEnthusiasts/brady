package me.fireballs.cps.listener;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import me.fireballs.cps.profile.Profile;
import me.fireballs.cps.profile.ProfileManager;

public record ClickListener(ProfileManager profileManager) implements PacketListener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPacketType() instanceof PacketType.Play.Client type)) return;

        int entityId = event.getUser().getEntityId();
        Profile profile = profileManager.getProfile(entityId);
        if (profile == null) return;

        if (WrapperPlayClientPlayerFlying.isFlying(type)) {
            profile.tick();
        } else if (type == PacketType.Play.Client.ANIMATION) {
            profile.click();
        }
    }
}
