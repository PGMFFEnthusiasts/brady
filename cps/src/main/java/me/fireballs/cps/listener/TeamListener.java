package me.fireballs.cps.listener;

import me.fireballs.cps.profile.Profile;
import me.fireballs.cps.profile.ProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.events.PlayerJoinPartyEvent;

public record TeamListener(ProfileManager profileManager) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeamChange(PlayerJoinPartyEvent event) {
        int entityId = event.getPlayer().getBukkit().getEntityId();
        Profile profile = profileManager.getProfile(entityId);
        if (profile == null) return;

        Party party = event.getNewParty();
        ChatColor color = party == null ? ChatColor.AQUA : party.getColor();
        profile.setTeamColor(color);
    }
}
