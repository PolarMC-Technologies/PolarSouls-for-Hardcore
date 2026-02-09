package com.mario.polarsouls.model;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String username;
    private int lives;
    private boolean isDead;
    private long firstJoin;    // epoch millis
    private long lastDeath;    // epoch millis, 0 = never died
    private long playTimeMillis; // accumulated play time in millis
    private long lastLogin;      // epoch millis when player last logged in, 0 = offline

    public PlayerData(UUID uuid, String username, int lives, boolean isDead,
                      long firstJoin, long lastDeath, long playTimeMillis, long lastLogin) {
        this.uuid = uuid;
        this.username = username;
        this.lives = lives;
        this.isDead = isDead;
        this.firstJoin = firstJoin;
        this.lastDeath = lastDeath;
        this.playTimeMillis = playTimeMillis;
        this.lastLogin = lastLogin;
    }

    public static PlayerData createNew(UUID uuid, String username, int defaultLives) {
        long now = System.currentTimeMillis();
        return new PlayerData(uuid, username, defaultLives, false,
                now, 0L, 0L, now);
    }

    public boolean isInGracePeriod(int gracePeriodHours) {
        if (gracePeriodHours <= 0) return false;
        long gracePeriodMillis = gracePeriodHours * 3600_000L;
        long effectivePlay = playTimeMillis;
        if (lastLogin > 0) {
            effectivePlay += System.currentTimeMillis() - lastLogin;
        }
        return effectivePlay < gracePeriodMillis;
    }

    public String getGraceTimeRemaining(int gracePeriodHours) {
        long gracePeriodMillis = gracePeriodHours * 3600_000L;
        long effectivePlay = playTimeMillis;
        if (lastLogin > 0) {
            effectivePlay += System.currentTimeMillis() - lastLogin;
        }
        long remaining = gracePeriodMillis - effectivePlay;

        if (remaining <= 0) return "0m";

        long hours = remaining / 3600_000L;
        long minutes = (remaining % 3600_000L) / 60_000L;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public int decrementLife() {
        if (lives > 0) {
            lives--;
        }
        if (lives <= 0) {
            lives = 0;
            isDead = true;
            lastDeath = System.currentTimeMillis();
        }
        return lives;
    }

    public void revive(int livesToRestore) {
        this.lives = livesToRestore;
        this.isDead = false;
    }

    public long getPlayTimeMillis() {
        return playTimeMillis;
    }

    public void setPlayTimeMillis(long playTimeMillis) {
        this.playTimeMillis = playTimeMillis;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public boolean isDead() {
        return isDead;
    }

    public void setDead(boolean dead) {
        isDead = dead;
    }

    public long getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }

    public long getLastDeath() {
        return lastDeath;
    }

    public void setLastDeath(long lastDeath) {
        this.lastDeath = lastDeath;
    }

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid +
                ", name=" + username +
                ", lives=" + lives +
                ", dead=" + isDead + "}";
    }
}
