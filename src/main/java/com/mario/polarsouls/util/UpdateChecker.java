package com.mario.polarsouls.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// i know damn well the ascii art is crack and overlaps but i really dont care its the thought that counts am i right
public class UpdateChecker {
    private static final String GITHUB_API = "https://api.github.com/repos/PolarMC-Technologies/PolarSouls-for-Hardcore/releases/latest";
    private static final String GITHUB_RELEASES_PAGE = "https://github.com/PolarMC-Technologies/PolarSouls-for-Hardcore/releases";
    private static final String BORDER_EMPTY = "║                                                           ║";
    private static final String BORDER_TOP = "╔═══════════════════════════════════════════════════════════╗";
    private static final String BORDER_BOTTOM = "╚═══════════════════════════════════════════════════════════╝";

    private final Plugin plugin;
    private final String currentVersion;

    public UpdateChecker(Plugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkForUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(GITHUB_API).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "PolarSouls-UpdateChecker");

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().log(Level.WARNING, "Failed to check for updates. HTTP response code: {0}", responseCode);
                    return;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                String latestVersion = json.get("tag_name").getAsString();

                if (latestVersion.startsWith("v")) {
                    latestVersion = latestVersion.substring(1);
                }

                if (isNewerVersion(latestVersion, currentVersion)) {
                    showUpdateNotification(latestVersion);
                } else {
                    plugin.getLogger().info("You are running the latest version!");
                }

            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates: {0}", e.getMessage());
            }
        });
    }

    private boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");

        int maxLength = Math.max(latestParts.length, currentParts.length);

        for (int i = 0; i < maxLength; i++) {
            int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }

        return false;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showUpdateNotification(String latestVersion) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("");
            plugin.getLogger().info(BORDER_TOP);
            plugin.getLogger().info(BORDER_EMPTY);
            plugin.getLogger().info("║           ⚡ UPDATE AVAILABLE ⚡                          ║");
            plugin.getLogger().info(BORDER_EMPTY);
            plugin.getLogger().log(Level.INFO, "║   Current version: {0}║", String.format("%-35s", currentVersion));
            plugin.getLogger().log(Level.INFO, "║   Latest version:  {0}║", String.format("%-35s", latestVersion));
            plugin.getLogger().info(BORDER_EMPTY);
            plugin.getLogger().log(Level.INFO, "║   Download: {0}║", String.format("%-43s", GITHUB_RELEASES_PAGE));
            plugin.getLogger().info(BORDER_EMPTY);
            plugin.getLogger().info(BORDER_BOTTOM);
            plugin.getLogger().info("");
        });
    }
}
