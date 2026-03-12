package com.mario.polarsouls.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([hms])");

    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static long parseTimeToMillis(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return -1;
        }

        timeStr = timeStr.trim().toLowerCase();

        try {
            int hours = Integer.parseInt(timeStr);
            return hours * 3600_000L;
        } catch (NumberFormatException e) {
            // not a plain integer
        }

        // parse time components
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


    public static String formatTime(long millis) {
        if (millis < 0) return "0s";  //
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
        if (seconds > 0 && hours == 0) {  
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(seconds).append("s");
        }

        return sb.isEmpty() ? "0s" : sb.toString();
    }
}
