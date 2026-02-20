package net.bloodpine.core.security;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class OpLockManager {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final BloodpineCore plugin;
    private File opLockFile;
    private YamlConfiguration opLockConfig;
    private BukkitTask enforceTask;

    private boolean enabled;
    private boolean blockOpCommands;
    private long enforceIntervalTicks;
    private boolean logEnforcement;
    private Set<UUID> allowedUuids = Collections.emptySet();

    public OpLockManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    public void init() {
        opLockFile = new File(plugin.getDataFolder(), "op-lock.yml");
        ensureDefaultFile();
        reload();
        start();
        enforceNow();
    }

    public void reload() {
        opLockConfig = YamlConfiguration.loadConfiguration(opLockFile);

        enabled = opLockConfig.getBoolean("enabled", true);
        blockOpCommands = opLockConfig.getBoolean("block-op-commands", true);

        long intervalSeconds = Math.max(1L, opLockConfig.getLong("enforce-interval-seconds", 10L));
        enforceIntervalTicks = intervalSeconds * 20L;
        logEnforcement = opLockConfig.getBoolean("log-enforcement", true);

        allowedUuids = parseAllowlist(opLockConfig.getStringList("allowlist"));
    }

    public void start() {
        stop();
        if (!enabled) {
            return;
        }

        enforceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::enforceNow, 40L, enforceIntervalTicks);
    }

    public void stop() {
        if (enforceTask != null) {
            enforceTask.cancel();
            enforceTask = null;
        }
    }

    public void enforceNow() {
        if (!enabled) {
            return;
        }

        // Remove OP from anyone not explicitly allowlisted
        for (OfflinePlayer operator : Bukkit.getOperators()) {
            UUID uuid = operator.getUniqueId();
            if (!allowedUuids.contains(uuid)) {
                if (operator.isOp()) {
                    operator.setOp(false);
                    if (logEnforcement) {
                        plugin.getLogger().warning("[OP-LOCK] De-opped " + safeName(operator) + " (not in allowlist)");
                    }
                }
            }
        }

        // Ensure allowlisted entries are OP
        for (UUID uuid : allowedUuids) {
            OfflinePlayer allowed = Bukkit.getOfflinePlayer(uuid);
            if (!allowed.isOp()) {
                allowed.setOp(true);
                if (logEnforcement) {
                    plugin.getLogger().info("[OP-LOCK] Opped " + safeName(allowed) + " (allowlist)");
                }
            }
        }
    }

    public boolean shouldBlockOpCommands() {
        return enabled && blockOpCommands;
    }

    public boolean isOpOrDeopCommand(String rawCommandOrMessage) {
        if (rawCommandOrMessage == null) {
            return false;
        }

        String trimmed = rawCommandOrMessage.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isEmpty()) {
            return false;
        }

        String[] parts = trimmed.split("\\s+", 2);
        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.lastIndexOf(':');
        if (colon >= 0 && colon < label.length() - 1) {
            label = label.substring(colon + 1);
        }
        return label.equals("op") || label.equals("deop");
    }

    public String getBlockedMessage() {
        return "§cOP is locked. Edit plugins/BloodpineCore/op-lock.yml (allowlist) and restart/reload.";
    }

    private void ensureDefaultFile() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        if (opLockFile.exists()) {
            return;
        }

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", true);
        cfg.set("block-op-commands", true);
        cfg.set("enforce-interval-seconds", 10);
        cfg.set("log-enforcement", true);

        // Bootstrap allowlist: current ops if any, otherwise config owner name
        Set<String> bootstrap = new HashSet<>();
        for (OfflinePlayer operator : Bukkit.getOperators()) {
            String name = operator.getName();
            if (name != null && !name.isBlank()) {
                bootstrap.add(name);
            } else {
                bootstrap.add(operator.getUniqueId().toString());
            }
        }

        if (bootstrap.isEmpty()) {
            String owner = plugin.getConfig().getString("end-control.owner", "");
            if (owner != null && !owner.isBlank()) {
                bootstrap.add(owner);
            }
        }

        cfg.set("allowlist", bootstrap.stream().toList());

        try {
            cfg.save(opLockFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create op-lock.yml: " + e.getMessage());
        }
    }

    private Set<UUID> parseAllowlist(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptySet();
        }

        Set<UUID> out = new HashSet<>();
        for (String raw : entries) {
            if (raw == null) {
                continue;
            }
            String value = raw.trim();
            if (value.isEmpty()) {
                continue;
            }

            if (UUID_PATTERN.matcher(value).matches()) {
                try {
                    out.add(UUID.fromString(value));
                } catch (IllegalArgumentException ignored) {
                    // ignore invalid
                }
                continue;
            }

            // Treat as player name (works in offline-mode too)
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(value);
            out.add(offlinePlayer.getUniqueId());
        }
        return out;
    }

    private String safeName(OfflinePlayer player) {
        String name = player.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return player.getUniqueId().toString();
    }
}
