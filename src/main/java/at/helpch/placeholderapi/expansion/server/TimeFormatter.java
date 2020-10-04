package at.helpch.placeholderapi.expansion.server;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

final class TimeFormatter {

    private final Config.TimeFormat config;

    TimeFormatter(Config config) {
        this.config = config.getTimeFormat();
    }

    String format(@NotNull Duration duration) {
        final StringBuilder builder = new StringBuilder();

        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        days %= 7;

        insert(builder, seconds, config.getSecond(), config.getSeconds());
        insert(builder, minutes, config.getMinute(), config.getMinutes());
        insert(builder, hours, config.getHour(), config.getHours());
        insert(builder, days, config.getDay(), config.getDays());
        insert(builder, weeks, config.getWeek(), config.getWeeks());

        return builder.toString();
    }

    private void insert(final StringBuilder builder, final long value, final String singular, final String plural) {
        if (value > 0) {
            if (builder.length() > 0) {
                builder.insert(0, ' ');
            }

            builder.insert(0, value + (value > 1 ? plural : singular));
        }
    }
}