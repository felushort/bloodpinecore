package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class CombatCrateCommand implements CommandExecutor {

    private final BloodpineCore plugin;
    private final Random random = new Random();

    public CombatCrateCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            plugin.getExpansionGUIManager().openEconomy(player);
            player.sendMessage(colorize("&7Click the crate in GUI or use &f/combatcrate open"));
            return true;
        }

        if (!args[0].equalsIgnoreCase("open")) {
            player.sendMessage(colorize("&cUsage: /combatcrate open"));
            return true;
        }

        int cost = plugin.getConfig().getInt("token-sinks.combat-crate-cost", 7);
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        if (data.getAvailableTokens() < cost) {
            player.sendMessage(colorize("&cNeed " + cost + " available tokens."));
            return true;
        }

        plugin.getTokenManager().removeTokens(player, cost);

        int roll = random.nextInt(6);
        switch (roll) {
            case 0 -> player.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
            case 1 -> player.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
            case 2 -> player.getInventory().addItem(ItemUtils.createTokenItem(3));
            case 3 -> plugin.getBoostManager().giveHeartShield(player, 1);
            case 4 -> plugin.getBoostManager().giveTokenBoost(player, 10);
            default -> player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 8));
        }

        player.sendMessage(colorize("&6Combat crate opened."));
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
