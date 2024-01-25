package at.helpch.placeholderapi.expansion.server.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;
import java.util.function.Function;

public class TpsFormatter {

    private final String lowColor;
    private final String mediumColor;
    private final String highColor;

    /**
     * @param lowColor    color for {@code tps < 16.0}
     * @param mediumColor color for {@code tps < 18.0}
     * @param highColor   color for {@code tps > 18.0}
     */
    public TpsFormatter(@NotNull final String lowColor, @NotNull final String mediumColor, @NotNull final String highColor) {
        this.lowColor = ChatColor.translateAlternateColorCodes('&', lowColor);
        this.mediumColor = ChatColor.translateAlternateColorCodes('&', mediumColor);
        this.highColor = ChatColor.translateAlternateColorCodes('&', highColor);
    }

    /**
     * Round the tps and append a {@code *} before if the value is higher than 20.0
     *
     * @param tps tps
     * @return tps rounded
     */
    private @NotNull String round(final double tps) {
        final double finalTps = Math.min(Math.round(tps), 20.0);
        return (tps > 20.0 ? "*" : "") + finalTps;
    }

    /**
     * Get the color based on the tps value
     *
     * @param tps tps
     * @return color for the tps
     */
    private @NotNull String getColor(final double tps) {
        return (tps > 18.0) ? highColor : (tps > 16.0) ? mediumColor : lowColor;
    }

    private @NotNull String getPercent(final double tps) {
        final double percent = Math.min(Math.round(100 / 20.0 * tps), 100.0);
        return (tps > 20.0 ? "*" : "") + percent + '%';
    }

    private @NotNull String getColoredTps(final double tps) {
        return getColor(tps) + round(tps);
    }

    private @NotNull String getColoredTpsPercent(final double tps) {
        return getColor(tps) + getPercent(tps);
    }

    private @NotNull String getAllTps(@NotNull final Function<Double, String> formatter) {
        final StringJoiner joiner = new StringJoiner(ChatColor.GRAY + ", ");

        for (final double tps : ServerUtil.getTps()) {
            joiner.add(formatter.apply(tps));
        }

        return joiner.toString();
    }

    public @Nullable String getTps(@Nullable final String type) {
        if (type == null || type.isEmpty()) {
            return getAllTps(this::getColoredTps);
        }

        switch (type) {
            case "1":
            case "one":
                return round(ServerUtil.getTps()[0]);

            case "5":
            case "five":
                return round(ServerUtil.getTps()[1]);

            case "15":
            case "fifteen":
                return round(ServerUtil.getTps()[2]);

            case "1_colored":
            case "one_colored":
                return getColoredTps(ServerUtil.getTps()[0]);

            case "5_colored":
            case "five_colored":
                return getColoredTps(ServerUtil.getTps()[1]);

            case "15_colored":
            case "fifteen_colored":
                return getColoredTps(ServerUtil.getTps()[2]);

            case "percent":
                return getAllTps(this::getColoredTpsPercent);

            case "1_percent":
            case "one_percent":
                return getPercent(ServerUtil.getTps()[0]);

            case "5_percent":
            case "five_percent":
                return getPercent(ServerUtil.getTps()[1]);

            case "15_percent":
            case "fifteen_percent":
                return getPercent(ServerUtil.getTps()[2]);

            case "1_percent_colored":
            case "one_percent_colored":
                return getColoredTpsPercent(ServerUtil.getTps()[0]);

            case "5_percent_colored":
            case "five_percent_colored":
                return getColoredTpsPercent(ServerUtil.getTps()[1]);

            case "15_percent_colored":
            case "fifteen_percent_colored":
                return getColoredTpsPercent(ServerUtil.getTps()[2]);
        }

        return null;
    }

}
