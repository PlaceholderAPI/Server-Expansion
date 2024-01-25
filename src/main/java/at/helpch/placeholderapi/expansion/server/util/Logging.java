package at.helpch.placeholderapi.expansion.server.util;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.logging.Level;

public final class Logging {

    private static void log(@NotNull final Level level, @Nullable final Throwable throwable, @NotNull final String message, @NotNull final Object... args) {
        PlaceholderAPIPlugin.getInstance()
            .getLogger()
            .log(level, "[server] " + MessageFormat.format(message, args), throwable);
    }

    public static void warn(@NotNull final String message, @NotNull final Object... args) {
        log(Level.WARNING, null, message, args);
    }

    public static void error(@NotNull final Throwable throwable, @NotNull final String message, @NotNull final Object... args) {
        log(Level.SEVERE, throwable, message, args);
    }

}
