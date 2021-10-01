package com.extendedclip.papi.expansion.server;

import org.bukkit.Bukkit;

public final class ServerUtils {

    public static final String VERSION = Bukkit.getBukkitVersion().split("-")[0];
    public static final String BUILD;

    static {
        String[] buildParts = Bukkit.getVersion().split("-");

        if (buildParts.length >= 3) {
            BUILD = buildParts[2];
        } else {
            BUILD = "UNKNOWN";
        }
    }

    private ServerUtils() { }

    public static String getServerVariant() {
        try {
            Class.forName("net.pl3x.purpur.PurpurConfig");
            return "Purpur";
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.tuinity.tuinity.config.TuinityConfig");
                return "Tuinity";
            } catch (ClassNotFoundException e1) {
                try {
                    Class.forName("com.destroystokyo.paper.PaperConfig");
                    return "Paper";
                } catch (ClassNotFoundException e2) {
                    try {
                        Class.forName("org.spigotmc.SpigotConfig");
                        return "Spigot";
                    } catch (ClassNotFoundException e3) {
                        return "Unknown";
                    }
                }
            }
        }
    }

}
