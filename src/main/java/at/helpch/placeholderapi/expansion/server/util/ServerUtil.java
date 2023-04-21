package at.helpch.placeholderapi.expansion.server.util;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

public final class ServerUtil {

    private static final Map<String, String> variants = new TreeMap<>();
    private static final String variant;

    // TPS stuff
    private static final Object craftServer;
    private static final Field tpsField;
    private static boolean hasTpsMethod; // Paper and its forks have Bukkit#getTps

    static {
        variants.put("net.pl3x.purpur.PurpurConfig", "Purpur");
        variants.put("gg.airplane.AirplaneConfig", "Airplane");
        variants.put("com.tuinity.tuinity.config.TuinityConfig", "Tuinity");
        variants.put("io.papermc.paper.configuration.ConfigurationLoaders", "Paper"); // New config location for Paper 1.19+
        variants.put("com.destroystokyo.paper.PaperConfig", "Paper"); // Still supported by Paper, but deprecated.
        variants.put("org.spigotmc.SpigotConfig", "Spigot");

        variant = findVariant();
        craftServer = getCraftServer();
        tpsField = getTpsHandler();
    }

    private static String findVariant() {
        try {
            for (final Map.Entry<String, String> entry : variants.entrySet()) {
                Class.forName(entry.getKey());
                return entry.getValue();
            }
        } catch (ClassNotFoundException ignored) { }

        // TODO attempt to get the variant from one extra place before returning Unknown
        //      Example 1: https://github.com/PaperMC/Paper/blob/f7717c3712265fd480d6ff0ad808c430b9972004/patches/server/0027-Show-Paper-in-client-crashes-server-lists-and-Mojang.patch#L17
        //      Example 2: https://github.com/PurpurMC/Purpur/blob/678eafef721b1a3d819fbaff7dd980bc9f4ee785/patches/server/0003-Rebrand.patch#L208
        return "Unknown";
    }

    private static Object getCraftServer() {
        try {
            Class<?> clas;

            if (VersionHelper.OVER_1_17) {
                clas = Class.forName("net.minecraft.server.MinecraftServer");
            } else {
                clas = Class.forName(String.format("net.minecraft.server.%s.MinecraftServer", VersionHelper.NMS_VERSION));
            }

            return clas.getMethod("getServer").invoke(null);
        } catch (ClassNotFoundException e) {
            PlaceholderAPIPlugin.getInstance().getLogger().log(Level.SEVERE, "[server] Could not find class MinecraftServer", e);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            PlaceholderAPIPlugin.getInstance().getLogger().log(Level.SEVERE, "[server] Could not invoke method MinecraftServer#getServer", e);
        }

        return null;
    }

    private static Field getTpsHandler() {
        try {
            Bukkit.class.getMethod("getTPS");
            hasTpsMethod = true;
        } catch (NoSuchMethodException ignored) { }

        if (craftServer == null) {
            PlaceholderAPIPlugin.getInstance().getLogger().log(Level.WARNING, "Could not get field 'recentTps' from class MinecraftServer because variable 'craftServer' is null");
            return null;
        }

        try {
            return craftServer.getClass().getField("recentTps");
        } catch (NoSuchFieldException e) {
            PlaceholderAPIPlugin.getInstance().getLogger().log(Level.WARNING, "Could not find field 'recentTps' in class " + craftServer.getClass().getName(), e);
            return null;
        }
    }

    public static String getVariant() {
        return variant;
    }

    public static double[] getTps() {
        if (hasTpsMethod) {
            return Bukkit.getTPS();
        }

        if (craftServer == null || tpsField == null) {
            return new double[]{0, 0, 0};
        }

        try {
            return (double[]) tpsField.get(craftServer);
        } catch (IllegalAccessException e) {
            return new double[]{0, 0, 0};
        }
    }

}
