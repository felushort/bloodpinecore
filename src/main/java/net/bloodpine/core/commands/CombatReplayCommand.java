package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.managers.GameplayExpansionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CombatReplayCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public CombatReplayCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            plugin.getExpansionGUIManager().openCombatReplay(player);
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("spectate")) {
            if (!player.hasPermission("bloodpine.admin") && !player.isOp()) {
                player.sendMessage(colorize("&cOnly staff can use spectate mode."));
                return true;
            }
            Player target = player.getServer().getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(colorize("&cTarget not online."));
                return true;
            }
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(target.getLocation());
            player.sendMessage(colorize("&7Now spectating combat near &f" + target.getName()));
            return true;
        }

        List<GameplayExpansionManager.DamageLog> logs = plugin.getGameplayExpansionManager().getIncomingDamage(player, 8);
        if (logs.isEmpty()) {
            player.sendMessage(colorize("&7No recent combat replay available."));
            return true;
        }

        player.sendMessage(colorize("&c&lCombat Replay &8(Last damage breakdown)"));
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        for (GameplayExpansionManager.DamageLog log : logs) {
            String time = format.format(new Date(log.timestamp()));
            player.sendMessage(colorize("&8[" + time + "] &f" + log.source() + " &7dealt &c"
                    + String.format("%.2f", log.damage()) + " &7via &e" + log.cause()));
        }

        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
