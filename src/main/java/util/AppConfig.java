package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads settings from config.properties in the project root.
 * Falls back to config.properties on the classpath (inside out/) if present.
 */
public class AppConfig {

    private static final Properties props = new Properties();

    static {
        load();
    }

    private static void load() {
        Path rootConfig = Paths.get(System.getProperty("user.dir"), "config.properties");
        try (InputStream in = openConfigStream(rootConfig)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Warning: could not load config.properties - using defaults.");
        }
    }

    private static InputStream openConfigStream(Path rootConfig) throws IOException {
        if (Files.exists(rootConfig)) {
            return Files.newInputStream(rootConfig);
        }
        return AppConfig.class.getClassLoader().getResourceAsStream("config.properties");
    }

    public static String get(String key, String defaultValue) {
        String envKey = key.replace('.', '_').toUpperCase();
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
