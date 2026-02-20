package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventsCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public EventsCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;

        if (args.length == 0) {
            if (player == null) {
                sender.sendMessage(colorize("&cUsage: /events start <bloodmoon|arena|hunt|boss|finalhour|supplydrop>"));
                sender.sendMessage(colorize("&cUsage: /events stop <bloodmoon|arena|hunt|boss|finalhour|all>"));
                return true;
            }
            plugin.getExpansionGUIManager().openEvents(player);
            player.sendMessage(plugin.getGameplayExpansionManager().getEventStatusLine());
            return true;
        }

        boolean canForce = (player != null && player.isOp()) || !(sender instanceof Player);
        if (!canForce) {
            player.sendMessage(colorize("&cOnly OPs can force-start events."));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("start")) {
            switch (args[1].toLowerCase()) {
                case "bloodmoon" -> plugin.getGameplayExpansionManager().startBloodMoon();
                case "arena" -> plugin.getGameplayExpansionManager().startArenaEvent();
                case "hunt" -> plugin.getGameplayExpansionManager().startHuntMarked();
                case "boss" -> plugin.getGameplayExpansionManager().spawnBoss();
                case "finalhour" -> plugin.getGameplayExpansionManager().startFinalHourEventManually();
                case "supplydrop", "supply" -> plugin.getGameplayExpansionManager().forceSupplyDrop();
                default -> sender.sendMessage(colorize("&cUnknown event."));
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("stop") && args.length >= 2) {
            switch (args[1].toLowerCase()) {
                case "arena" -> plugin.getGameplayExpansionManager().stopArenaEvent();
                case "bloodmoon" -> {
                    plugin.getGameplayExpansionManager().stopBloodMoon();
                    sender.sendMessage(colorize("&7Blood Moon stopped."));
                }
                case "hunt" -> {
                    plugin.getGameplayExpansionManager().stopHuntMarked();
                    sender.sendMessage(colorize("&7Hunt the Marked stopped."));
                }
                case "boss" -> {
                    plugin.getGameplayExpansionManager().despawnBoss();
                    sender.sendMessage(colorize("&7World boss despawned."));
                }
                case "finalhour" -> plugin.getGameplayExpansionManager().stopFinalHourEventManually();
                case "all" -> plugin.getGameplayExpansionManager().stopAllEvents(true);
                case "supplydrop", "supply" -> sender.sendMessage(colorize("&eSupply drops are one-shot events. Use &f/events start supplydrop&e."));
                default -> sender.sendMessage(colorize("&cUnknown stop event. Use: bloodmoon, arena, hunt, boss, finalhour, all"));
            }
            return true;
        }

        sender.sendMessage(colorize("&cUsage: /events start <bloodmoon|arena|hunt|boss|finalhour|supplydrop>"));
        sender.sendMessage(colorize("&cUsage: /events stop <bloodmoon|arena|hunt|boss|finalhour|all>"));
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
