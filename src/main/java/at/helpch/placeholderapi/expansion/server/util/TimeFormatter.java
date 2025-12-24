package at.helpch.placeholderapi.expansion.server.util;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

public final class TimeFormatter {

    private final Map<String, DateTimeFormatter> dateFormatsCache = new HashMap<>();

    private final Locale timeLocale;
    private final ZoneId timeZone;
    private final Map<@NotNull String, @Nullable Object> timeUnitSuffixes;

    public TimeFormatter(
        @NotNull final Locale timeLocale, @NotNull final ZoneId timeZone,
        @NotNull Map<@NotNull String, @Nullable Object> timeUnitSuffixes
    ) {
        this.timeLocale = timeLocale;
        this.timeZone = timeZone;
        this.timeUnitSuffixes = timeUnitSuffixes;
    }

    /**
     * Parse a string to a {@link DateTimeFormatter} and cache it
     *
     * @param pattern pattern to parse
     * @return {@link DateTimeFormatter} if the argument is a valid pattern, otherwise null
     */
    public @Nullable DateTimeFormatter parseFormat(@NotNull final String pattern) {
        return dateFormatsCache.computeIfAbsent(pattern, __ -> {
            try {
                return DateTimeFormatter.ofPattern(pattern, timeLocale);
            } catch (IllegalArgumentException e) {
                Logging.error(e, "Could not parse DateTimeFormatter from pattern \"{0}\"", pattern);
            } catch (DateTimeException e) {
                if (e.getMessage().contains("obtain LocalTime from TemporalAccessor")) {
                    Logging.error(e, "The pattern \"{0}\" might be missing the time definition, try to add 'HH' at the end and set it to '00' (beginning of the day)", pattern);
                } else {
                    Logging.error(e, "Could not parse DateTimeFormatter from pattern \"{0}\"", pattern);
                }
            }

            return null;
        });
    }

    /**
     * Format the current time with the given format
     *
     * @param format format
     * @return {@code null} if the format is empty or invalid, otherwise {@link ZonedDateTime#now(ZoneId)} formatted
     */
    public @Nullable String formatTime(@NotNull final String format) {
        if (format.trim().isEmpty()) {
            return null;
        }

        return Optional.ofNullable(parseFormat(format))
            .map(formatter -> ZonedDateTime.now(timeZone).format(formatter))
            .orElse(null);
    }

    /**
     * Format time as {@code #w #d #h #m #s}
     *
     * @param time time in seconds
     * @return time formatted
     */
    public @NotNull String formatTimeInSeconds(long time) {
        final StringJoiner joiner = new StringJoiner(" ");
        final BiConsumer<@NotNull Long, @Nullable Object> appendTime = (value, unit) -> {
            if (value > 0 && unit != null) {
                joiner.add(value + unit.toString());
            }
        };

        long seconds = time;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        final long weeks = days / 7;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        days %= 7;

        appendTime.accept(weeks, timeUnitSuffixes.get("week"));
        appendTime.accept(days, timeUnitSuffixes.get("day"));
        appendTime.accept(hours, timeUnitSuffixes.get("hour"));
        appendTime.accept(minutes, timeUnitSuffixes.get("minute"));
        appendTime.accept(seconds, timeUnitSuffixes.get("second"));
        return joiner.toString();
    }

    /**
     * Calculate the time between {@link ZonedDateTime#now(ZoneId)} and another date and return a formatted value
     * using {@link #formatTimeInSeconds(long)} if {@code formatTime} is {@code true}.
     *
     * @param player      player
     * @param args        arguments received on {@link me.clip.placeholderapi.expansion.PlaceholderExpansion#onRequest(OfflinePlayer, String)}
     * @param isCountdown whether the time calculated is for a countdown or a count-up
     * @param formatTime  whether the time should be formatted with {@link #formatTimeInSeconds(long)}
     * @return time formatter
     */
    public @NotNull String calculateTimeBetween(
        @Nullable final OfflinePlayer player, @NotNull final String args,
        final boolean isCountdown, final boolean formatTime
    ) {
        DateTimeFormatter formatter;
        String otherDateString;

        // Placeholder with custom time format
        if (args.contains("_")) {
            final String[] parts = PlaceholderAPI.setBracketPlaceholders(player, args).split("_");

            if (parts.length != 2) {
                return "invalid format and time";
            }

            formatter = this.parseFormat(parts[0]);
            otherDateString = parts[1];
        } else {
            formatter = this.parseFormat(PlaceholderAPIPlugin.getInstance().getPlaceholderAPIConfig().dateFormat());
            otherDateString = args;
        }

        if (formatter == null) {
            return "invalid date format";
        }

        DateTimeFormatter zonedFormatter = formatter.withZone(timeZone);

        try {
            ZonedDateTime now = ZonedDateTime.now(timeZone);
            ZonedDateTime otherDate = ZonedDateTime.parse(otherDateString, zonedFormatter);

            if (otherDate.isEqual(now)) {
                return "0";
            }

            // 'otherDate' should be after 'now' for countdowns and before for count-ups
            if ((isCountdown && otherDate.isBefore(now)) || (!isCountdown && otherDate.isAfter(now))) {
                return "0";
            }

            final long time = isCountdown ? now.until(otherDate, ChronoUnit.SECONDS) : otherDate.until(now, ChronoUnit.SECONDS);
            return formatTime ? this.formatTimeInSeconds(time) : String.valueOf(time);
        } catch (DateTimeParseException e) {
            final String type = isCountdown ? "countdown" : "count-up";
            Logging.error(e, "Could not calculate {0} (format: \"{1}\", other date: \"{2}\")", type, zonedFormatter.toString(), otherDateString);
            return "invalid date";
        }
    }

    public @NotNull String calculateTimeBetweenWithoutFormat(
        @Nullable final OfflinePlayer player, @NotNull final String args,
        final boolean isCountdown
    ) {
        return calculateTimeBetween(player, args, isCountdown, false);
    }

}
