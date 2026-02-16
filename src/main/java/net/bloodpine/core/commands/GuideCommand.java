package net.bloodpine.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class GuideCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cOnly players can use this command."));
            return true;
        }

        ItemStack guideBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) guideBook.getItemMeta();

        meta.setTitle(colorize("&4Bloodpine Guide"));
        meta.setAuthor("Bloodpine SMP");

        List<String> pages = new ArrayList<>();

        pages.add(colorize(
                "&c&lBLOODPINE GUIDE\n\n" +
                "&7Welcome to Bloodpine.\n\n" +
                "&fThis book covers everything you need to know to survive, progress, and avoid punishments.\n\n" +
                "&8Use /guide anytime."
        ));

        pages.add(colorize(
                "&c&lCORE LOOP\n\n" +
                "&7• Kill players to earn tokens\n" +
                "&7• Allocate tokens into stats\n" +
                "&7• Rebirth for permanent scaling\n\n" +
                "&fPvP style: sword/axe/mace focused.\n" +
                "&fCrystal mastery is disabled."
        ));

        pages.add(colorize(
                "&c&lTOKENS & STATS\n\n" +
                "&7Use /allocate <stat> <amount>\n\n" +
                "&fStats:\n" +
                "&7- Damage\n" +
                "&7- Defense\n" +
                "&7- Totem\n" +
                "&7- Vitality\n\n" +
                "&fUse /tokens and /stats to check progress."
        ));

        pages.add(colorize(
                "&c&lLIFESTEAL\n\n" +
                "&fOn PvP death:\n" +
                "&7- Victim loses a heart\n" +
                "&7- Killer gains a heart\n\n" +
                "&fHeart Shields can block heart loss.\n" +
                "&fIf you hit minimum hearts, you can be eliminated until revived."
        ));

        pages.add(colorize(
                "&c&lREBIRTH\n\n" +
                "&fUse /rebirth when you meet token requirement.\n\n" +
                "&7Rebirth gives permanent bonuses and points.\n" +
                "&7Higher rebirth = stronger scaling over time.\n\n" +
                "&fCheck with /rebirth info."
        ));

        pages.add(colorize(
                "&c&lTOKEN SHOP\n\n" +
                "&fUse /tshop for in-game token purchases (boosts, kits, heals, etc).\n\n" +
                "&fWeb store: store.bloodpine.net\n" +
                "&7PayPal checkout + instant delivery to your username.\n\n" +
                "&fDouble-check your exact name before buying."
        ));

        pages.add(colorize(
                "&c&lWORLD / SAFETY\n\n" +
                "&7• Huge spawn safe-zone prevents spawn-killing\n" +
                "&7• The End is locked until staff opens it\n" +
                "&7• Premium accounts only\n\n" +
                "&fIf something looks bugged, report it instead of abusing it."
        ));

        pages.add(colorize(
                "&c&lPLAYER COMMANDS\n\n" +
                "&f/menu &7- Open stat GUI\n" +
                "&f/tokens &7- Token summary\n" +
                "&f/stats [player] &7- Stat info\n" +
                "&f/leaderboard &7- Top players\n" +
                "&f/bounty &7- Bounty system\n" +
                "&f/paytokens &7- Send available tokens\n" +
                "&f/payhearts &7- Send hearts\n" +
                "&f/guide &7- Open this book"
        ));

        pages.add(colorize(
                "&c&lRULES (SHORT)\n\n" +
                "&7• No cheats or exploits\n" +
                "&7• No dupes/macros/scripts\n" +
                "&7• No hate speech/doxxing\n" +
                "&7• No chargeback fraud\n" +
                "&7• Respect staff decisions\n\n" +
                "&fFull rules are posted in Discord."
        ));

        pages.add(colorize(
                "&c&lNEED HELP?\n\n" +
                "&fDiscord: Bloodpine community\n" +
                "&fApply/Admin panel: apply.bloodpine.net\n" +
                "&fStore: store.bloodpine.net\n\n" +
                "&7Good luck.\n" +
                "&cDominate the season."
        ));

        meta.setPages(pages);
        guideBook.setItemMeta(meta);

        player.openBook(guideBook);
        return true;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
