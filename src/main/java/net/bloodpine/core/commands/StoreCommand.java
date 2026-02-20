package net.bloodpine.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StoreCommand implements CommandExecutor {

    private static final String STORE_URL = "https://store.bloodpine.net";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Store: " + STORE_URL);
            return true;
        }

        player.sendMessage(Component.text("Bloodpine Store: ", NamedTextColor.GOLD)
                .append(Component.text(STORE_URL, NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(STORE_URL))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open store", NamedTextColor.GRAY)))));
        return true;
    }
}
