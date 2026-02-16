package net.bloodpine.core.commands;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class AdminCommand implements CommandExecutor {
    
    private final BloodpineCore plugin;
    
    public AdminCommand(BloodpineCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bloodpine.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        Player target;
        
        switch (args[0].toLowerCase()) {
            case "give":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine give <player> <amount>"));
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    plugin.getTokenManager().giveTokens(target, amount);
                    sender.sendMessage(colorize("&aGave &e" + amount + " tokens &ato &f" + target.getName()));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid amount!"));
                }
                break;
                
            case "take":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine take <player> <amount>"));
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    plugin.getTokenManager().removeTokens(target, amount);
                    sender.sendMessage(colorize("&aTook &e" + amount + " tokens &afrom &f" + target.getName()));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid amount!"));
                }
                break;
                
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine set <player> <amount>"));
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    plugin.getTokenManager().setTokens(target, amount);
                    sender.sendMessage(colorize("&aSet &f" + target.getName() + "&a's tokens to &e" + amount));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid amount!"));
                }
                break;
                
            case "reload":
                plugin.reloadConfig();
                plugin.getDataManager().loadData();
                sender.sendMessage(colorize("&aConfiguration reloaded!"));
                break;
                
            case "save":
                plugin.getDataManager().saveData();
                sender.sendMessage(colorize("&aData saved!"));
                break;
                
            case "reset":
                if (args.length < 2) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine reset <player|all>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    plugin.getDataManager().resetAllData();
                    sender.sendMessage(colorize("&aReset all player data!"));
                } else {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(colorize("&cPlayer not found!"));
                        return true;
                    }
                    plugin.getDataManager().resetPlayerData(target.getUniqueId());
                    sender.sendMessage(colorize("&aReset data for &f" + target.getName()));
                }
                break;

            case "wipe":
                if (args.length < 2) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine wipe <player>"));
                    return true;
                }
                resetEverythingForPlayer(sender, args[1]);
                break;

            // ========== STORE COMMANDS ==========

            case "boost":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine boost <player> <minutes>"));
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return true;
                }
                try {
                    int minutes = Integer.parseInt(args[2]);
                    plugin.getBoostManager().giveTokenBoost(target, minutes);
                    sender.sendMessage(colorize("&aGave &e2x Token Boost &afor &e" + minutes + "m &ato &f" + target.getName()));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid minutes!"));
                }
                break;

            case "shield":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine shield <player> <uses>"));
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return true;
                }
                try {
                    int uses = Integer.parseInt(args[2]);
                    plugin.getBoostManager().giveHeartShield(target, uses);
                    sender.sendMessage(colorize("&aGave &e" + uses + " Heart Shield(s) &ato &f" + target.getName()));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid uses!"));
                }
                break;

            case "givehearts":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine givehearts <player> <amount>"));
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return true;
                }
                try {
                    int hearts = Integer.parseInt(args[2]);
                    plugin.getBoostManager().giveExtraHearts(target, hearts);
                    sender.sendMessage(colorize("&aGave &c" + hearts + " heart(s) &ato &f" + target.getName()));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid amount!"));
                }
                break;

            case "kit":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine kit <player> <warrior|tank|god>"));
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return true;
                }
                String kitName = args[2].toLowerCase();
                if (!kitName.equals("warrior") && !kitName.equals("tank") && !kitName.equals("god")) {
                    sender.sendMessage(colorize("&cInvalid kit! Use: warrior, tank, or god"));
                    return true;
                }
                plugin.getBoostManager().giveKit(target, kitName);
                sender.sendMessage(colorize("&aGave &e" + kitName + " kit &ato &f" + target.getName()));
                break;

            case "killeffect":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine killeffect <player> <minutes>"));
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return true;
                }
                try {
                    int minutes = Integer.parseInt(args[2]);
                    plugin.getBoostManager().giveKillEffect(target, minutes);
                    sender.sendMessage(colorize("&aGave &eKill Effect &afor &e" + minutes + "m &ato &f" + target.getName()));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid minutes!"));
                }
                break;

            case "revive":
                if (args.length < 2) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine revive <player>"));
                    return true;
                }
                plugin.getBoostManager().revivePlayer(args[1]);
                sender.sendMessage(colorize("&aRevived &f" + args[1] + "&a! They can rejoin now."));
                break;

            case "rebirthgrant":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine rebirthgrant <player> <levels>"));
                    return true;
                }
                try {
                    int levels = Integer.parseInt(args[2]);
                    OfflinePlayer grantTarget = Bukkit.getOfflinePlayer(args[1]);
                    if (!grantTarget.isOnline() && !grantTarget.hasPlayedBefore()) {
                        sender.sendMessage(colorize("&cPlayer not found!"));
                        return true;
                    }
                    plugin.getDataManager().getPlayerData(grantTarget.getUniqueId()).addRebirthLevels(levels);
                    plugin.getDataManager().getPlayerData(grantTarget.getUniqueId()).addRebirthPoints(levels * plugin.getRebirthManager().getPointsPerRebirth());
                    Player onlineGrantTarget = Bukkit.getPlayer(grantTarget.getUniqueId());
                    if (onlineGrantTarget != null) {
                        plugin.getStatManager().applyStats(onlineGrantTarget);
                        plugin.getDisplayManager().updateDisplay(onlineGrantTarget);
                    }
                    plugin.getDataManager().saveData();
                    sender.sendMessage(colorize("&aGranted &e" + levels + " rebirth level(s) &ato &f" + (grantTarget.getName() != null ? grantTarget.getName() : args[1])));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid levels!"));
                }
                break;

            case "rebirthpoint":
                if (args.length < 3) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine rebirthpoint <player> <points>"));
                    return true;
                }
                try {
                    int points = Integer.parseInt(args[2]);
                    OfflinePlayer pointsTarget = Bukkit.getOfflinePlayer(args[1]);
                    if (!pointsTarget.isOnline() && !pointsTarget.hasPlayedBefore()) {
                        sender.sendMessage(colorize("&cPlayer not found!"));
                        return true;
                    }
                    plugin.getDataManager().getPlayerData(pointsTarget.getUniqueId()).addRebirthPoints(points);
                    Player onlinePointsTarget = Bukkit.getPlayer(pointsTarget.getUniqueId());
                    if (onlinePointsTarget != null) {
                        plugin.getDisplayManager().updateDisplay(onlinePointsTarget);
                    }
                    plugin.getDataManager().saveData();
                    sender.sendMessage(colorize("&aGranted &e" + points + " rebirth point(s) &ato &f" + (pointsTarget.getName() != null ? pointsTarget.getName() : args[1])));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid points!"));
                }
                break;

            case "rebirthreset":
                if (args.length < 2) {
                    sender.sendMessage(colorize("&cUsage: /bloodpine rebirthreset <player>"));
                    return true;
                }
                OfflinePlayer resetTarget = Bukkit.getOfflinePlayer(args[1]);
                if (!resetTarget.isOnline() && !resetTarget.hasPlayedBefore()) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return true;
                }
                plugin.getDataManager().getPlayerData(resetTarget.getUniqueId()).setRebirthLevel(0);
                plugin.getDataManager().getPlayerData(resetTarget.getUniqueId()).setRebirthPoints(0);
                Player onlineResetTarget = Bukkit.getPlayer(resetTarget.getUniqueId());
                if (onlineResetTarget != null) {
                    plugin.getStatManager().applyStats(onlineResetTarget);
                    plugin.getDisplayManager().updateDisplay(onlineResetTarget);
                }
                plugin.getDataManager().saveData();
                sender.sendMessage(colorize("&aReset rebirth progression for &f" + (resetTarget.getName() != null ? resetTarget.getName() : args[1])));
                break;
                
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&c&lBloodpine Admin Commands"));
        sender.sendMessage(colorize("&e/bloodpine give <player> <amount> &7- Give tokens"));
        sender.sendMessage(colorize("&e/bloodpine take <player> <amount> &7- Take tokens"));
        sender.sendMessage(colorize("&e/bloodpine set <player> <amount> &7- Set tokens"));
        sender.sendMessage(colorize("&e/bloodpine reload &7- Reload config"));
        sender.sendMessage(colorize("&e/bloodpine save &7- Save data"));
        sender.sendMessage(colorize("&e/bloodpine reset <player|all> &7- Reset data"));
        sender.sendMessage(colorize("&e/bloodpine wipe <player> &7- Reset EVERYTHING for player"));
        sender.sendMessage(colorize("&6&lStore Commands:"));
        sender.sendMessage(colorize("&e/bloodpine boost <player> <minutes> &7- 2x token boost"));
        sender.sendMessage(colorize("&e/bloodpine shield <player> <uses> &7- Heart shield"));
        sender.sendMessage(colorize("&e/bloodpine givehearts <player> <amount> &7- Extra hearts"));
        sender.sendMessage(colorize("&e/bloodpine kit <player> <name> &7- Give kit"));
        sender.sendMessage(colorize("&e/bloodpine killeffect <player> <minutes> &7- Kill effects"));
        sender.sendMessage(colorize("&e/bloodpine revive <player> &7- Revive eliminated player"));
        sender.sendMessage(colorize("&e/bloodpine rebirthgrant <player> <levels> &7- Grant rebirth levels"));
        sender.sendMessage(colorize("&e/bloodpine rebirthpoint <player> <points> &7- Grant rebirth points"));
        sender.sendMessage(colorize("&e/bloodpine rebirthreset <player> &7- Reset rebirth progression"));
    }
    
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void resetEverythingForPlayer(CommandSender sender, String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(colorize("&cPlayer not found!"));
            return;
        }

        UUID uuid = offlinePlayer.getUniqueId();

        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(playerName);
        if (offlinePlayer.getName() != null) {
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(offlinePlayer.getName());
        }

        plugin.getDataManager().resetPlayerData(uuid);
        plugin.getBoostManager().clearPlayerEffects(uuid);
        plugin.getKillstreakManager().clearPlayerState(uuid);
        plugin.getBountyManager().clearBounty(uuid);
        plugin.getCombatLogListener().clearPlayerState(uuid);

        deleteVanillaPlayerFiles(uuid);
        deleteEssentialsUserFile(uuid);

        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            onlinePlayer.getInventory().clear();
            onlinePlayer.getEnderChest().clear();
            onlinePlayer.setFireTicks(0);
            onlinePlayer.setFoodLevel(20);
            onlinePlayer.setSaturation(20f);
            onlinePlayer.setExhaustion(0f);
            onlinePlayer.setLevel(0);
            onlinePlayer.setExp(0f);
            onlinePlayer.setTotalExperience(0);
            onlinePlayer.setFallDistance(0f);
            onlinePlayer.setHealth(onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            onlinePlayer.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            onlinePlayer.getActivePotionEffects().forEach(effect -> onlinePlayer.removePotionEffect(effect.getType()));
            onlinePlayer.removePotionEffect(org.bukkit.potion.PotionEffectType.GLOWING);
            plugin.getStatManager().applyStats(onlinePlayer);
            plugin.getDisplayManager().updateDisplay(onlinePlayer);
            onlinePlayer.sendMessage(colorize("&c&lYour Bloodpine data has been fully reset by an admin."));
        }

        String displayName = offlinePlayer.getName() != null ? offlinePlayer.getName() : playerName;
        sender.sendMessage(colorize("&aFULL WIPE complete for &f" + displayName + "&a (plugin + vanilla data)."));
    }

    private void deleteVanillaPlayerFiles(UUID uuid) {
        String uuidName = uuid.toString();
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            File worldFolder = world.getWorldFolder();
            deleteIfExists(new File(new File(worldFolder, "playerdata"), uuidName + ".dat"));
            deleteIfExists(new File(new File(worldFolder, "stats"), uuidName + ".json"));
            deleteIfExists(new File(new File(worldFolder, "advancements"), uuidName + ".json"));
        }
    }

    private void deleteEssentialsUserFile(UUID uuid) {
        File essentialsFolder = new File(Bukkit.getWorldContainer(), "plugins/Essentials/userdata");
        deleteIfExists(new File(essentialsFolder, uuid.toString() + ".yml"));
    }

    private void deleteIfExists(File file) {
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete wipe target file: " + file.getAbsolutePath());
        }
    }
}
