package me.fireballs.share.command;

import me.fireballs.share.util.FootballDebugChannel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FootballDebugCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;
        final boolean added = FootballDebugChannel.togglePlayer(player);
        if (added) player.sendMessage("Added to debug channel");
        else player.sendMessage("Removed from debug channel");
        return true;
    }
}
