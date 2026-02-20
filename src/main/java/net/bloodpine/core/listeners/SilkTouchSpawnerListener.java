package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class SilkTouchSpawnerListener implements Listener {

    private final BloodpineCore plugin;

    public SilkTouchSpawnerListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("mechanics.silk-touch-spawners.enabled", true)) {
            return;
        }

        if (event.getBlock().getType() != Material.SPAWNER) {
            return;
        }

        if (event.getPlayer().getGameMode() == GameMode.CREATIVE || event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool == null || tool.getType().isAir()) {
            return;
        }

        if (tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) <= 0) {
            return;
        }

        BlockState state = event.getBlock().getState();
        if (!(state instanceof CreatureSpawner creatureSpawner)) {
            return;
        }

        EntityType spawnedType = creatureSpawner.getSpawnedType();

        ItemStack drop = new ItemStack(Material.SPAWNER, 1);
        ItemMeta meta = drop.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta) {
            BlockState itemState = blockStateMeta.getBlockState();
            if (itemState instanceof CreatureSpawner itemSpawner) {
                itemSpawner.setSpawnedType(spawnedType);
                blockStateMeta.setBlockState(itemSpawner);
                drop.setItemMeta(blockStateMeta);
            }
        }

        event.setDropItems(false);
        event.setExpToDrop(0);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5), drop);
    }
}
