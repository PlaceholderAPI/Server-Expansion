package at.helpch.placeholderapi.expansion.server;

import at.helpch.placeholderapi.expansion.server.util.Logging;
import at.helpch.placeholderapi.expansion.server.util.ServerUtil;
import at.helpch.placeholderapi.expansion.server.util.TimeFormatter;
import at.helpch.placeholderapi.expansion.server.util.TpsFormatter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;

public final class ServerExpansion extends PlaceholderExpansion implements Cacheable, Configurable {

    private final Cache<String, Integer> cache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();

    private final Runtime runtime = Runtime.getRuntime();

    /**
     * Constant that represents how many bytes are in a mebibyte
     * <br/>
     * Required for the {@code ram_} placeholders, as the values provided by the system are in bytes
     */
    private final int MiB = 1_048_576;

    // ----- Values from config
    private String serverName;
    // -----

    private TpsFormatter tpsFormatter;
    private TimeFormatter timeFormatter;

    @Override
    public @NotNull String getIdentifier() {
        return "server";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HelpChat";
    }

    @Override
    public @NotNull String getVersion() {
        return "3.0.0";
    }

    @Override
    public boolean canRegister() {
        this.serverName = getString("server_name", "A Minecraft Server");
        this.tpsFormatter = new TpsFormatter(
            getString("tps_color.low", "&c"),
            getString("tps_color.medium", "&e"),
            getString("tps_color.high", "&a")
        );
        this.timeFormatter = new TimeFormatter(
            Locale.forLanguageTag(this.getString("time.locale", Locale.getDefault().toLanguageTag())),
            ZoneId.of(this.getString("time.zone", ZoneId.systemDefault().getId())),
            Optional.ofNullable(getConfigSection("time.suffix")).map(section -> section.getValues(false)).orElseGet(HashMap::new)
        );
        return true;
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        timeFormatter = null;
        tpsFormatter = null;
    }

    @Override
    public Map<String, Object> getDefaults() {
        return ImmutableMap.<String, Object>builder()
            .put("server_name", "A Minecraft Server")
            .put("time.locale", Locale.getDefault().toLanguageTag())
            .put("time.zone", ZoneId.systemDefault().getId())
            .put("time.suffix.week", "w")
            .put("time.suffix.day", "d")
            .put("time.suffix.hour", "h")
            .put("time.suffix.minute", "m")
            .put("time.suffix.second", "s")
            .put("tps_color.high", "&a")
            .put("tps_color.medium", "&e")
            .put("tps_color.low", "&c")
            .build();
    }

    /**
     * Get some data from all worlds
     *
     * @param function data to get (e.g. total entities)
     * @return sum
     */
    private int getFromAllWorlds(@NotNull final ToIntFunction<? super World> function) {
        return Bukkit.getWorlds()
            .stream()
            .mapToInt(function)
            .sum();
    }

    /**
     * Get a value from the {@link #cache}
     *
     * @param key      key
     * @param callable cache method
     * @return value if found otherwise empty
     */
    private @NotNull String getFromCache(@NotNull final String key, @NotNull final Callable<Integer> callable) {
        try {
            return String.valueOf(cache.get(key, callable));
        } catch (ExecutionException e) {
            Logging.error(e, "Could not get key \"{0}\" from cache", key);
            return "";
        }
    }

    /**
     * Format a {@code boolean} as {@link PlaceholderAPIPlugin#booleanTrue()} or {@link PlaceholderAPIPlugin#booleanFalse()}
     *
     * @param bool boolean
     * @return {@link PlaceholderAPIPlugin#booleanTrue()} if the boolean is true, otherwise {@link PlaceholderAPIPlugin#booleanFalse()}
     */
    private @NotNull String bool(final boolean bool) {
        return bool ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
    }

    /**
     * Get the number of online players from a world
     *
     * @param worldName the name of the world
     * @return {@code -1} if {@code worldName} is empty or the world doesn't exist, otherwise {@link World#getPlayers()}.size()
     */
    private int getOnlinePlayers(@NotNull final String worldName) {
        if (worldName.trim().isEmpty()) {
            return -1;
        }

        return Optional.ofNullable(Bukkit.getWorld(worldName))
            .map(World::getPlayers)
            .map(List::size)
            .orElse(-1);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params) {
            // ----- Players placeholders
            case "online":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max_players":
                return String.valueOf(Bukkit.getMaxPlayers());
            case "unique_joins":
                return String.valueOf(Bukkit.getOfflinePlayers().length);
            // -----

            // ----- Server software placeholders
            case "name":
                return this.serverName;
            case "variant":
                return ServerUtil.getVariant();
            case "version":
                return ServerUtil.getServerVersion();
            case "build":
                return ServerUtil.getBuild();
            case "version_build":
            case "version_full":
                return ServerUtil.getServerVersion() + '-' + ServerUtil.getBuild();
            // -----

            // ----- RAM placeholders
            case "ram_used":
                return String.valueOf((runtime.totalMemory() - runtime.freeMemory()) / MiB);
            case "ram_free":
                return String.valueOf(runtime.freeMemory() / MiB);
            case "ram_total":
                return String.valueOf(runtime.totalMemory() / MiB);
            case "ram_max":
                return String.valueOf(runtime.maxMemory() / MiB);
            // -----

            case "tps":
                return tpsFormatter.getTps(null);
            case "uptime":
                return timeFormatter.formatTimeInSeconds(TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime()));
            case "total_chunks":
                return getFromCache("chunks", () -> getFromAllWorlds(world -> world.getLoadedChunks().length));
            case "total_living_entities":
                return getFromCache("livingEntities", () -> getFromAllWorlds(world -> world.getLivingEntities().size()));
            case "total_entities":
                return getFromCache("totalEntities", () -> getFromAllWorlds(world -> world.getEntities().size()));
            case "has_whitelist":
                return bool(Bukkit.hasWhitelist());
        }

        // tps_<type>
        if (params.startsWith("tps_")) {
            return tpsFormatter.getTps(params.substring("tps_".length()));
        }

        // online_<world name>
        if (params.startsWith("online_")) {
            return String.valueOf(getOnlinePlayers(params.substring("online_".length())));
        }

        // time_<simple date format>
        if (params.startsWith("time_")) {
            return timeFormatter.formatTime(params.substring("time_".length()));
        }

        // countdown_raw_<date> or countdown_raw_<custom format>_<date>
        if (params.startsWith("countdown_raw_")) {
            return timeFormatter.calculateTimeBetweenWithoutFormat(player, params.substring("countdown_raw_".length()), true);
        }

        // countdown_<date> or countdown_<custom format>_<date>
        if (params.startsWith("countdown_")) {
            return timeFormatter.calculateTimeBetween(player, params.substring("countdown_".length()), true, true);
        }

        // countup_raw_<date> or countup_raw_<custom format>_<date>
        if (params.startsWith("countup_raw_")) {
            return timeFormatter.calculateTimeBetweenWithoutFormat(player, params.substring("countup_raw_".length()), false);
        }

        // countup_<date> or countup_<custom format>_<date>
        if (params.startsWith("countup_")) {
            return timeFormatter.calculateTimeBetween(player, params.substring("countup_".length()), false, true);
        }

        return null;
    }

}
