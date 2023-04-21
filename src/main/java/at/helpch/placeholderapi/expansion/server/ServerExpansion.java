package at.helpch.placeholderapi.expansion.server;

import at.helpch.placeholderapi.expansion.server.util.ServerUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;

public class ServerExpansion extends PlaceholderExpansion implements Cacheable, Configurable {

    private final Cache<String, Integer> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    private final Runtime runtime = Runtime.getRuntime();

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
    public void clear() {

    }

    @Override
    public Map<String, Object> getDefaults() {
        return null;
    }

    private int sumAllWorlds(@NotNull final ToIntFunction<? super World> function) {
        return Bukkit.getWorlds()
                .stream()
                .mapToInt(function)
                .sum();
    }

    private String getFromCache(@NotNull final String key, @NotNull final Callable<Integer> callable) {
        try {
            return String.valueOf(cache.get(key, callable));
        } catch (ExecutionException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params) {
            case "variant":
                return ServerUtil.getVariant();
            case "total_chunks":
                return getFromCache("chunks", () -> sumAllWorlds(world -> world.getLoadedChunks().length));
            case "total_living_entities":
                return getFromCache("livingEntities", () -> sumAllWorlds(world -> world.getLivingEntities().size()));
            case "total_entities":
                return getFromCache("totalEntities", () -> sumAllWorlds(world -> world.getEntities().size()));
        }

        return null;
    }

}
