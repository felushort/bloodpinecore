package net.bloodpine.core.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemMetaBuilder {

    private ItemMetaBuilder() {}

    public static void applyIdentity(ItemStack item, org.bukkit.entity.Player owner, int kills, int rebirthLevel) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String tierName = tierForRebirth(rebirthLevel);
        String baseName = item.getType().name().replace("_", " ");

        meta.setDisplayName(colorize("&c" + owner.getName() + "'s " + tierName + " &f" + title(baseName)));

        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Rebirth Tier: &d" + rebirthLevel));
        lore.add(colorize("&7Weapon Kills: &e" + kills));
        lore.add(colorize("&8A weapon forged in Bloodpine chaos."));

        if (kills >= 15) {
            lore.add(colorize("&6Glow Upgrade I"));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (kills >= 50) {
            lore.add(colorize("&5Glow Upgrade II"));
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private static String tierForRebirth(int rebirth) {
        if (rebirth >= 15) return "Mythic";
        if (rebirth >= 10) return "Ancient";
        if (rebirth >= 5) return "Ascendant";
        if (rebirth >= 2) return "Bloodforged";
        return "Survivor";
    }

    private static String title(String text) {
        String[] parts = text.toLowerCase().split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return builder.toString().trim();
    }

    private static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
