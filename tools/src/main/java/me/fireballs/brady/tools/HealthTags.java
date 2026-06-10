package me.fireballs.brady.tools;

import me.fireballs.brady.core.BooleanSettingValue;
import me.fireballs.brady.core.tag.TagAPI;
import me.fireballs.brady.core.tag.TagLayer;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class HealthTags {

    private final BooleanSettingValue setting;

    public HealthTags() {
        ToolsSettings settings = KoinJavaComponent.get(ToolsSettings.class);
        this.setting = settings.getHealthTags();

        TagAPI tagAPI = KoinJavaComponent.get(TagAPI.class);
        tagAPI.register((new TagLayer("health", 2, this::renderHearts, this::showToViewer)));
    }

    private String renderHearts(Player target) {
        double absorption = ((CraftPlayer) target).getHandle().getAbsorptionHearts();
        return hearts(absorption, ChatColor.GOLD, ChatColor.YELLOW) + hearts(target.getHealth(), ChatColor.DARK_RED, ChatColor.RED);
    }

    private static String hearts(double halves, ChatColor fullColor, ChatColor halfColor) {
        int rounded = (int) Math.round(halves);
        int full = rounded / 2;

        var sb = new StringBuilder();
        if (full > 0) sb.append(fullColor).append("❤".repeat(full));
        if (rounded % 2 != 0) sb.append(halfColor).append("❤");
        return sb.toString();
    }

    private boolean showToViewer(Player target, Player viewer) {
        if (!setting.retrieveValue(viewer)) return false;

        MatchPlayer targetPlayer = PGM.get().getMatchManager().getPlayer(target);
        if (targetPlayer == null || !targetPlayer.isParticipating()) return false;

        MatchPlayer viewerPlayer = PGM.get().getMatchManager().getPlayer(viewer);
        return viewerPlayer != null && viewerPlayer.isObserving();
    }
}
