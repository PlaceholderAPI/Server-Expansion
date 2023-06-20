package at.helpch.placeholderapi.expansion.server;

import at.helpch.placeholderapi.expansion.server.util.ServerUtil;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;
import java.util.logging.Level;

public final class ServerExpansion extends PlaceholderExpansion implements Cacheable, Configurable {

    private final Cache<String, Integer> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    private final Map<String, SimpleDateFormat> dateFormats = new HashMap<>();

    private final Runtime runtime = Runtime.getRuntime();
    /**
     * Constant that represents how many bytes are in a megabyte
     */
    private final int MB = 1_048_576;

    // ----- Values from config
    private String serverName;

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
        return true;
    }

    @Override
    public void clear() {

    }

    @Override
    public Map<String, Object> getDefaults() {
        return ImmutableMap.<String, Object>builder()
                .put("server_name", "A Minecraft Server")
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
            PlaceholderAPIPlugin.getInstance()
                    .getLogger()
                    .log(Level.WARNING, "[server] Could not get '" + key + "' from cache", e);
            return "";
        }
    }

    /**
     * Format a boolean {@code boolean} as {@link PlaceholderAPIPlugin#booleanTrue()} or {@link PlaceholderAPIPlugin#booleanFalse()}
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

        final World world = Bukkit.getWorld(worldName);
        return world == null ? -1 : world.getPlayers().size();
    }

    /**
     * Format the current time with the given format
     *
     * @param format format
     * @return {@code null} if the format is empty or invalid, otherwise the current {@link Date} formatted
     */
    private @Nullable String formatTime(@NotNull final String format) {
        if (format.trim().isEmpty()) {
            return null;
        }

        final SimpleDateFormat simpleDateFormat = dateFormats.computeIfAbsent(format, __ -> {
            try {
                return new SimpleDateFormat(format);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        });
        return simpleDateFormat == null ? null : simpleDateFormat.format(new Date());
    }

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
            // TODO add version_build and version_full (same placeholder)
            // -----

            // ----- RAM placeholders
            case "ram":
                return String.valueOf((runtime.totalMemory() - runtime.freeMemory()) / MB);
            case "ram_free":
                return String.valueOf(runtime.freeMemory() / MB);
            case "ram_total":
                return String.valueOf(runtime.totalMemory() / MB);
            case "ram_max":
                return String.valueOf(runtime.maxMemory() / MB);
            // -----

            case "total_chunks":
                return getFromCache("chunks", () -> getFromAllWorlds(world -> world.getLoadedChunks().length));
            case "total_living_entities":
                return getFromCache("livingEntities", () -> getFromAllWorlds(world -> world.getLivingEntities().size()));
            case "total_entities":
                return getFromCache("totalEntities", () -> getFromAllWorlds(world -> world.getEntities().size()));
            case "has_whitelist":
                return bool(Bukkit.hasWhitelist());
        }

        // online_<world name>
        if (params.startsWith("online_")) {
            return String.valueOf(getOnlinePlayers(params.replace("online_", "")));
        }

        // time_<simple date format>
        if (params.startsWith("time_")) {
            return formatTime(params.replace("time_", ""));
        }

        return null;
    }

}
