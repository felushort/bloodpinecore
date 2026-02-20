package net.bloodpine.core.managers;

import net.bloodpine.core.BloodpineCore;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AntiVpnManager {

    private static final Pattern STRING_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern BOOLEAN_FIELD = Pattern.compile("\"%s\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"?(\\d+)\"?");

    private final BloodpineCore plugin;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public AntiVpnManager(BloodpineCore plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("security.anti-vpn.enabled", true);
    }

    public boolean isBypassed(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return false;
        }
        for (String exempt : plugin.getConfig().getStringList("security.anti-vpn.bypass-names")) {
            if (exempt != null && exempt.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public String getKickMessage() {
        return plugin.getConfig().getString("security.anti-vpn.kick-message",
                "&cVPN/Proxy/Hosting connections are blocked on this server.");
    }

    public CheckResult checkIp(String ipAddress) {
        if (!isEnabled()) {
            return CheckResult.allow("anti-vpn disabled", "disabled");
        }

        String ip = normalizeIp(ipAddress);
        if (ip.isBlank()) {
            return CheckResult.allow("unknown ip", "none");
        }

        long now = System.currentTimeMillis();
        CachedResult cached = cache.get(ip);
        if (cached != null && cached.expiresAtMillis > now) {
            return cached.result;
        }

        String provider = plugin.getConfig().getString("security.anti-vpn.provider", "proxycheck");
        CheckResult result = queryProvider(provider, ip);

        String fallback = plugin.getConfig().getString("security.anti-vpn.fallback-provider", "ip-api");
        if (result.error && fallback != null && !fallback.isBlank() && !fallback.equalsIgnoreCase(provider)) {
            CheckResult fallbackResult = queryProvider(fallback, ip);
            if (!fallbackResult.error) {
                result = fallbackResult;
            }
        }

        if (result.error) {
            boolean failOpen = plugin.getConfig().getBoolean("security.anti-vpn.fail-open", true);
            result = failOpen
                    ? CheckResult.allow("lookup failed (fail-open): " + result.reason, "lookup-error")
                    : CheckResult.block("lookup failed (fail-closed): " + result.reason, "lookup-error");
        }

        int ttlMinutes = Math.max(1, plugin.getConfig().getInt("security.anti-vpn.cache-minutes", 180));
        cache.put(ip, new CachedResult(result, now + ttlMinutes * 60_000L));
        return result;
    }

    private CheckResult queryProvider(String providerRaw, String ip) {
        String provider = providerRaw == null ? "" : providerRaw.trim().toLowerCase();
        if (provider.isBlank()) {
            provider = "proxycheck";
        }

        return switch (provider) {
            case "ip-api", "ipapi" -> queryIpApi(ip);
            case "proxycheck", "proxycheck.io" -> queryProxyCheck(ip);
            default -> CheckResult.error("unknown provider: " + provider, provider);
        };
    }

    private CheckResult queryIpApi(String ip) {
        String encodedIp = URLEncoder.encode(ip, StandardCharsets.UTF_8);
        String url = "http://ip-api.com/json/" + encodedIp + "?fields=status,message,proxy,hosting,mobile,query";

        HttpResponse<String> response = httpGet(url);
        if (response == null) {
            return CheckResult.error("ip-api no response", "ip-api");
        }
        if (response.statusCode() != 200) {
            return CheckResult.error("ip-api http " + response.statusCode(), "ip-api");
        }

        String body = response.body() == null ? "" : response.body();
        String status = extractString(body, "status");
        if (!"success".equalsIgnoreCase(status)) {
            String message = extractString(body, "message");
            return CheckResult.error("ip-api status=" + status + " msg=" + message, "ip-api");
        }

        boolean proxy = extractBoolean(body, "proxy");
        boolean hosting = extractBoolean(body, "hosting");
        boolean mobile = extractBoolean(body, "mobile");

        boolean blockHosting = plugin.getConfig().getBoolean("security.anti-vpn.ip-api.block-hosting", true);
        boolean blockMobile = plugin.getConfig().getBoolean("security.anti-vpn.ip-api.block-mobile", false);

        boolean blocked = proxy || (blockHosting && hosting) || (blockMobile && mobile);
        String reason = "ip-api proxy=" + proxy + ", hosting=" + hosting + ", mobile=" + mobile;
        return blocked ? CheckResult.block(reason, "ip-api") : CheckResult.allow(reason, "ip-api");
    }

    private CheckResult queryProxyCheck(String ip) {
        String encodedIp = URLEncoder.encode(ip, StandardCharsets.UTF_8);
        String key = plugin.getConfig().getString("security.anti-vpn.proxycheck.api-key", "").trim();
        String url = "https://proxycheck.io/v2/" + encodedIp + "?vpn=1&risk=1";
        if (!key.isBlank()) {
            url += "&key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
        }

        HttpResponse<String> response = httpGet(url);
        if (response == null) {
            return CheckResult.error("proxycheck no response", "proxycheck");
        }
        if (response.statusCode() != 200) {
            return CheckResult.error("proxycheck http " + response.statusCode(), "proxycheck");
        }

        String body = response.body() == null ? "" : response.body();
        String status = extractString(body, "status");
        if (!"ok".equalsIgnoreCase(status)) {
            return CheckResult.error("proxycheck status=" + status, "proxycheck");
        }

        boolean proxy = body.contains("\"proxy\":\"yes\"")
                || body.contains("\"proxy\": \"yes\"")
                || body.contains("\"proxy\":true");
        int risk = extractNumber(body, "risk", 0);
        int minRisk = Math.max(0, plugin.getConfig().getInt("security.anti-vpn.proxycheck.min-risk-to-block", 0));
        boolean blocked = proxy && risk >= minRisk;

        String reason = "proxycheck proxy=" + proxy + ", risk=" + risk + ", minRisk=" + minRisk;
        return blocked ? CheckResult.block(reason, "proxycheck") : CheckResult.allow(reason, "proxycheck");
    }

    private HttpResponse<String> httpGet(String url) {
        try {
            int timeoutMs = Math.max(500, plugin.getConfig().getInt("security.anti-vpn.request-timeout-millis", 3500));
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("User-Agent", "BloodpineCore-AntiVPN")
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("security.anti-vpn.debug-log", false)) {
                plugin.getLogger().warning("AntiVPN lookup failed: " + e.getMessage());
            }
            return null;
        }
    }

    private String extractString(String json, String field) {
        Matcher matcher = Pattern.compile(String.format(STRING_FIELD.pattern(), Pattern.quote(field))).matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private boolean extractBoolean(String json, String field) {
        Matcher matcher = Pattern.compile(String.format(BOOLEAN_FIELD.pattern(), Pattern.quote(field)), Pattern.CASE_INSENSITIVE).matcher(json);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }

    private int extractNumber(String json, String field, int fallback) {
        Matcher matcher = Pattern.compile(String.format(NUMBER_FIELD.pattern(), Pattern.quote(field))).matcher(json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private String normalizeIp(String ipAddress) {
        if (ipAddress == null) {
            return "";
        }
        String ip = ipAddress.trim();
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }
        return ip;
    }

    private record CachedResult(CheckResult result, long expiresAtMillis) {}

    public static final class CheckResult {
        private final boolean blocked;
        private final boolean error;
        private final String reason;
        private final String provider;

        private CheckResult(boolean blocked, boolean error, String reason, String provider) {
            this.blocked = blocked;
            this.error = error;
            this.reason = reason;
            this.provider = provider;
        }

        public static CheckResult allow(String reason, String provider) {
            return new CheckResult(false, false, reason, provider);
        }

        public static CheckResult block(String reason, String provider) {
            return new CheckResult(true, false, reason, provider);
        }

        public static CheckResult error(String reason, String provider) {
            return new CheckResult(false, true, reason, provider);
        }

        public boolean isBlocked() {
            return blocked;
        }

        public boolean isError() {
            return error;
        }

        public String getReason() {
            return reason;
        }

        public String getProvider() {
            return provider;
        }
    }
}
