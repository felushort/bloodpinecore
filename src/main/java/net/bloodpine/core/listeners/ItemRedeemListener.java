package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.data.PlayerData;
import net.bloodpine.core.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ItemRedeemListener implements Listener {

    private final BloodpineCore plugin;

    public ItemRedeemListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        ItemStack item = event.getItem();
        
        // Check for right-click
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Check if it's a Token item
        if (ItemUtils.isTokenItem(item)) {
            cancelAndFixPhantomBlocks(event);
            redeemToken(player, item);
            return;
        }
        
        // Check if it's a Heart item
        if (ItemUtils.isHeartItem(item)) {
            cancelAndFixPhantomBlocks(event);
            if (!plugin.getConfig().getBoolean("lifesteal.enabled", true)) {
                player.sendMessage(colorize("&cBloodpine heart items are currently disabled."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            redeemHeart(player, item);
            return;
        }

        // Check if it's a Stat Boost Scroll
        if (ItemUtils.isStatBoostScroll(item)) {
            cancelAndFixPhantomBlocks(event);
            redeemStatBoostScroll(player, item);
            return;
        }
    }

    /**
     * General phantom-block prevention: runs AFTER all other plugins.
     * If anything (AuthMe, WorldGuard, etc.) cancels a RIGHT_CLICK_BLOCK
     * event, we send block-change updates so the client doesn't desync.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteractMonitor(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!event.isCancelled() && event.useItemInHand() != Event.Result.DENY) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        Player player = event.getPlayer();
        player.sendBlockChange(clicked.getLocation(), clicked.getBlockData());
        BlockFace face = event.getBlockFace();
        if (face != null && face != BlockFace.SELF) {
            Block adjacent = clicked.getRelative(face);
            player.sendBlockChange(adjacent.getLocation(), adjacent.getBlockData());
        }
        // Resync inventory so eating/item-use state is correct on the client
        player.updateInventory();
    }

    /**
     * Cancels the event and sends block-change updates to prevent phantom blocks
     * that appear on the client when a PlayerInteractEvent is cancelled while
     * right-clicking on a block (especially with a placeable item in the off-hand).
     * Setting Result.DENY on the item and block use explicitly signals Paper's
     * built-in phantom-block correction logic in addition to the manual block refresh.
     */
    private void cancelAndFixPhantomBlocks(PlayerInteractEvent event) {
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block clicked = event.getClickedBlock();
            BlockFace face = event.getBlockFace();
            event.getPlayer().sendBlockChange(clicked.getLocation(), clicked.getBlockData());
            if (face != null) {
                Block adjacent = clicked.getRelative(face);
                event.getPlayer().sendBlockChange(adjacent.getLocation(), adjacent.getBlockData());
            }
        }
    }

    private void redeemToken(Player player, ItemStack item) {
        int amount = item.getAmount();
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        // Check if player can receive tokens
        int currentTokens = data.getTotalTokens();
        int maxTokens = plugin.getConfig().getInt("tokens.max-total", 50);
        int availableSpace = maxTokens <= 0 ? Integer.MAX_VALUE : maxTokens - currentTokens;

        if (maxTokens > 0 && availableSpace <= 0) {
            player.sendMessage(colorize("&cYou've reached the maximum token limit! &7(" + maxTokens + " tokens)"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Calculate how many tokens can be redeemed
        int tokensToRedeem = Math.min(amount, availableSpace);
        
        // Remove items from inventory
        if (tokensToRedeem == amount) {
            item.setAmount(0); // Remove all
        } else {
            item.setAmount(amount - tokensToRedeem);
        }
        
        // Add tokens to player
        plugin.getTokenManager().giveTokens(player, tokensToRedeem);
        
        // Play sound and send message
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.sendMessage(colorize("&a&l✓ &aRedeemed &e" + tokensToRedeem + " Token" + (tokensToRedeem > 1 ? "s" : "") + "&a!"));
        
        if (tokensToRedeem < amount) {
            int remaining = amount - tokensToRedeem;
            player.sendMessage(colorize("&eYou could only redeem &6" + tokensToRedeem + "/" + amount + " &etokens due to the cap."));
        }
        
        // Update displays
        plugin.getDisplayManager().updateDisplay(player);
        plugin.getSidebarManager().updatePlayer(player);
    }

    private void redeemHeart(Player player, ItemStack item) {
        int amount = item.getAmount();
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        // Check if player can receive hearts
        double maxHearts = plugin.getConfig().getDouble("lifesteal.max-hearts", 20.0);
        double currentHearts = data.getLifestealHearts();
        double availableSpace = maxHearts <= 0 ? Double.MAX_VALUE : maxHearts - currentHearts;

        if (maxHearts > 0 && availableSpace <= 0) {
            player.sendMessage(colorize("&cYou've reached the maximum heart limit! &7(" + (int)maxHearts + " hearts)"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Calculate how many hearts can be redeemed
        int heartsToRedeem = Math.min(amount, (int)availableSpace);
        
        // Remove items from inventory
        if (heartsToRedeem == amount) {
            item.setAmount(0); // Remove all
        } else {
            item.setAmount(amount - heartsToRedeem);
        }
        
        // Add hearts to player
        for (int i = 0; i < heartsToRedeem; i++) {
            data.addLifestealHeart();
        }
        
        // Apply stats
        plugin.getStatManager().applyStats(player);
        
        // Play sound and send message
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        player.sendMessage(colorize("&c&l❤ &aAbsorbed &c" + heartsToRedeem + " Heart" + (heartsToRedeem > 1 ? "s" : "") + "&a!"));
        
        if (heartsToRedeem < amount) {
            int remaining = amount - heartsToRedeem;
            player.sendMessage(colorize("&eYou could only absorb &6" + heartsToRedeem + "/" + amount + " &ehearts due to the cap."));
        }
        
        // Update displays
        plugin.getDisplayManager().updateDisplay(player);
        plugin.getSidebarManager().updatePlayer(player);
    }

    private void redeemStatBoostScroll(Player player, ItemStack item) {
        int amount = item.getAmount();
        if (amount <= 0) {
            return;
        }

        item.setAmount(amount - 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 60, 0, true, true, true));
        player.sendMessage(colorize("&d&l✦ Stat Boost activated &7(Strength I + Regeneration I for 60s)"));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
