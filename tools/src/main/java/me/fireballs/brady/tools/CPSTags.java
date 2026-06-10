package me.fireballs.brady.tools;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import me.fireballs.brady.core.BooleanSettingValue;
import me.fireballs.brady.core.tag.TagAPI;
import me.fireballs.brady.core.tag.TagLayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jctools.maps.NonBlockingHashMap;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.events.PlayerJoinPartyEvent;

import java.util.Map;

import static me.fireballs.brady.core.PluginExtensionsKt.registerPacketEvents;

public class CPSTags extends PacketListenerAbstract implements Listener {

    private final BooleanSettingValue setting;

    private final Map<Integer, Profile> profiles = new NonBlockingHashMap<>();

    public CPSTags() {
        Tools plugin = KoinJavaComponent.get(Tools.class);
        registerPacketEvents(plugin, this);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        ToolsSettings settings = KoinJavaComponent.get(ToolsSettings.class);
        this.setting = settings.getCpsTags();

        TagAPI tagAPI = KoinJavaComponent.get(TagAPI.class);
        tagAPI.register(new TagLayer("cps", 0, this::getCPS, (_, viewer) -> setting.retrieveValue(viewer)));
    }

    private String getCPS(Player target) {
        Profile profile = profiles.get(target.getEntityId());
        if (profile == null) return "";
        return profile.teamColor.toString() + profile.cps + ChatColor.GRAY + " CPS";
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        profiles.put(event.getUser().getEntityId(), new Profile());
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        profiles.remove(event.getUser().getEntityId());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPacketType() instanceof PacketType.Play.Client type)) return;

        Profile profile = profiles.get(event.getUser().getEntityId());
        if (profile == null) return;

        if (WrapperPlayClientPlayerFlying.isFlying(type)) {
            profile.tick();
        } else if (type == PacketType.Play.Client.ANIMATION) {
            profile.click();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeamChange(PlayerJoinPartyEvent event) {
        Profile profile = profiles.get(event.getPlayer().getBukkit().getEntityId());
        if (profile == null) return;

        Party party = event.getNewParty();
        profile.teamColor = party == null ? ChatColor.AQUA : party.getColor();
    }

    private static class Profile {

        private final int[] clickBuffer = new int[20];
        private int index;
        private int count;

        private volatile int cps;
        private ChatColor teamColor = ChatColor.AQUA;

        private void click() {
            clickBuffer[index]++;
            count++;
            cps = count;
        }

        private void tick() {
            index = (index + 1) % 20;
            count -= clickBuffer[index];
            clickBuffer[index] = 0;
            cps = count;
        }
    }
}
