package me.fireballs.cps;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.fireballs.cps.listener.ClickListener;
import me.fireballs.cps.listener.LoginListener;
import me.fireballs.cps.listener.TagListener;
import me.fireballs.cps.listener.TeamListener;
import me.fireballs.cps.profile.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CPSPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ProfileManager profileManager = new ProfileManager();
        EventManager eventManager = PacketEvents.getAPI().getEventManager();
        PluginManager pluginManager = Bukkit.getPluginManager();

        eventManager.registerListener(new LoginListener(profileManager), PacketListenerPriority.MONITOR);
        eventManager.registerListener(new ClickListener(profileManager), PacketListenerPriority.MONITOR);
        eventManager.registerListener(new TagListener(profileManager), PacketListenerPriority.MONITOR);

        pluginManager.registerEvents(new TeamListener(profileManager), this);
    }
}
