/*
 *
 * Server-Expansion
 * Copyright (C) 2018 Ryan McCarthy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.extendedclip.papi.expansion.server;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerExpansion extends PlaceholderExpansion implements Cacheable, Configurable {

	private final Map<String, SimpleDateFormat> dateFormats = new HashMap<>();
	private final int MB = 1048576;
	private final Runtime runtime = Runtime.getRuntime();
	private Object craftServer;
	private Field tps;
	private String version;
	private String serverName = null;
	private String low = "&c";
	private String medium = "&e";
	private String high = "&a";
	private String variant;

	private final String VERSION = getClass().getPackage().getImplementationVersion();

	public ServerExpansion() {
		try {
			version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
			if (minecraftVersion() >= 17) {
				craftServer = Class.forName("net.minecraft.server.MinecraftServer").getMethod("getServer").invoke(null);
			} else {
				craftServer = Class.forName("net.minecraft.server." + version + ".MinecraftServer").getMethod("getServer").invoke(null);
			}
			tps = craftServer.getClass().getField("recentTps");
			variant = initializeVariant();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean canRegister() {
		serverName = this.getString("server_name", "A Minecraft Server");
		low = this.getString("tps_color.low", "&c");
		medium = this.getString("tps_color.medium", "&e");
		high = this.getString("tps_color.high", "&a");
		return true;
	}

	@Override
	public void clear() {
		craftServer = null;
		tps = null;
		version = null;
		dateFormats.clear();
	}

	@Override
	public String getIdentifier() {
		return "server";
	}

	@Override
	public String getAuthor() {
		return "clip";
	}

	@Override
	public String getVersion() {
		return VERSION;
	}

	public String initializeVariant() {
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

	@Override
	public Map<String, Object> getDefaults() {
		final Map<String, Object> defaults = new HashMap<>();
		defaults.put("tps_color.high", "&a");
		defaults.put("tps_color.medium", "&e");
		defaults.put("tps_color.low", "&c");
		defaults.put("server_name", "A Minecraft Server");
		return defaults;
	}

	@Override
	public String onRequest(OfflinePlayer p, String identifier) {

		switch (identifier) {
		case "name":
			return serverName == null ? "" : serverName;
		case "variant":
			return variant;
		case "tps":
			return getTps(null);
		case "online":
			return String.valueOf(Bukkit.getOnlinePlayers().size());
		case "max_players":
			return String.valueOf(Bukkit.getMaxPlayers());
		case "unique_joins":
			return String.valueOf(Bukkit.getOfflinePlayers().length);
		case "uptime":
			long seconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
			return formatTime(Duration.of(seconds, ChronoUnit.SECONDS));
		case "has_whitelist":
			return Bukkit.getServer().hasWhitelist() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
		case "version":
			return Bukkit.getBukkitVersion().split("-")[0];
		case "ram_used":
			return String.valueOf((runtime.totalMemory() - runtime.freeMemory()) / MB);
		case "ram_free":
			return String.valueOf(runtime.freeMemory() / MB);
		case "ram_total":
			return String.valueOf(runtime.totalMemory() / MB);
		case "ram_max":
			return String.valueOf(runtime.maxMemory() / MB);
		case "total_chunks":
			int loadedChunks = 0;
			for (final World world : Bukkit.getWorlds()) {
				loadedChunks += world.getLoadedChunks().length;
			}
			return Integer.toString(loadedChunks);
		case "total_living_entities":
			int livingEntities = 0;
			for (final World world : Bukkit.getWorlds()) {
				livingEntities += world.getEntitiesByClasses(LivingEntity.class).size();
			}
			return Integer.toString(livingEntities);
		case "total_entities":
			int allEntities = 0;
			for (final World world : Bukkit.getWorlds()) {
				allEntities += world.getEntities().size();
			}
			return Integer.toString(allEntities);
		}

		if (identifier.startsWith("tps_")) {
			identifier = identifier.replace("tps_", "");
			return getTps(identifier);
		}

		if (identifier.startsWith("online_")) {

			identifier = identifier.replace("online_", "");

			int i = 0;

			for (Player o : Bukkit.getOnlinePlayers()) {
				if (o.getWorld().getName().equals(identifier)) {
					i = i + 1;
				}
			}
			return String.valueOf(i);
		}

		if (identifier.startsWith("countdown_")) {
			String time = identifier.replace("countdown_", "");

			if (time.indexOf("_") == -1) {

				Date then = null;

				try {
					then = PlaceholderAPIPlugin.getDateFormat().parse(time);
				} catch (Exception e) {
					return null;
				}

				Date now = new Date();

				long between = then.getTime() - now.getTime();

				if (between <= 0) {
					return "0";
				}

				return formatTime(Duration.of((int) TimeUnit.MILLISECONDS.toSeconds(between), ChronoUnit.SECONDS));

			} else {

				String[] parts = PlaceholderAPI.setBracketPlaceholders(p, time).split("_");

				if (parts.length != 2) {
					return "invalid format and time";
				}

				time = parts[1];

				String format = parts[0];

				SimpleDateFormat f = null;

				try {
					f = new SimpleDateFormat(format);
				} catch (Exception e) {
					return "invalid date format";
				}

				Date then = null;

				try {
					then = f.parse(time);
				} catch (Exception e) {
					return "invalid date";
				}

				long t = System.currentTimeMillis();

				long between = then.getTime() - t;

				if (between <= 0) {
					return "0";
				}

				return formatTime(Duration.of((int) TimeUnit.MILLISECONDS.toSeconds(between), ChronoUnit.SECONDS));

			}
		}

		if (identifier.startsWith("time_")) {

			identifier = identifier.replace("time_", "");

			if (dateFormats.containsKey(identifier)) {
				return dateFormats.get(identifier).format(new Date());
			}

			try {
				SimpleDateFormat format = new SimpleDateFormat(identifier);

				dateFormats.put(identifier, format);

				return format.format(new Date());
			} catch (NullPointerException | IllegalArgumentException ex) {
				return null;
			}
		}

		return null;
	}

	private double[] tps() {
		if (version == null || craftServer == null || tps == null) {
			return new double[] { 0, 0, 0 };
		}
		try {
			return ((double[]) tps.get(craftServer));
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return new double[] { 0, 0, 0 };
	}

	private double fix(double tps) {
		return Math.min(Math.round(tps * 100.0) / 100.0, 20.0);
	}

	private String color(double tps) {
		return ChatColor.translateAlternateColorCodes('&', (tps > 18.0) ? high : (tps > 16.0) ? medium : low)
				+ ((tps > 20.0) ? "*" : "") + fix(tps);
	}

	public String getTps(String arg) {
		if (arg == null || arg.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (double t : tps()) {
				sb.append(color(t));
				sb.append(", ");
			}
			return sb.toString();
		}
		switch (arg) {
		case "1":
		case "one":
			return String.valueOf(fix(tps()[0]));
		case "5":
		case "five":
			return String.valueOf(fix(tps()[1]));
		case "15":
		case "fifteen":
			return String.valueOf(tps()[2]);
		case "1_colored":
		case "one_colored":
			return color(tps()[0]);
		case "5_colored":
		case "five_colored":
			return color(tps()[1]);
		case "15_colored":
		case "fifteen_colored":
			return color(tps()[2]);
		}
		return null;
	}

	/**
	 * @author Sxtanna
	 */
	public static String formatTime(final Duration duration) {
		final StringBuilder builder = new StringBuilder();

		long seconds = duration.getSeconds();
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;
		final long weeks = days / 7;

		seconds %= 60;
		minutes %= 60;
		hours %= 24;
		days %= 7;

		if (seconds > 0) {
			builder.insert(0, seconds + "s");
		}

		if (minutes > 0) {
			if (builder.length() > 0) {
				builder.insert(0, ' ');
			}

			builder.insert(0, minutes + "m");
		}

		if (hours > 0) {
			if (builder.length() > 0) {
				builder.insert(0, ' ');
			}

			builder.insert(0, hours + "h");
		}

		if (days > 0) {
			if (builder.length() > 0) {
				builder.insert(0, ' ');
			}

			builder.insert(0, days + "d");
		}

		if (weeks > 0) {
			if (builder.length() > 0) {
				builder.insert(0, ' ');
			}

			builder.insert(0, weeks + "w");
		}

		return builder.toString();
	}

	/**
	 * Helper method to return the major version that the server is running.
	 *
	 * This is needed because in 1.17, NMS is no longer versioned.
	 *
	 * @return the major version of Minecraft the server is running
	 */
	public static int minecraftVersion() {
		try {
			final Matcher matcher = Pattern.compile("\\(MC: (\\d)\\.(\\d+)\\.?(\\d+?)?\\)").matcher(Bukkit.getVersion());
			if (matcher.find()) {
				return Integer.parseInt(matcher.toMatchResult().group(2), 10);
			} else {
				throw new IllegalArgumentException(String.format("No match found in '%s'", Bukkit.getVersion()));
			}
		} catch (final IllegalArgumentException ex) {
			throw new RuntimeException("Failed to determine Minecraft version", ex);
		}
	}

}
