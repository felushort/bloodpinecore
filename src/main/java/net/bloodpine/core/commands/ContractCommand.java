package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import net.bloodpine.core.managers.ExpansionDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ContractCommand implements CommandExecutor {

    private final BloodpineCore plugin;
    private final Random random = new Random();

    public ContractCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            plugin.getExpansionGUIManager().openContract(player);
        }

        ExpansionDataManager.ExpansionProfile profile = plugin.getExpansionDataManager().getProfile(player);

        if (args.length >= 1 && args[0].equalsIgnoreCase("new")) {
            List<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
            candidates.remove(player);
            if (candidates.isEmpty()) {
                player.sendMessage(colorize("&cNo contract targets are online."));
                return true;
            }

            Player target = candidates.get(random.nextInt(candidates.size()));
            profile.setContractTarget(target.getName());
            profile.setContractProgress(0);
            profile.setContractGoal(plugin.getConfig().getInt("spawn.contract-kill-goal", 2));

            player.sendMessage(colorize("&6New contract: Eliminate &f" + target.getName()
                    + " &6" + profile.getContractGoal() + " times."));
            return true;
        }

        if (profile.getContractTarget().isEmpty()) {
            player.sendMessage(colorize("&7No active contract. Use /contract new"));
            return true;
        }

        player.sendMessage(colorize("&6Contract Target: &f" + profile.getContractTarget()));
        player.sendMessage(colorize("&7Progress: &f" + profile.getContractProgress() + "/" + profile.getContractGoal()));
        player.sendMessage(colorize("&7Use /contract new to reroll target."));
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
