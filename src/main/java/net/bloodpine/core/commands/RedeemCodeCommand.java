package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.managers.RedeemCodeManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedeemCodeCommand implements CommandExecutor {

    private final BloodpineCore plugin;

    public RedeemCodeCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cOnly players can use this command."));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(colorize("&cUsage: /redeem <code>"));
            return true;
        }

        String code = args[0].trim().toUpperCase();
        RedeemCodeManager.RedeemReward reward = plugin.getRedeemCodeManager().getReward(code);
        if (reward == null) {
            player.sendMessage(colorize("&cInvalid code."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        if (data.hasRedeemedCode(code)) {
            player.sendMessage(colorize("&eYou already redeemed this code."));
            return true;
        }

        int tokenReward = reward.tokens();
        int heartReward = reward.hearts();

        data.markCodeRedeemed(code);
        if (tokenReward > 0) {
            plugin.getTokenManager().giveTokens(player, tokenReward);
        }

        int maxHearts = plugin.getConfig().getInt("lifesteal.max-hearts", 22);
        int startingHearts = plugin.getConfig().getInt("lifesteal.starting-hearts", 10);
        int currentTotalHearts = startingHearts + data.getLifestealHearts();
        int grantedHearts = 0;
        for (int index = 0; index < heartReward; index++) {
            currentTotalHearts = startingHearts + data.getLifestealHearts();
            if (maxHearts > 0 && currentTotalHearts >= maxHearts) {
                break;
            }
            data.addLifestealHeart();
            grantedHearts++;
        }
        heartReward = grantedHearts;

        plugin.getStatManager().applyStats(player);

        List<ItemStack> rewardItems = new ArrayList<>();
        if (reward.totems() > 0) {
            rewardItems.add(new ItemStack(Material.TOTEM_OF_UNDYING, reward.totems()));
        }
        if (reward.goldenApples() > 0) {
            rewardItems.add(new ItemStack(Material.GOLDEN_APPLE, reward.goldenApples()));
        }
        if (reward.experienceBottles() > 0) {
            rewardItems.add(new ItemStack(Material.EXPERIENCE_BOTTLE, reward.experienceBottles()));
        }

        Map<Integer, ItemStack> leftovers = rewardItems.isEmpty()
                ? Map.of()
                : player.getInventory().addItem(rewardItems.toArray(new ItemStack[0]));
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        plugin.getDisplayManager().updateDisplay(player);
        plugin.getSidebarManager().updatePlayer(player);
        plugin.getDataManager().saveData();

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.sendMessage(colorize("&a&lCODE REDEEMED &7- &f" + code));
        player.sendMessage(colorize("&7Rewards: &e+" + tokenReward + " tokens&7, &c+" + heartReward + " heart(s)&7, &f+" + reward.totems() + " totem(s)&7, &6+" + reward.goldenApples() + " gapple(s)&7, &a+" + reward.experienceBottles() + " XP bottle(s)"));
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
