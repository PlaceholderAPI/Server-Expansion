package com.extendedclip.papi.expansion.server;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public final class ServerUtils {

    public static final String VERSION = Bukkit.getBukkitVersion().split("-")[0];
    public static final String BUILD;
    
    private static final Map<String, String> variants = new HashMap<>();

    static {
        boolean isPaper = false;
        variants.put("Purpur", "net.pl3x.purpur.PurpurConfig");
        variants.put("Airplane", "gg.airplane.AirplaneConfig");
        variants.put("Tuinity", "com.tuinity.tuinity.config.TuinityConfig");
        variants.put("Paper", "com.destroystokyo.paper.PaperConfig");
        variants.put("Spigot", "org.spigotmc.SpigotConfig");

        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaper = true;
        } catch (ClassNotFoundException ignored) { }

        String[] buildParts = Bukkit.getVersion().split("-");

        if (buildParts.length >= 3) {
            BUILD = isPaper ? buildParts[2].substring(0, buildParts[2].indexOf(" ")) : buildParts[0];
        } else {
            BUILD = "UNKNOWN";
        }
    }

    private ServerUtils() { }

    public static String getServerVariant() {

        for (Map.Entry<String, String> variant : variants.entrySet()) {
            try {
                Class.forName(variant.getValue());
                return variant.getKey();
            } catch (ClassNotFoundException ignored) { }
        }

        return "Unknown";
    }

}
