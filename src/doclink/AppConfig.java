package doclink;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {

    private static final String CONFIG_FILE_NAME = "config.properties";
    public static final String CENTRAL_DB_URL_KEY = "doclink.central.db.url";

    // NEW: Sync Configuration Keys
    public static final String SYNC_ROLE_KEY = "doclink.sync.role"; // "Server", "Client", "Both"
    public static final String SYNC_INTERVAL_MINUTES_KEY = "doclink.sync.interval.minutes";
    public static final String CONFLICT_RESOLUTION_STRATEGY_KEY = "doclink.sync.conflict.strategy"; // "LastWriteWins", "ServerWins", "ClientWins"
    public static final String CHANGELOG_RETENTION_DAYS_KEY = "doclink.sync.changelog.retention.days";
    public static final String AUTO_SYNC_ENABLED_KEY = "doclink.sync.auto.enabled";
    public static final String COMPRESSION_ENABLED_KEY = "doclink.sync.compression.enabled";
    public static final String ENCRYPTION_ENABLED_KEY = "doclink.sync.encryption.enabled";
    public static final String HYBRID_SYNC_MODE_KEY = "doclink.sync.hybrid.mode"; // "P2P_ONLY", "CENTRAL_API_ONLY", "HYBRID"
    public static final String CENTRAL_API_URL_KEY = "doclink.central.api.url";
    public static final String CENTRAL_API_AUTH_TOKEN_KEY = "doclink.central.api.auth.token";
    public static final String LAST_CENTRAL_PULL_TIMESTAMP_KEY = "doclink.sync.last.central.pull.timestamp"; // NEW: Key for last central pull timestamp
    public static final String DELETE_LOCAL_ON_CENTRAL_PULL_KEY = "doclink.sync.delete.local.on.central.pull"; // NEW: Key for enabling/disabling local deletion on central pull

    // NEW: Communication Manager Keys
    public static final String SMTP_HOST_KEY = "doclink.comm.smtp.host";
    public static final String SMTP_PORT_KEY = "doclink.comm.smtp.port";
    public static final String SMTP_USERNAME_KEY = "doclink.comm.smtp.username";
    public static final String SMTP_PASSWORD_KEY = "doclink.comm.smtp.password";
    public static final String SENDER_EMAIL_KEY = "doclink.comm.sender.email";
    public static final String SMS_API_KEY = "doclink.comm.sms.api.key";
    public static final String SMS_SENDER_ID_KEY = "doclink.comm.sms.sender.id";
    public static final String AUTO_NOTIFICATIONS_ENABLED_KEY = "doclink.comm.auto.notifications.enabled";

    private static Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE_NAME)) {
            properties.load(fis);
            System.out.println("AppConfig: Loaded properties from " + CONFIG_FILE_NAME);
        } catch (IOException e) {
            System.out.println("AppConfig: " + CONFIG_FILE_NAME + " not found or could not be read. Using default settings.");
            // File might not exist yet, which is fine.
        }
    }

    private static void saveProperties() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE_NAME)) {
            properties.store(fos, "DocLink Application Configuration");
            System.out.println("AppConfig: Saved properties to " + CONFIG_FILE_NAME);
        } catch (IOException e) {
            System.err.println("AppConfig: Error saving properties to " + CONFIG_FILE_NAME + ": " + e.getMessage());
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveProperties();
    }

    public static void removeProperty(String key) {
        properties.remove(key);
        saveProperties();
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    public static void setBooleanProperty(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
        saveProperties();
    }

    public static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("AppConfig: Invalid integer format for key " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    public static void setIntProperty(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
        saveProperties();
    }
}