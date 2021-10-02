package com.extendedclip.papi.expansion.server;

import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ServerUtils {

    public static final String VERSION = Bukkit.getBukkitVersion().split("-")[0];
    public static final String BUILD;

    static {
        boolean isPaper = false;

        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaper = true;
        } catch (ClassNotFoundException ignored) { }

        String[] buildParts = Bukkit.getVersion().split("-");

        if (buildParts.length >= 3) {
            BUILD = isPaper ? buildParts[2] : buildParts[0];
        } else {
            BUILD = "UNKNOWN";
        }
    }

    private ServerUtils() { }

    @SuppressWarnings("SpellCheckingInspection")
    public static String getServerVariant() {
        final Map<String, String> variants = new LinkedHashMap<>();
        variants.put("Purpur", "net.pl3x.purpur.PurpurConfig");
        variants.put("Airplane", "gg.airplane.AirplaneConfig");
        variants.put("Tuinity", "com.tuinity.tuinity.config.TuinityConfig");
        variants.put("Paper", "com.destroystokyo.paper.PaperConfig");
        variants.put("Spigot", "org.spigotmc.SpigotConfig");

        for (Map.Entry<String, String> variant : variants.entrySet()) {
            try {
                Class.forName(variant.getValue());
                return variant.getKey();
            } catch (ClassNotFoundException ignored) { }
        }

        return "Unknown";
    }

}
