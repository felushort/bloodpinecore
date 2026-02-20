package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class BlackMarketCommand implements CommandExecutor {

    private final BloodpineCore plugin;
    private final Random random = new Random();

    public BlackMarketCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int unlock = plugin.getConfig().getInt("black-market.unlock-tokens", 30);

        if (data.getTotalTokens() < unlock) {
            player.sendMessage(colorize("&cBlack Market is locked. Need " + unlock + "+ total tokens."));
            return true;
        }

        if (args.length == 0) {
            plugin.getExpansionGUIManager().openBlackMarket(player);
            return true;
        }

        int available = data.getAvailableTokens();

        switch (args[0].toLowerCase()) {
            case "warp" -> {
                World world = player.getWorld();
                String configuredWorld = plugin.getConfig().getString("black-market.world", world.getName());
                World destinationWorld = player.getServer().getWorld(configuredWorld);
                if (destinationWorld == null) {
                    player.sendMessage(colorize("&cBlack Market warp world is not available."));
                    return true;
                }
                Location destination = new Location(destinationWorld,
                        plugin.getConfig().getDouble("black-market.x", 150),
                        plugin.getConfig().getDouble("black-market.y", 75),
                        plugin.getConfig().getDouble("black-market.z", 150));
                player.teleport(destination);
                player.sendMessage(colorize("&5You slip into the Black Market..."));
            }
            case "illegalbook" -> {
                int cost = plugin.getConfig().getInt("black-market.illegal-book-cost", 12);
                if (available < cost) {
                    player.sendMessage(colorize("&cNeed " + cost + " available tokens."));
                    return true;
                }
                plugin.getTokenManager().removeTokens(player, cost);

                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                meta.addStoredEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 6, true);
                meta.setDisplayName(colorize("&4Contraband Tome"));
                book.setItemMeta(meta);
                player.getInventory().addItem(book);
                player.sendMessage(colorize("&4You bought an illegal enchant tome."));
            }
            case "riskybuff" -> {
                int cost = plugin.getConfig().getInt("black-market.risky-buff-cost", 8);
                if (available < cost) {
                    player.sendMessage(colorize("&cNeed " + cost + " available tokens."));
                    return true;
                }
                plugin.getTokenManager().removeTokens(player, cost);

                boolean good = random.nextBoolean();
                if (good) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 180, 1, true, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 180, 0, true, true));
                    player.sendMessage(colorize("&aDeal favored you. Massive combat buff active."));
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 90, 0, true, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 90, 0, true, true));
                    player.sendMessage(colorize("&cThe market cursed you. Bad roll."));
                }
            }
            case "combatperk" -> {
                int cost = plugin.getConfig().getInt("black-market.combat-perk-cost", 10);
                if (available < cost) {
                    player.sendMessage(colorize("&cNeed " + cost + " available tokens."));
                    return true;
                }
                plugin.getTokenManager().removeTokens(player, cost);
                plugin.getBoostManager().giveTokenBoost(player, 15);
                plugin.getBoostManager().giveHeartShield(player, 1);
                player.sendMessage(colorize("&6Black Market combat perk activated."));
            }
            default -> player.sendMessage(colorize("&cUsage: /blackmarket <illegalbook|riskybuff|combatperk>"));
        }

        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
