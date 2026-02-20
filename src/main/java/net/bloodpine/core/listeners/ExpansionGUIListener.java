package net.bloodpine.core.listeners;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ExpansionGUIListener implements Listener {

    private final BloodpineCore plugin;

    public ExpansionGUIListener(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!(title.contains("PvP Stats")
            || title.contains("Bloodpine Hub")
                || title.contains("Learn Bloodpine")
                || title.contains("Events Panel")
                || title.contains("Black Market")
                || title.contains("Daily Quest")
                || title.contains("Contracts")
                || title.contains("Bounty Board")
                || title.contains("Ascension")
                || title.contains("Economy Sinks")
                || title.contains("Parkour Challenge")
                || title.contains("Season Arc")
                || title.contains("Combat Replay"))) {
            return;
        }

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String clicked = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (title.contains("Bloodpine Hub")) {
            if (clicked.contains("PvP Stats")) player.performCommand("pvpstats");
            else if (clicked.contains("Events")) player.performCommand("events");
            else if (clicked.contains("Black Market")) player.performCommand("blackmarket");
            else if (clicked.contains("Economy")) player.performCommand("gamble");
            else if (clicked.contains("Bounty Board")) player.performCommand("bountyboard");
            else if (clicked.contains("Season Arc")) player.performCommand("season");
            else if (clicked.contains("Learn Bloodpine")) player.performCommand("learn");
            else if (clicked.contains("Daily Quest")) player.performCommand("dailyquest");
            else if (clicked.contains("Contracts")) player.performCommand("contract");
            else if (clicked.contains("Ascension")) player.performCommand("ascend");
            else if (clicked.contains("Parkour")) player.performCommand("parkour");
            else if (clicked.contains("Combat Replay")) player.performCommand("combatreplay");
            else if (clicked.contains("Refresh")) plugin.getExpansionGUIManager().openMainHub(player);
        }

        if (title.contains("Learn Bloodpine")) {
            if (clicked.contains("Open PvP Stats")) player.performCommand("pvpstats");
            else if (clicked.contains("Open Economy")) player.performCommand("gamble");
            else if (clicked.contains("Open Contracts")) player.performCommand("contract");
            else if (clicked.contains("Open Bounty Board")) player.performCommand("bountyboard");
            else if (clicked.contains("Open Ascension")) player.performCommand("ascend");
            else if (clicked.contains("Open Full Guide")) player.performCommand("guide");
            else if (clicked.contains("Open Main Hub")) player.performCommand("hub");
        }

        if (title.contains("PvP Stats")) {
            if (clicked.contains("View Leaderboards")) player.performCommand("pvpstats leaderboard");
            if (clicked.contains("Refresh")) player.performCommand("pvpstats");
        }

        if (title.contains("Events Panel")) {
            if (!player.isOp()) {
                player.sendMessage(colorize("&cOnly OP can control events from this panel."));
                return;
            }
            if (clicked.contains("Start Blood Moon")) player.performCommand("events start bloodmoon");
            else if (clicked.contains("Start Arena")) player.performCommand("events start arena");
            else if (clicked.contains("Start Hunt Marked")) player.performCommand("events start hunt");
            else if (clicked.contains("Spawn Boss")) player.performCommand("events start boss");
            else if (clicked.contains("Start Final Hour")) player.performCommand("events start finalhour");
            else if (clicked.contains("Force Supply Drop")) player.performCommand("events start supplydrop");
            else if (clicked.contains("Bosses Removed")) player.sendMessage(colorize("&aOnly Blood Tyrant is enabled."));
            else if (clicked.contains("Stop Arena")) player.performCommand("events stop arena");
            else if (clicked.contains("Stop Final Hour")) player.performCommand("events stop finalhour");
            else if (clicked.contains("Stop All Events")) player.performCommand("events stop all");
            else if (clicked.contains("Status")) player.performCommand("events");
        }

        if (title.contains("Black Market")) {
            if (clicked.contains("Warp")) player.performCommand("blackmarket warp");
            else if (clicked.contains("Illegal")) player.performCommand("blackmarket illegalbook");
            else if (clicked.contains("Risky")) player.performCommand("blackmarket riskybuff");
            else if (clicked.contains("Combat Perk")) player.performCommand("blackmarket combatperk");
        }

        if (title.contains("Daily Quest")) {
            if (clicked.contains("Claim")) player.performCommand("dailyquest claim");
            else player.performCommand("dailyquest");
        }

        if (title.contains("Contracts")) {
            if (clicked.contains("New Contract")) player.performCommand("contract new");
            else player.performCommand("contract");
        }

        if (title.contains("Bounty Board")) {
            if (clicked.contains("Refresh")) plugin.getExpansionGUIManager().openBountyBoard(player);
        }

        if (title.contains("Ascension")) {
            if (clicked.contains("Ascend")) player.performCommand("ascend");
        }

        if (title.contains("Economy Sinks")) {
            if (clicked.contains("Gamble 10")) player.performCommand("gamble 10");
            else if (clicked.contains("Gamble 25")) player.performCommand("gamble 25");
            else if (clicked.contains("Gamble 50")) player.performCommand("gamble 50");
            else if (clicked.contains("Combat Crate")) player.performCommand("combatcrate open");
            else if (clicked.contains("Blood Forge")) player.performCommand("bloodforge forge");
            else if (clicked.contains("Heart Insurance")) player.performCommand("heartinsurance buy");
        }

        if (title.contains("Parkour Challenge")) {
            if (clicked.contains("Start Timer")) player.performCommand("parkour start");
            else if (clicked.contains("Finish Timer")) player.performCommand("parkour finish");
        }

        if (title.contains("Season Arc")) {
            player.performCommand("season");
        }

        if (title.contains("Combat Replay")) {
            if (clicked.contains("Refresh")) player.performCommand("combatreplay");
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
