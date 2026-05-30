package at.helpch.papi.expansion.server;

import at.helpch.placeholderapi.PlaceholderAPIPlugin;
import at.helpch.placeholderapi.expansion.PlaceholderExpansion;

import java.time.Instant;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.HytaleServerConfig;

public final class ServerExpansion extends PlaceholderExpansion {
  public String getIdentifier() {
    return "hyServer";
  }
  
  public String getAuthor() {
    return "HelpChat";
  }
  
  public String getVersion() {
    return "1.0.0";
  }
  
  public String onPlaceholderRequest(PlayerRef player, String identifier) {
    if (player == null)
      return ""; 

    switch (identifier) {
      case "name":
        return String.valueOf(HytaleServerConfig.load().getServerName());
      case "online":
        return String.valueOf(Universe.get().getPlayers().size());
      case "worlds":
        return String.valueOf(Universe.get().getWorlds().size());
      case "max_players":
        return String.valueOf(HytaleServerConfig.load().getMaxPlayers());
      case "max_view_radius":
        return String.valueOf(HytaleServerConfig.load().getMaxViewRadius());
      case "motd":
        return String.valueOf(HytaleServerConfig.load().getMotd());
      case "default_gamemode":
        return String.valueOf(HytaleServerConfig.load().getDefaults().getGameMode());
      case "default_world":
        return String.valueOf(HytaleServerConfig.load().getDefaults().getWorld());
      case "rate_limit_enabled":
        return bool(HytaleServerConfig.load().getRateLimitConfig().isEnabled());
      case "rate_limit_packets_per_second":
        return String.valueOf(HytaleServerConfig.load().getRateLimitConfig().getPacketsPerSecond());
      case "rate_limit_burst_capacity":
        return String.valueOf(HytaleServerConfig.load().getRateLimitConfig().getBurstCapacity());

      case "is_booting":
        return bool(HytaleServer.get().isBooting());
      case "is_shutting_down":
        return bool(HytaleServer.get().isShuttingDown());
      case "boot_timestamp":
        return String.valueOf(HytaleServer.get().getBoot());

      case "uptime_millis": {
        Instant bootTime = HytaleServer.get().getBoot();
        long uptimeMillis = System.currentTimeMillis() - bootTime.toEpochMilli();
        return String.valueOf(Math.max(0L, uptimeMillis));
      }

      case "uptime_seconds": {
        Instant bootTime = HytaleServer.get().getBoot();
        long uptimeSeconds = (System.currentTimeMillis() - bootTime.toEpochMilli()) / 1000L;
        return String.valueOf(Math.max(0L, uptimeSeconds));
      }
      case "uptime": {
        Instant bootTime = HytaleServer.get().getBoot();
        long uptimeMillis = System.currentTimeMillis() - bootTime.toEpochMilli();
        return formatDuration(Math.max(0L, uptimeMillis));
      }
      case "plugin_count":
        return String.valueOf(PluginManager.get().getPlugins().size());

    } 

   
    return null;
  }
  
  public String bool(boolean b) {
    return b ? PlaceholderAPIPlugin.instance().configManager().config().booleanValue().trueValue() : PlaceholderAPIPlugin.instance().configManager().config().booleanValue().falseValue();
  }

  private String formatDuration(long uptimeMillis) {
    long totalSeconds = uptimeMillis / 1000L;
    long seconds = totalSeconds % 60L;
    long totalMinutes = totalSeconds / 60L;
    long minutes = totalMinutes % 60L;
    long totalHours = totalMinutes / 60L;
    long hours = totalHours % 24L;
    long days = totalHours / 24L;

    StringBuilder sb = new StringBuilder();
    if (days > 0) sb.append(days).append("d ");
    if (hours > 0 || days > 0) sb.append(hours).append("h ");
    if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
    sb.append(seconds).append("s");
    return sb.toString().trim();
  }
}