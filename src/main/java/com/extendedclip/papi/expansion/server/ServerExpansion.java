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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ServerExpansion extends PlaceholderExpansion implements Cacheable, Configurable {
	
	private ServerUtils serverUtils = null;
	
	private final Map<String, SimpleDateFormat> dateFormats = new HashMap<>();
	private final Runtime runtime = Runtime.getRuntime();

	// config stuff
	private String serverName;
	private String low = "&c";
	private String medium = "&e";
	private String high = "&a";
	// -----
	
	private final Cache<String, Integer> cache = CacheBuilder.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build();

	private final String VERSION = getClass().getPackage().getImplementationVersion();

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
		dateFormats.clear();
		serverUtils = null;
		cache.invalidateAll();
	}

	@Override
	public @NotNull String getIdentifier() {
		return "server";
	}

	@Override
	public @NotNull String getAuthor() {
		return "clip";
	}

	@Override
	public @NotNull String getVersion() {
		return VERSION;
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

	private @Nullable String getCached(String key, Callable<Integer> callable) {
		try {
			return String.valueOf(cache.get(key, callable));
		} catch (ExecutionException e) {
			if (getPlaceholderAPI().getPlaceholderAPIConfig().isDebugMode()) {
				getPlaceholderAPI().getLogger().log(Level.SEVERE, "[server] Could not access cache key " + key, e);
			}
			return "";
		}
	}

	private int getChunks(){
		return Bukkit.getWorlds()
				.stream()
				.mapToInt(world -> world.getLoadedChunks().length)
				.sum();
	}

	private int getLivingEntities(){
		return Bukkit.getWorlds()
				.stream()
				.mapToInt(world -> world.getLivingEntities().size())
				.sum();
	}

	private Integer getTotalEntities(){
		return Bukkit.getWorlds()
				.stream()
				.mapToInt(world -> world.getEntities().size())
				.sum();
	}

	@Override
	public String onRequest(OfflinePlayer p, @NotNull String identifier) {
		final int MB = 1048576;
		if (serverUtils == null) {
			serverUtils = new ServerUtils();
		}

		switch (identifier) {
			// Players placeholders
			case "online":
				return String.valueOf(Bukkit.getOnlinePlayers().size());
			case "max_players":
				return String.valueOf(Bukkit.getMaxPlayers());
			case "unique_joins":
				return String.valueOf(Bukkit.getOfflinePlayers().length);
			// -----

			// Version placeholders
			case "version":
				return serverUtils.getVersion();
			case "build":
				return serverUtils.getBuild();
			case "version_build":
			case "version_full":
				return serverUtils.getVersion() + '-' + serverUtils.getBuild();
			// -----

			// Ram placeholders
			case "ram_used":
				return String.valueOf((runtime.totalMemory() - runtime.freeMemory()) / MB);
			case "ram_free":
				return String.valueOf(runtime.freeMemory() / MB);
			case "ram_total":
				return String.valueOf(runtime.totalMemory() / MB);
			case "ram_max":
				return String.valueOf(runtime.maxMemory() / MB);
			// -----

			// Identity placeholders
			case "name":
				return serverName == null ? "" : serverName;
			case "variant":
				return serverUtils.getServerVariant();
			// -----

			// Other placeholders
			case "tps":
				return getTps(null);
			case "uptime":
				long seconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
				return formatTime(Duration.of(seconds, ChronoUnit.SECONDS));
			case "total_chunks":
				return getCached("chunks", this::getChunks);
			case "total_living_entities":
				return getCached("livingEntities", this::getLivingEntities);
			case "total_entities":
				return getCached("totalEntities", this::getTotalEntities);
			case "has_whitelist":
				return Bukkit.getServer().hasWhitelist() ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
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

			if (!time.contains("_")) {

				Date then;

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

				SimpleDateFormat f;

				try {
					f = new SimpleDateFormat(format);
				} catch (Exception e) {
					return "invalid date format";
				}

				Date then;

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

	public String getTps(String arg) {
		if (arg == null || arg.isEmpty()) {
			StringJoiner joiner = new StringJoiner(ChatColor.GRAY + ", ");
			for (double tps : serverUtils.getTps()) {
				joiner.add(getColoredTps(tps));
			}
			return joiner.toString();
		}
		switch (arg) {
			case "1":
			case "one": 
				return fix(serverUtils.getTps()[0]);
			case "5":
			case "five":
				return fix(serverUtils.getTps()[1]);
			case "15":
			case "fifteen":
				return fix(serverUtils.getTps()[2]);
			case "1_colored":
			case "one_colored":
				return getColoredTps(serverUtils.getTps()[0]);
			case "5_colored":
			case "five_colored":
				return getColoredTps(serverUtils.getTps()[1]);
			case "15_colored":
			case "fifteen_colored":
				return getColoredTps(serverUtils.getTps()[2]);
			case "percent": {
				final StringJoiner joiner = new StringJoiner(ChatColor.GRAY + ", ");

				for (double t : serverUtils.getTps()) {
					joiner.add(getColoredTpsPercent(t));
				}

				return joiner.toString();
			}
			case "1_percent":
			case "one_percent":
				return getPercent(serverUtils.getTps()[0]);
			case "5_percent":
			case "five_percent":
				return getPercent(serverUtils.getTps()[1]);
			case "15_percent":
			case "fifteen_percent":
				return getPercent(serverUtils.getTps()[2]);
			case "1_percent_colored":
			case "one_percent_colored":
				return getColoredTpsPercent(serverUtils.getTps()[0]);
			case "5_percent_colored":
			case "five_percent_colored":
				return getColoredTpsPercent(serverUtils.getTps()[1]);
			case "15_percent_colored":
			case "fifteen_percent_colored":
				return getColoredTpsPercent(serverUtils.getTps()[2]);
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
	
	private String fix(double tps) {
		double finalTps = Math.min(Math.round(tps), 20.0);
		
		return (tps > 20.0 ? "*" : "") + finalTps;
	}
	
	private String color(double tps) {
		return ChatColor.translateAlternateColorCodes('&', (tps > 18.0) ? high : (tps > 16.0) ? medium : low);
	}
	
	private String getColoredTps(double tps) {
		return color(tps) + fix(tps);
	}
	
	private String getColoredTpsPercent(double tps){
		return color(tps) + getPercent(tps);
	}
	
	private String getPercent(double tps){
		double finalPercent = Math.min(Math.round(100 / 20.0 * tps), 100.0);
		
		return (tps > 20.0 ? "*" : "") + finalPercent + "%";
	}

}
