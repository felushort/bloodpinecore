package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class SessionLocationManager {

    private final BloodpineCore plugin;
    private File file;
    private FileConfiguration config;

    public SessionLocationManager(BloodpineCore plugin) {
        this.plugin = plugin;
        setupFile();
        load();
    }

    private void setupFile() {
        file = new File(plugin.getDataFolder(), "last-locations.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Could not create last-locations.yml");
                exception.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void load() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save last-locations.yml");
            exception.printStackTrace();
        }
    }

    public synchronized void saveLocation(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Location location = player.getLocation();
        if (location.getWorld() == null) {
            return;
        }

        String basePath = "players." + player.getName().toLowerCase(Locale.ROOT);
        config.set(basePath + ".world", location.getWorld().getName());
        config.set(basePath + ".x", location.getX());
        config.set(basePath + ".y", location.getY());
        config.set(basePath + ".z", location.getZ());
        config.set(basePath + ".yaw", location.getYaw());
        config.set(basePath + ".pitch", location.getPitch());
        config.set(basePath + ".updatedAt", System.currentTimeMillis());
    }

    public synchronized void saveOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            saveLocation(player);
        }
        save();
    }

    public synchronized boolean restoreLocation(Player player) {
        if (player == null) {
            return false;
        }

        String basePath = "players." + player.getName().toLowerCase(Locale.ROOT);
        ConfigurationSection section = config.getConfigurationSection(basePath);
        if (section == null) {
            return false;
        }

        String worldName = section.getString("world", "");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }

        double x = section.getDouble("x", world.getSpawnLocation().getX());
        double y = section.getDouble("y", world.getSpawnLocation().getY());
        double z = section.getDouble("z", world.getSpawnLocation().getZ());
        float yaw = (float) section.getDouble("yaw", world.getSpawnLocation().getYaw());
        float pitch = (float) section.getDouble("pitch", world.getSpawnLocation().getPitch());

        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return false;
        }

        Location target = new Location(world, x, y, z, yaw, pitch);
        player.teleport(target);
        return true;
    }
}
