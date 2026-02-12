package com.mario.polarsouls.model;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String username;
    private int lives;
    private boolean isDead;
    private long firstJoin;    // epoch millis
    private long lastDeath;    // epoch millis, 0 = never died (yet)
    private long lastSeen;     // epoch millis, 0 = currently online
    private long graceUntil;   // epoch millis when grace expires, 0 = no active grace

    public PlayerData(UUID uuid, String username, int lives, boolean isDead,
                      long firstJoin, long lastDeath, long lastSeen, long graceUntil) {
        this.uuid = uuid;
        this.username = username;
        this.lives = lives;
        this.isDead = isDead;
        this.firstJoin = firstJoin;
        this.lastDeath = lastDeath;
        this.lastSeen = lastSeen;
        this.graceUntil = graceUntil;
    }

    public static PlayerData createNew(UUID uuid, String username, int defaultLives) {
        return createNew(uuid, username, defaultLives, 0L);
    }

    public static PlayerData createNew(UUID uuid, String username, int defaultLives,
                                        long gracePeriodMillis) {
        long graceUntil = gracePeriodMillis > 0
                ? System.currentTimeMillis() + gracePeriodMillis : 0L;
        return new PlayerData(uuid, username, defaultLives, false,
                System.currentTimeMillis(), 0L, 0L, graceUntil);
    }

    public boolean isInGracePeriod(long gracePeriodMillis) {
        if (graceUntil > 0) {
            return System.currentTimeMillis() < graceUntil;
        }
        // Legacy fallback: use firstJoin + global config
        if (gracePeriodMillis <= 0) return false;
        long elapsed = System.currentTimeMillis() - firstJoin;
        return elapsed >= 0 && elapsed < gracePeriodMillis;
    }

    public String getGraceTimeRemaining(long gracePeriodMillis) {
        long remaining;
        if (graceUntil > 0) {
            remaining = graceUntil - System.currentTimeMillis();
        } else {
            long elapsed = System.currentTimeMillis() - firstJoin;
            remaining = gracePeriodMillis - elapsed;
        }

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

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getGraceUntil() {
        return graceUntil;
    }

    public void setGraceUntil(long graceUntil) {
        this.graceUntil = graceUntil;
    }

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid +
                ", name=" + username +
                ", lives=" + lives +
                ", dead=" + isDead + "}";
    }
}
