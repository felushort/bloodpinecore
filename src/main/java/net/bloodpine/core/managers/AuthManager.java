package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AuthManager {

    private final BloodpineCore plugin;
    private final Set<UUID> authenticated = new HashSet<>();
    private File authFile;
    private FileConfiguration authConfig;

    public AuthManager(BloodpineCore plugin) {
        this.plugin = plugin;
        setupFile();
    }

    private void setupFile() {
        authFile = new File(plugin.getDataFolder(), "auth-users.yml");
        if (!authFile.exists()) {
            try {
                authFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create auth-users.yml");
                e.printStackTrace();
            }
        }
        authConfig = YamlConfiguration.loadConfiguration(authFile);
    }

    public synchronized void save() {
        try {
            authConfig.save(authFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save auth-users.yml");
            e.printStackTrace();
        }
    }

    public boolean isAuthRequired() {
        return !plugin.getServer().getOnlineMode();
    }

    public synchronized boolean isRegistered(String playerName) {
        return authConfig.isConfigurationSection("users." + playerName.toLowerCase());
    }

    public synchronized boolean register(String playerName, String password) {
        return register(playerName, password, null);
    }

    public synchronized boolean register(String playerName, String password, String ipAddress) {
        if (isRegistered(playerName)) {
            return false;
        }

        if (isAuthRequired() && !isExemptName(playerName)) {
            if (!canRegisterFromIp(ipAddress)) {
                return false;
            }
            if (getRemainingRegistrationCooldownSeconds(ipAddress) > 0) {
                return false;
            }
        }

        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        String path = "users." + playerName.toLowerCase();
        authConfig.set(path + ".salt", salt);
        authConfig.set(path + ".hash", hash);
        authConfig.set(path + ".registeredAt", System.currentTimeMillis());

        String normalizedIp = normalizeIp(ipAddress);
        if (!normalizedIp.isBlank()) {
            authConfig.set(path + ".firstIp", normalizedIp);
            authConfig.set(path + ".lastIp", normalizedIp);
            authConfig.set(path + ".lastLoginAt", System.currentTimeMillis());
            touchRegistrationCooldownBucket(normalizedIp);
        }

        save();
        return true;
    }

    public synchronized boolean checkPassword(String playerName, String password) {
        String path = "users." + playerName.toLowerCase();
        ConfigurationSection section = authConfig.getConfigurationSection(path);
        if (section == null) {
            return false;
        }

        String salt = section.getString("salt", "");
        String storedHash = section.getString("hash", "");
        if (salt.isBlank() || storedHash.isBlank()) {
            return false;
        }

        String computedHash = hashPassword(password, salt);
        return storedHash.equals(computedHash);
    }

    public void markAuthenticated(Player player) {
        authenticated.add(player.getUniqueId());
    }

    public void clearAuthentication(Player player) {
        authenticated.remove(player.getUniqueId());
    }

    public boolean isAuthenticated(Player player) {
        return authenticated.contains(player.getUniqueId());
    }

    public boolean isAltProtectionEnabled() {
        return plugin.getConfig().getBoolean("security.cracked-auth.enabled", true);
    }

    public boolean isExemptName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return false;
        }
        List<String> exempt = plugin.getConfig().getStringList("security.cracked-auth.exempt-names");
        for (String entry : exempt) {
            if (entry != null && entry.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public int getMaxAccountsPerIp() {
        return Math.max(0, plugin.getConfig().getInt("security.cracked-auth.max-accounts-per-ip", 2));
    }

    public synchronized int countRegisteredAccountsForIp(String ipAddress) {
        String incomingIp = normalizeIp(ipAddress);
        if (incomingIp.isBlank()) {
            return 0;
        }

        ConfigurationSection users = authConfig.getConfigurationSection("users");
        if (users == null) {
            return 0;
        }

        int count = 0;
        for (String key : users.getKeys(false)) {
            ConfigurationSection section = users.getConfigurationSection(key);
            if (section == null) continue;
            String firstIp = normalizeIp(section.getString("firstIp", ""));
            if (firstIp.isBlank()) continue;
            if (ipMatchesByMode(firstIp, incomingIp, plugin.getConfig().getString("security.cracked-auth.account-limit-match", "subnet"))) {
                count++;
            }
        }
        return count;
    }

    public synchronized boolean canRegisterFromIp(String ipAddress) {
        if (!isAuthRequired() || !isAltProtectionEnabled()) {
            return true;
        }

        int maxAccounts = getMaxAccountsPerIp();
        if (maxAccounts <= 0) {
            return true;
        }

        return countRegisteredAccountsForIp(ipAddress) < maxAccounts;
    }

    public synchronized long getRemainingRegistrationCooldownSeconds(String ipAddress) {
        if (!isAuthRequired() || !isAltProtectionEnabled()) {
            return 0L;
        }

        int cooldownSeconds = Math.max(0, plugin.getConfig().getInt("security.cracked-auth.registration-cooldown-seconds", 120));
        if (cooldownSeconds <= 0) {
            return 0L;
        }

        String normalizedIp = normalizeIp(ipAddress);
        if (normalizedIp.isBlank()) {
            return 0L;
        }

        String bucket = getCooldownBucketKey(normalizedIp);
        long lastRegistration = authConfig.getLong("meta.registrationBuckets." + bucket + ".lastRegistrationAt", 0L);
        long remainingMillis = (cooldownSeconds * 1000L) - (System.currentTimeMillis() - lastRegistration);
        if (remainingMillis <= 0L) {
            return 0L;
        }
        return (remainingMillis + 999L) / 1000L;
    }

    public synchronized boolean isIpAllowedForAccount(String playerName, String ipAddress) {
        if (!isAuthRequired() || !isAltProtectionEnabled() || isExemptName(playerName)) {
            return true;
        }

        if (!plugin.getConfig().getBoolean("security.cracked-auth.ip-lock-enabled", true)) {
            return true;
        }

        String normalizedIp = normalizeIp(ipAddress);
        if (normalizedIp.isBlank()) {
            return true;
        }

        ConfigurationSection section = authConfig.getConfigurationSection("users." + playerName.toLowerCase());
        if (section == null) {
            return true;
        }

        String firstIp = normalizeIp(section.getString("firstIp", ""));
        if (firstIp.isBlank()) {
            return true;
        }

        String mode = plugin.getConfig().getString("security.cracked-auth.ip-lock-match", "subnet");
        return ipMatchesByMode(firstIp, normalizedIp, mode);
    }

    public synchronized void recordSuccessfulLogin(String playerName, String ipAddress) {
        String path = "users." + playerName.toLowerCase();
        ConfigurationSection section = authConfig.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        String normalizedIp = normalizeIp(ipAddress);
        if (!normalizedIp.isBlank()) {
            if (normalizeIp(section.getString("firstIp", "")).isBlank()) {
                authConfig.set(path + ".firstIp", normalizedIp);
            }
            authConfig.set(path + ".lastIp", normalizedIp);
        }
        authConfig.set(path + ".lastLoginAt", System.currentTimeMillis());
        save();
    }

    private void touchRegistrationCooldownBucket(String normalizedIp) {
        String bucket = getCooldownBucketKey(normalizedIp);
        authConfig.set("meta.registrationBuckets." + bucket + ".lastRegistrationAt", System.currentTimeMillis());
    }

    private String getCooldownBucketKey(String normalizedIp) {
        String mode = plugin.getConfig().getString("security.cracked-auth.account-limit-match", "subnet");
        if ("exact".equalsIgnoreCase(mode)) {
            return "ip_" + sanitizePathKey(normalizedIp);
        }

        int ipv4Bits = Math.max(0, Math.min(32, plugin.getConfig().getInt("security.cracked-auth.ipv4-subnet-bits", 24)));
        int ipv6Bits = Math.max(0, Math.min(128, plugin.getConfig().getInt("security.cracked-auth.ipv6-subnet-bits", 64)));
        return "net_" + subnetBucket(normalizedIp, ipv4Bits, ipv6Bits);
    }

    private String subnetBucket(String ipAddress, int ipv4Bits, int ipv6Bits) {
        try {
            byte[] address = InetAddress.getByName(ipAddress).getAddress();
            int prefixBits = address.length == 4 ? ipv4Bits : ipv6Bits;
            prefixBits = Math.max(0, Math.min(address.length * 8, prefixBits));

            byte[] masked = Arrays.copyOf(address, address.length);
            int fullBytes = prefixBits / 8;
            int remainingBits = prefixBits % 8;

            if (fullBytes < masked.length) {
                if (remainingBits > 0) {
                    int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                    masked[fullBytes] = (byte) (masked[fullBytes] & mask);
                    fullBytes++;
                }
                for (int i = fullBytes; i < masked.length; i++) {
                    masked[i] = 0;
                }
            }

            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(masked);
            return encoded + "_" + prefixBits + "_" + masked.length;
        } catch (Exception ignored) {
            return sanitizePathKey(ipAddress);
        }
    }

    private boolean ipMatchesByMode(String storedIp, String incomingIp, String mode) {
        if (storedIp.isBlank() || incomingIp.isBlank()) {
            return false;
        }
        if ("exact".equalsIgnoreCase(mode)) {
            return storedIp.equalsIgnoreCase(incomingIp);
        }

        int ipv4Bits = Math.max(0, Math.min(32, plugin.getConfig().getInt("security.cracked-auth.ipv4-subnet-bits", 24)));
        int ipv6Bits = Math.max(0, Math.min(128, plugin.getConfig().getInt("security.cracked-auth.ipv6-subnet-bits", 64)));
        return sameSubnet(storedIp, incomingIp, ipv4Bits, ipv6Bits);
    }

    private boolean sameSubnet(String firstIp, String secondIp, int ipv4Bits, int ipv6Bits) {
        try {
            byte[] a = InetAddress.getByName(firstIp).getAddress();
            byte[] b = InetAddress.getByName(secondIp).getAddress();
            if (a.length != b.length) {
                return false;
            }

            int prefixBits = a.length == 4 ? ipv4Bits : ipv6Bits;
            prefixBits = Math.max(0, Math.min(a.length * 8, prefixBits));
            if (prefixBits == 0) {
                return true;
            }

            int fullBytes = prefixBits / 8;
            int remainingBits = prefixBits % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (a[i] != b[i]) {
                    return false;
                }
            }

            if (remainingBits > 0) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                return (a[fullBytes] & mask) == (b[fullBytes] & mask);
            }
            return true;
        } catch (Exception ignored) {
            return firstIp.equalsIgnoreCase(secondIp);
        }
    }

    private String normalizeIp(String ipAddress) {
        return ipAddress == null ? "" : ipAddress.trim();
    }

    private String sanitizePathKey(String value) {
        return value.replace('.', '_').replace(':', '_');
    }

    private String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
