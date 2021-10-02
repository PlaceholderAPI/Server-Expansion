package at.helpch.placeholderapi.expansion.server;

import at.helpch.placeholderapi.api.expansion.Expansion;
import at.helpch.placeholderapi.api.expansion.ExpansionDescription;
import at.helpch.placeholderapi.api.expansion.Platform;
import at.helpch.placeholderapi.api.expansion.placeholder.Placeholder;
import at.helpch.placeholderapi.api.expansion.placeholder.PlaceholderContext;
import at.helpch.placeholderapi.api.server.Server;
import at.helpch.placeholderapi.api.server.keyable.key.ServerKeys;
import at.helpch.placeholderapi.expansion.time.TimeFormatter;
import at.helpch.placeholderapi.expansion.time.file.Config;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@ExpansionDescription(
        name = "Server",
        version = "2.0.0",
        identifier = "server",
        authors = "HelpChat",
        platforms = {Platform.BUKKIT, Platform.NUKKIT, Platform.SPONGE}
)
public final class ServerExpansion extends Expansion {

    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final int MB = 1048576;

    private final TimeFormatter timeFormatter;
    private final Config config;
    private final Server<?> server;
    private final Map<String, SimpleDateFormat> dateFormats = new HashMap<>();

    @Inject
    public ServerExpansion(@NotNull final Server<?> server, @NotNull final Config config) {
        this.server = server;
        this.config = config;

        // TODO: instantiate a single TimeFormatter through out the project, with a main config
        this.timeFormatter = new TimeFormatter(config);
    }

    @Override
    public void enable() {
        register();
    }

    private void register() {
        server("name", server -> this.config.getServerName().orElse("A Minecraft Server"));
        server("online", server -> server.get(ServerKeys.PLAYERS).orElse(Collections.emptySet()).size());
        server("version", server1 -> server.get(ServerKeys.VERSION));
        server("max_players", server -> server.get(ServerKeys.MAX_PLAYERS));
        server("unique_joins", server -> server.get(ServerKeys.OFFLINE_PLAYERS).orElse(0));
        server("uptime", server -> timeFormatter.format(Duration.ofSeconds(TimeUnit.MILLISECONDS.toSeconds(server.get(ServerKeys.UPTIME).orElse(0L)))));
        server("has_whitelist", server -> server.get(ServerKeys.WHITELISTED));
    }

    private void server(@NotNull final String name, @NotNull final Function<Server<?>, Object> function) {
        registerPlaceholder(context -> function.apply(this.server), name);
    }

    @Placeholder("countdown")
    private String countdown(final PlaceholderContext context) {
        final List<String> arguments = context.getArguments();
        final String argument = String.join("_", arguments);

        if (!argument.contains("_")) {
            Date then;
            try {
                then = new SimpleDateFormat(config.getDateFormat().getFormat()).parse(argument);
            } catch (final ParseException ignored) {
                return null;
            }

            final Date now = new Date();
            final long between = then.getTime() - now.getTime();

            if (between <= 0) {
                return null;
            }

            return timeFormatter.format(Duration.ofSeconds(TimeUnit.MILLISECONDS.toSeconds(between)));
        }

        if (arguments.size() != 2) {
            return "Invalid Date Format and Time.";
        }

        SimpleDateFormat format;
        try {
            format = new SimpleDateFormat(arguments.get(0));
        } catch (final IllegalArgumentException ignored) {
            return "Invalid Date Format.";
        }

        Date then;
        try {
            then = format.parse(arguments.get(1));
        } catch (final ParseException ignored) {
            return "Invalid Date.";
        }

        final long current = System.currentTimeMillis();
        final long between = then.getTime() - current;

        return timeFormatter.format(Duration.ofSeconds(TimeUnit.MICROSECONDS.toSeconds(between)));
    }

    @Placeholder("time")
    private String time(final PlaceholderContext context) {
        final String argument = String.join(" ", context.getArguments());

        if (dateFormats.get(argument) != null) {
            return dateFormats.get(argument).format(new Date());
        }

        try {
            final SimpleDateFormat format = new SimpleDateFormat(argument);
            dateFormats.put(argument, format);

            return format.format(new Date());
        } catch (final NullPointerException | IllegalArgumentException ignored) {
            return null;
        }
    }

    @Placeholder("total")
    private String total(final PlaceholderContext context) {
        final String argument = String.join("_", context.getArguments());

        switch (argument.toLowerCase()) {
            case "chunks":
                return Long.toString(server.get(ServerKeys.LOADED_CHUNKS).orElse(0L));
            case "living_entities":
                return Long.toString(server.get(ServerKeys.LIVING_ENTITIES).orElse(0L));
            case "entities":
                return Long.toString(server.get(ServerKeys.TOTAL_ENTITIES).orElse(0L));
        }

        return "";
    }

    @Placeholder("online")
    private String online(final PlaceholderContext context) {
        final String argument = String.join("_", context.getArguments());

        final Optional<Map<String, Long>> worlds = server.get(ServerKeys.WORLD_PLAYERS);
        return Long.toString(worlds.isPresent() ? worlds.get().get(argument) : 0L);
    }

    @Placeholder("tps")
    private String tps(final PlaceholderContext context) {
        final List<String> arguments = context.getArguments();

        final double currentTps = server.get(ServerKeys.CURRENT_TPS).orElse(0d);
        return arguments.size() == 0 ? Double.toString(fix(currentTps)) : color(currentTps);
    }

    private double fix(final double tps) {
        return Math.min(Math.round(tps * 100.0) / 100.0, 20.0);
    }

    private String color(final double tps) {
        final Config.TpsColor color = config.getTpsColor();

        return tps > 18.0 ? color.getHigh() : (tps > 16.0 ? color.getMedium() : color.getLow()) + "" + fix(tps);
    }

    @Placeholder("ram")
    private String ram(final PlaceholderContext context) {
        final String argument = String.join("_", context.getArguments());

        switch (argument.toLowerCase()) {
            case "used":
                return Long.toString((RUNTIME.totalMemory() - RUNTIME.freeMemory()) / MB);
            case "free":
                return Long.toString(RUNTIME.freeMemory() / MB);
            case "total":
                return Long.toString(RUNTIME.totalMemory() / MB);
            case "max":
                return Long.toString(RUNTIME.maxMemory() / MB);
        }

        return "";
    }

}
