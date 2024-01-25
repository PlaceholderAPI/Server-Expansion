package at.helpch.placeholderapi.expansion.server.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.TreeMap;

public final class ServerUtil {

    private static final Map<String, String> variants = new TreeMap<>();
    private static final String variant;
    private static final String serverVersion;
    private static final String build;

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

        craftServer = getCraftServer();
        tpsField = getTpsHandler();
        variant = findVariant();
        serverVersion = Bukkit.getBukkitVersion().split("-")[0]; // The version is formatted as 1.20.1-R0.1-SNAPSHOT
        build = findBuild();
    }

    private static String findVariant() {
        for (final Map.Entry<String, String> entry : variants.entrySet()) {
            try {
                    Class.forName(entry.getKey());
                    return entry.getValue();
            } catch (ClassNotFoundException ignored) { }
        }

        // Source: https://github.com/PaperMC/Paper/blob/f7717c3/patches/server/0027-Show-Paper-in-client-crashes-server-lists-and-Mojang.patch#L17
        if (craftServer != null) {
            try {
                return (String) craftServer.getClass()
                    .getMethod("getServerModName")
                    .invoke(craftServer);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) { }
        }

        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return "Spigot";
        } catch (ClassNotFoundException ignored) {
            return "Unknown"; // Not even spigot??
        }
    }

    private static Object getCraftServer() {
        try {
            Class<?> clas;

            if (VersionHelper.IS_1_17_OR_HIGHER) {
                clas = Class.forName("net.minecraft.server.MinecraftServer");
            } else {
                clas = Class.forName(String.format("net.minecraft.server.%s.MinecraftServer", VersionHelper.NMS_VERSION));
            }

            return clas.getMethod("getServer").invoke(null);
        } catch (ClassNotFoundException e) {
            Logging.error(e, "Could not find class MinecraftServer");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Logging.error(e, "Could not invoke method MinecraftServer#getServer");
        }

        return null;
    }

    private static Field getTpsHandler() {
        try {
            Bukkit.class.getMethod("getTPS");
            hasTpsMethod = true;
            return null;
        } catch (NoSuchMethodException ignored) { }

        if (craftServer == null) {
            Logging.warn("Could not get field 'recentTps' from class MinecraftServer because variable 'craftServer' is null");
            return null;
        }

        try {
            return craftServer.getClass().getField("recentTps");
        } catch (NoSuchFieldException e) {
            Logging.error(e, "Could not find field 'recentTps' in class {0}", craftServer.getClass().getName());
            return null;
        }
    }

    private static String findBuild() {
        String[] buildParts = Bukkit.getVersion().split("-");

        switch (getVariant().toLowerCase()) {
            case "spigot":
                return buildParts[0];

            // Source: https://github.com/PaperMC/Paper/blob/0ae58c0/patches/server/0001-Setup-Gradle-project.patch#L75
            case "paper":
            // Source: https://github.com/Tuinity/Tuinity/blob/4c31ea1/patches/server/0001-Build-changes.patch#L34
            case "tuinity":
            // Source: https://github.com/PurpurMC/Purpur/blob/c049bfb/patches/server/0002-Rebrand.patch#L44
            case "purpur":
            // Source: https://github.com/TECHNOVE/Airplane/blob/af3563c/patches/server/0002-Airplane-Branding-Changes.patch#L41
            case "airplane": {
                // Format: git-<Paper/Tuinity/Purpur/Airplane>-<version>
                if (buildParts.length >= 3) {
                    if (buildParts[2].contains(" ")) {
                        return buildParts[2].substring(0, buildParts[2].indexOf(" "));
                    } else {
                        return buildParts[2];
                    }
                }
            }
        }

        return "Unknonw";
    }

    public static String getVariant() {
        return variant;
    }

    public static String getServerVersion() {
        return serverVersion;
    }

    public static String getBuild() {
        return build;
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
