package com.extendedclip.papi.expansion.server;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerUtils {
    
    private String version = null;
    private String build = null;
    private String variant = null;
    
    private final Map<String, String> variants = new HashMap<>();
    
    private Object craftServer = null;
    private Field tps = null;
    
    private boolean hasTpsMethod = false;
    
    public ServerUtils() {
        variants.put("Spigot", "org.spigotmc.SpigotConfig");
        variants.put("Paper", "com.destroystokyo.paper.PaperConfig");
        variants.put("Tuinity", "com.tuinity.tuinity.config.TuinityConfig");
        variants.put("Airplane", "gg.airplane.AirplaneConfig");
        variants.put("Purpur", "net.pl3x.purpur.PurpurConfig");
        
        resolveTPSHandler();
    }
    
    public String getServerVariant() {
        if (variant != null) {
            return variant;
        }
        
        for(Map.Entry<String, String> variant : variants.entrySet()) {
            try {
                Class.forName(variant.getValue());
                
                return (this.variant = variant.getKey());
            } catch (ClassNotFoundException ignored) {} 
        }
        
        return (this.variant = "Unknown");
    }
    
    public String getVersion() {
        if (version != null) {
            return version;
        }
        
        return (version = Bukkit.getBukkitVersion().split("-")[0]);
    }
    
    public String getBuild() {
        if (build != null) {
            return build;
        }
        
        String[] buildParts = Bukkit.getVersion().split("-");
        switch (getServerVariant().toLowerCase(Locale.ROOT)) {
            case "spigot":
            // TODO: Find out what those variants return.
            case "tuinity":
            case "airplane":
            case "purpur":
                return (build = buildParts[0]);
            
            case "paper":
                if (buildParts.length >= 3) {
                    if (buildParts[2].contains(" ")) {
                        return (build = buildParts[2].substring(0, buildParts[2].indexOf(" ")));
                    } else {
                        return (build = buildParts[2]);
                    }
                } else {
                    return (build = "Unknown");
                }
            
            default:
                return (build = "Unknown");
        }
    }
    
    public double[] getTps() {
        if (hasTpsMethod) {
            return Bukkit.getTPS();
        }
        
        if (craftServer == null || tps == null) {
            return new double[]{0, 0, 0};
        }
        
        try {
            return (double[]) tps.get(craftServer);
        } catch (IllegalAccessException ignored) {
            return new double[]{0, 0, 0};
        }
    }
    
    private void resolveTPSHandler() {
        try {
            // If this throws is the server not a fork...
            Bukkit.class.getMethod("getTPS");
            hasTpsMethod = true;
        } catch (NoSuchMethodException ignored) {
            final String mcVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            
            try {
                if (getMajorVersion() >= 17) {
                    craftServer = Class.forName("net.minecraft.server.MinecraftServer")
                        .getMethod("getServer").invoke(null);
                } else {
                    craftServer = Class.forName("net.minecraft.server." + mcVersion + ".MinecraftServer")
                        .getMethod("getServer").invoke(null);
                }
                
                tps = craftServer.getClass().getField("recentTps");
            } catch (Exception ex) {
                PlaceholderAPIPlugin.getInstance().getLogger().warning("Could not resolve TPS handling!");
                ex.printStackTrace();
            }
        }
    }
    
    private int getMajorVersion() {
        final Matcher matcher = Pattern.compile("\\(MC: (\\d)\\.(\\d+)\\.?(\\d+?)?\\)").matcher(Bukkit.getVersion());
        if (matcher.find()) {
            try{
                return Integer.parseInt(matcher.toMatchResult().group(2), 10);
            } catch (NumberFormatException ignored) {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
