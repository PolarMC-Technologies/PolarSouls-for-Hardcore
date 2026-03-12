package com.mario.polarsouls.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing time duration strings.
 * Supports formats like "2h", "30m", "1h30m", "90m", etc.
 */
public final class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([hms])");

    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Parse a time string into milliseconds.
     * Supports formats: "2h", "30m", "1h30m", "90m", "45s", "1h30m45s"
     * Also supports plain integer values (interpreted as hours for backward compatibility).
     *
     * @param timeStr The time string to parse
     * @return The time in milliseconds, or -1 if parsing failed
     */
    public static long parseTimeToMillis(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return -1;
        }

        timeStr = timeStr.trim().toLowerCase();

        // Try to parse as plain integer (backward compatibility - treat as hours)
        try {
            int hours = Integer.parseInt(timeStr);
            return hours * 3600_000L;
        } catch (NumberFormatException e) {
            // Not a plain integer, continue with pattern matching
        }

        // Parse time components (hours, minutes, seconds)
        Matcher matcher = TIME_PATTERN.matcher(timeStr);
        long totalMillis = 0;
        boolean foundAny = false;

        while (matcher.find()) {
            foundAny = true;
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            totalMillis += switch (unit) {
                case "h" -> value * 3600_000L;
                case "m" -> value * 60_000L;
                case "s" -> value * 1000L;
                default -> 0L;
            };
        }

        return foundAny ? totalMillis : -1;
    }

    /**
     * Format milliseconds into a human-readable time string.
     * Note: Seconds are only shown if hours is 0, to keep common durations concise.
     *
     * @param millis The time in milliseconds
     * @return A formatted string like "2h 30m" or "45m" or "30s", or "0s" for non-positive values
     */
    public static String formatTime(long millis) {
        if (millis < 0) return "0s";  // Treat negative as 0
        if (millis == 0) return "0s";

        long hours = millis / 3600_000L;
        long minutes = (millis % 3600_000L) / 60_000L;
        long seconds = (millis % 60_000L) / 1000L;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        if (minutes > 0) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(minutes).append("m");
        }
        if (seconds > 0 && hours == 0) {  // Only show seconds if no hours, to keep display concise
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(seconds).append("s");
        }

        return sb.isEmpty() ? "0s" : sb.toString();
    }
}
