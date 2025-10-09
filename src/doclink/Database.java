package doclink;

import java.io.File;
import java.sql.*;

public class Database {
    private static final String DB_PATH = "data";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH + "/doclink.db";

    static {
        initializeDatabase();
    }

    // --- Initialize the database ---
    private static void initializeDatabase() {
        try {
            // Ensure data directory exists
            File dataDir = new File(DB_PATH);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                System.out.println("📁 Created data directory: " + dataDir.getAbsolutePath());
            }

            try (Connection conn = getConnection()) {
                if (conn != null) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("""
                            CREATE TABLE IF NOT EXISTS users (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                email TEXT UNIQUE NOT NULL,
                                password TEXT NOT NULL
                            )
                        """);
                        System.out.println("✅ Database initialized successfully.");
                    }
                    insertDemoUser(); // optional
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Database initialization failed:");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // --- Validate user login ---
    public static boolean validateUser(String email, String password) {
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, email.trim());
            ps.setString(2, password.trim());

            try (ResultSet rs = ps.executeQuery()) {
                boolean isValid = rs.next();
                if (!isValid) System.out.println("⚠️ Invalid login attempt for: " + email);
                return isValid;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- Add a demo user (for first run) ---
    public static void insertDemoUser() {
        String query = "INSERT OR IGNORE INTO users (email, password) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, "admin@gmail.com");
            ps.setString(2, "12345"); // plain text for demo (replace with hashing later)
            ps.executeUpdate();
            System.out.println("👤 Demo user inserted (admin@gmail.com / 12345)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
