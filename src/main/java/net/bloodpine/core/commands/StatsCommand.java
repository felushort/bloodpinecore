package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.data.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {
    
    private final BloodpineCore plugin;
    
    public StatsCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cPlayer not found!"));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cPlease specify a player!"));
                return true;
            }
            target = (Player) sender;
        }
        
        PlayerData data = plugin.getDataManager().getPlayerData(target);
        
        sender.sendMessage(colorize("&c&m-----&r &c&l" + target.getName() + "'s Stats &c&m-----"));
        sender.sendMessage(colorize("&7Tokens: &e" + data.getAvailableTokens() + " &7available / &e" + 
            data.getTotalTokens() + " &7total"));
        sender.sendMessage("");
        sender.sendMessage(colorize("&c⚔ Damage: &e" + data.getAllocatedTokens(StatType.DAMAGE) + 
            "&7/" + plugin.getStatManager().getMaxTokensForStat(StatType.DAMAGE) + 
            " &7(+" + (data.getAllocatedTokens(StatType.DAMAGE) * 
            plugin.getConfig().getDouble("stats.damage.per-token", 1.0)) + "%)"));
        
        sender.sendMessage(colorize("&9🛡 Defense: &e" + data.getAllocatedTokens(StatType.DEFENSE) + 
            "&7/" + plugin.getStatManager().getMaxTokensForStat(StatType.DEFENSE) + 
            " &7(-" + (data.getAllocatedTokens(StatType.DEFENSE) * 
            plugin.getConfig().getDouble("stats.defense.per-token", 1.0)) + "%)"));
        
        sender.sendMessage(colorize("&6🗿 Totem: &e" + data.getAllocatedTokens(StatType.TOTEM) + 
            "&7/" + plugin.getStatManager().getMaxTokensForStat(StatType.TOTEM) + 
            " &7(-" + (data.getAllocatedTokens(StatType.TOTEM) * 
            plugin.getConfig().getInt("stats.totem.per-token", 50)) + "ms)"));
        
        sender.sendMessage(colorize("&a❤ Vitality: &e" + data.getAllocatedTokens(StatType.VITALITY) + 
            "&7/" + plugin.getStatManager().getMaxTokensForStat(StatType.VITALITY) + 
            " &7(+" + (data.getAllocatedTokens(StatType.VITALITY) * 
            plugin.getConfig().getDouble("stats.vitality.per-token", 1.0)) + " hearts)"));
        
        sender.sendMessage("");
        sender.sendMessage(colorize("&7Kills: &e" + data.getTotalKills() + " &7| Deaths: &e" + 
            data.getTotalDeaths() + " &7| K/D: &e" + String.format("%.2f", data.getKDRatio())));
        
        if (data.isMarked()) {
            sender.sendMessage(colorize("&c&l⚠ MARKED ⚠"));
        }
        
        sender.sendMessage(colorize("&c&m---------------------------"));
        
        return true;
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
