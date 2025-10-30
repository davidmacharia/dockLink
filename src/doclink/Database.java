package doclink;

import doclink.models.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class Database {
    private static final String SQLITE_DB_URL = "jdbc:sqlite:data/doclink.db";

    private static Connection activeConnection = null;
    private static DatabaseType currentDatabaseType = DatabaseType.SQLITE; // Default to SQLite

    public enum DatabaseType {
        SQLITE, MYSQL
    }

    private Database() {}

    private static Connection createConnection(String url) throws SQLException {
        System.out.println("Attempting to connect to: " + url);
        // Load MySQL driver explicitly if URL indicates MySQL
        if (url.startsWith("jdbc:mysql")) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC Driver not found. Please ensure mysql-connector-java.jar is in your classpath.");
                throw new SQLException("MySQL JDBC Driver not found.", e);
            }
        }
        return DriverManager.getConnection(url);
    }

    public static void initializeDatabase() {
        String centralDbUrl = AppConfig.getProperty(AppConfig.CENTRAL_DB_URL_KEY);

        try {
            // Always initialize local SQLite as the active connection
            activeConnection = createConnection(SQLITE_DB_URL);
            currentDatabaseType = DatabaseType.SQLITE;
            System.out.println("Successfully connected to local SQLite database as primary.");

            try (Statement stmt = activeConnection.createStatement()) {
                // Users Table
                String userTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                                   "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                   "name TEXT NOT NULL," +
                                   "email TEXT UNIQUE NOT NULL," +
                                   "password TEXT NOT NULL," +
                                   "role TEXT NOT NULL," +
                                   "contact TEXT)";
                stmt.execute(userTableSql);

                if (!columnExists(activeConnection, "users", "contact")) {
                    stmt.execute("ALTER TABLE users ADD COLUMN contact TEXT");
                }

                // Plans Table
                String planTableSql = "CREATE TABLE IF NOT EXISTS plans (" +
                                   "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                   "applicant_name TEXT NOT NULL," +
                                   "contact TEXT NOT NULL," +
                                   "plot_no TEXT NOT NULL," +
                                   "location TEXT NOT NULL," +
                                   "date_submitted TEXT NOT NULL," +
                                   "reference_no TEXT UNIQUE," +
                                   "status TEXT NOT NULL," +
                                   "remarks TEXT)";
                stmt.execute(planTableSql);

                // Documents Table
                String docTableSql = "CREATE TABLE IF NOT EXISTS documents (" +
                                  "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                  "plan_id INTEGER NOT NULL," +
                                  "doc_name TEXT NOT NULL," +
                                  "file_path TEXT," +
                                  "is_attached BOOLEAN NOT NULL," +
                                  "document_type TEXT NOT NULL DEFAULT 'Submitted'," +
                                  "FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE)";
                stmt.execute(docTableSql);

                if (!columnExists(activeConnection, "documents", "document_type")) {
                    stmt.execute("ALTER TABLE documents ADD COLUMN document_type TEXT NOT NULL DEFAULT 'Submitted'");
                }

                // Billing Table
                String billingTableSql = "CREATE TABLE IF NOT EXISTS billing (" +
                                      "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                      "plan_id INTEGER NOT NULL," +
                                      "amount REAL NOT NULL," +
                                      "receipt_no TEXT UNIQUE," +
                                      "date_paid TEXT," +
                                      "FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE)";
                stmt.execute(billingTableSql);

                // Logs Table
                String logTableSql = "CREATE TABLE IF NOT EXISTS logs (" +
                                  "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                  "plan_id INTEGER NOT NULL," +
                                  "from_role TEXT NOT NULL," +
                                  "to_role TEXT NOT NULL," +
                                  "action TEXT NOT NULL," +
                                  "remarks TEXT," +
                                  "date TEXT NOT NULL," +
                                  "FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE)";
                stmt.execute(logTableSql);

                // Meetings Table
                String meetingTableSql = "CREATE TABLE IF NOT EXISTS meetings (" +
                                      "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                      "title TEXT NOT NULL," +
                                      "date TEXT NOT NULL," +
                                      "time TEXT NOT NULL," +
                                      "location TEXT NOT NULL," +
                                      "agenda TEXT," +
                                      "status TEXT NOT NULL)";
                stmt.execute(meetingTableSql);

                // Document Checklist Items Table
                String checklistTableSql = "CREATE TABLE IF NOT EXISTS document_checklist_items (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "item_name TEXT UNIQUE NOT NULL," +
                                        "is_required BOOLEAN NOT NULL DEFAULT 1," +
                                        "requires_file_upload BOOLEAN NOT NULL DEFAULT 0)";
                stmt.execute(checklistTableSql);

                if (!columnExists(activeConnection, "document_checklist_items", "requires_file_upload")) {
                    stmt.execute("ALTER TABLE document_checklist_items ADD COLUMN requires_file_upload BOOLEAN NOT NULL DEFAULT 0");
                }

                // Peers Table
                String peersTableSql = "CREATE TABLE IF NOT EXISTS peers (" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "ip_address TEXT NOT NULL," +
                                    "port INTEGER NOT NULL," +
                                    "last_sync_time TEXT," +
                                    "is_trusted BOOLEAN NOT NULL DEFAULT 0," +
                                    "status TEXT NOT NULL DEFAULT 'Unknown'," +
                                    "UNIQUE(ip_address, port))";
                stmt.execute(peersTableSql);

                // System Updates Table
                String systemUpdatesTableSql = "CREATE TABLE IF NOT EXISTS system_updates (" +
                                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                            "version TEXT," +
                                            "title TEXT NOT NULL," +
                                            "message TEXT NOT NULL," +
                                            "created_at TEXT NOT NULL)";
                stmt.execute(systemUpdatesTableSql);

                // Message Templates Table
                String messageTemplatesTableSql = "CREATE TABLE IF NOT EXISTS message_templates (" +
                                               "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                               "template_name TEXT UNIQUE NOT NULL," +
                                               "subject TEXT," +
                                               "body TEXT NOT NULL," +
                                               "type TEXT NOT NULL)";
                stmt.execute(messageTemplatesTableSql);

                // Message Logs Table
                String messageLogsTableSql = "CREATE TABLE IF NOT EXISTS message_logs (" +
                                          "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                          "timestamp TEXT NOT NULL," +
                                          "recipient TEXT NOT NULL," +
                                          "message_type TEXT NOT NULL," +
                                          "subject TEXT," +
                                          "message TEXT NOT NULL," +
                                          "status TEXT NOT NULL," +
                                          "details TEXT)";
                stmt.execute(messageLogsTableSql);

                // User Preferences Table
                String userPreferencesTableSql = "CREATE TABLE IF NOT EXISTS user_preferences (" +
                                              "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                              "user_id INTEGER UNIQUE NOT NULL," +
                                              "email_notifications_enabled BOOLEAN NOT NULL DEFAULT 1," +
                                              "sms_notifications_enabled BOOLEAN NOT NULL DEFAULT 1," +
                                              "last_seen_update_id INTEGER NOT NULL DEFAULT 0," +
                                              "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
                stmt.execute(userPreferencesTableSql);

                // Add new column if it doesn't exist
                if (!columnExists(activeConnection, "user_preferences", "last_seen_update_id")) {
                    stmt.execute("ALTER TABLE user_preferences ADD COLUMN last_seen_update_id INTEGER NOT NULL DEFAULT 0");
                }

                // NEW: Changelog Table for local-to-central sync
                String changelogTableSql = "CREATE TABLE IF NOT EXISTS changelog (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "table_name TEXT NOT NULL," +
                                        "record_id INTEGER NOT NULL," +
                                        "change_type TEXT NOT NULL," +
                                        "column_name TEXT," +
                                        "old_value TEXT," +
                                        "new_value TEXT," +
                                        "timestamp TEXT NOT NULL," +
                                        "is_synced BOOLEAN NOT NULL DEFAULT 0)";
                stmt.execute(changelogTableSql);


                System.out.println("Database schema initialized successfully for local SQLite.");
            }

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Fatal Error: Could not initialize database. Application will exit.", "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        addDemoChecklistItems();
        addDemoMessageTemplates();
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getColumns(null, null, tableName, columnName);
        return rs.next();
    }

    public static Connection getActiveConnection() {
        if (activeConnection == null) {
            System.err.println("Warning: Attempted to get active connection before initialization. Initializing now.");
            initializeDatabase();
        }
        return activeConnection;
    }

    // NEW: Method to get a connection to the central database specifically for sync
    public static Connection getCentralConnection() throws SQLException {
        String centralDbUrl = AppConfig.getProperty(AppConfig.CENTRAL_DB_URL_KEY);
        if (centralDbUrl == null || centralDbUrl.trim().isEmpty()) {
            throw new SQLException("Central DB URL is not configured.");
        }
        // Determine central DB type for driver loading
        DatabaseType centralDbType = DatabaseType.SQLITE; // Default to SQLite if not specified
        if (centralDbUrl.startsWith("jdbc:mysql")) {
            centralDbType = DatabaseType.MYSQL;
        }
        System.out.println("Attempting to get central connection to: " + centralDbUrl + " (Type: " + centralDbType + ")");
        return createConnection(centralDbUrl);
    }

    public static DatabaseType getCurrentDatabaseType() {
        return currentDatabaseType;
    }

    public static boolean testConnection(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try (Connection testConn = createConnection(url)) { // Use createConnection to handle driver loading
            return true;
        } catch (SQLException e) {
            System.err.println("Test connection failed for URL: " + url + " - " + e.getMessage());
            return false;
        }
    }

    public static void closeConnection() {
        if (activeConnection != null) {
            try {
                activeConnection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }

    public static void addDemoUsers() {
        String sql = "INSERT OR IGNORE INTO users (name, email, password, role, contact) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            String[][] users = {
                {"Receptionist", "reception@doclink.com", "password", "Reception", "+11234567890"},
                {"Planning Officer", "planning@doclink.com", "password", "Planning", "+11234567891"},
                {"Committee Member", "committee@doclink.com", "password", "Committee", "+11234567892"},
                {"Director", "director@doclink.com", "password", "Director", "+11234567893"},
                {"Structural Engineer", "structural@doclink.com", "password", "Structural", "+11234567894"},
                {"Client User", "client@doclink.com", "password", "Client", "+11234567895"},
                {"Admin User", "admin@doclink.com", "password", "Admin", "+11234567896"},
                {"Developer User", "developer@doclink.com", "password", "Developer", "+11234567897"}
            };

            for (String[] user : users) {
                pstmt.setString(1, user[0]);
                pstmt.setString(2, user[1]);
                pstmt.setString(3, user[2]);
                pstmt.setString(4, user[3]);
                pstmt.setString(5, user[4]);
                pstmt.executeUpdate();
            }
            System.out.println("Demo users added/ensured.");
        } catch (SQLException e) {
            System.err.println("Error adding demo users: " + e.getMessage());
        }
    }

    public static void addDemoChecklistItems() {
        String sql = "INSERT OR IGNORE INTO document_checklist_items (item_name, is_required, requires_file_upload) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {

            Object[][] items = {
                {"Site Plan", true, true},
                {"Title Deed", true, true},
                {"Architectural Drawings", true, true},
                {"Structural Drawings", false, true},
                {"Environmental Impact Assessment", false, false},
                {"Fire Safety Report", false, false}
            };

            for (Object[] item : items) {
                pstmt.setString(1, (String) item[0]);
                pstmt.setBoolean(2, (Boolean) item[1]);
                pstmt.setBoolean(3, (Boolean) item[2]);
                pstmt.executeUpdate();
            }
            System.out.println("Demo checklist items added/ensured.");
        } catch (SQLException e) {
            System.err.println("Error adding demo checklist items: " + e.getMessage());
        }
    }

    public static void addDemoMessageTemplates() {
        String sql = "INSERT OR IGNORE INTO message_templates (template_name, subject, body, type) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {

            Object[][] templates = {
                {"Client_NewApplicationSubmitted_Email", "Your Plan Application Submitted - Ref: {referenceNo}", "Dear {clientName},\n\nYour building plan application (Ref: {referenceNo}) for Plot No: {plotNo} has been successfully submitted on {dateSubmitted}. We will notify you of its progress.\n\nRegards,\nDocLink Team", "EMAIL"},
                {"Client_ApplicationApproved_Email", "Your Plan Application Approved! - Ref: {referenceNo}", "Dear {clientName},\n\nGreat news! Your building plan application (Ref: {referenceNo}) for Plot No: {plotNo} has been APPROVED. You can pick up your certificate at the reception.\n\nRegards,\nDocLink Team", "EMAIL"},
                {"Client_ApplicationRejected_Email", "Update on Your Plan Application - Ref: {referenceNo}", "Dear {clientName},\n\nYour building plan application (Ref: {referenceNo}) for Plot No: {plotNo} has been REJECTED. Please review the remarks: {remarks} and resubmit if necessary.\n\nRegards,\nDocLink Team", "EMAIL"},
                {"Client_StatusUpdate_SMS", null, "DocLink: Your plan {referenceNo} status updated to {status}. Remarks: {remarks}", "SMS"},
                {"Committee_MeetingScheduled_Email", "New Committee Meeting Scheduled: {meetingTitle}", "Dear Committee Member,\n\nA new meeting titled '{meetingTitle}' has been scheduled for {meetingDate} at {meetingTime} in {meetingLocation}. Agenda: {meetingAgenda}\n\nRegards,\nDocLink Team", "EMAIL"},
                {"Committee_MeetingReminder_SMS", null, "Reminder: Committee Meeting '{meetingTitle}' in 1 hour at {meetingLocation}.", "SMS"},
                {"System_Update_AllUsers_Email", "Important System Update: {version} - {title}", "Dear User,\n\nWe have an important system update for DocLink (Version: {version}).\n\nTitle: {title}\nMessage: {message}\n\nThank you,\nDocLink Team", "EMAIL"}
            };

            for (Object[] template : templates) {
                pstmt.setString(1, (String) template[0]);
                pstmt.setString(2, (String) template[1]);
                pstmt.setString(3, (String) template[2]);
                pstmt.setString(4, (String) template[3]);
                pstmt.executeUpdate();
            }
            System.out.println("Demo message templates added/ensured.");
        } catch (SQLException e) {
            System.err.println("Error adding demo message templates: " + e.getMessage());
        }
    }


    public static User authenticateUser(String email, String password) {
        String sql = "SELECT id, name, email, contact, role FROM users WHERE email = ? AND password = ?";
        Connection conn = getActiveConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                System.out.println("Database: Authenticated user role: '" + role + "'");
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("contact"), role);
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return null;
    }

    public static void addPlan(Plan plan, List<Document> documents) {
        String planSql = "INSERT INTO plans (applicant_name, contact, plot_no, location, date_submitted, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String docSql = "INSERT INTO documents (plan_id, doc_name, file_path, is_attached, document_type) VALUES (?, ?, ?, ?, ?)";
        Connection conn = getActiveConnection();
        try (PreparedStatement planPstmt = conn.prepareStatement(planSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement docPstmt = conn.prepareStatement(docSql)) {

            conn.setAutoCommit(false);

            planPstmt.setString(1, plan.getApplicantName());
            planPstmt.setString(2, plan.getContact());
            planPstmt.setString(3, plan.getPlotNo());
            planPstmt.setString(4, plan.getLocation());
            planPstmt.setObject(5, plan.getDateSubmitted()); // Use setObject for LocalDate
            planPstmt.setString(6, plan.getStatus());
            planPstmt.setString(7, plan.getRemarks());
            planPstmt.executeUpdate();

            ResultSet rs = planPstmt.getGeneratedKeys();
            if (rs.next()) {
                int planId = rs.getInt(1);
                plan.setId(planId);

                // NEW: Log plan insertion if using SQLite
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("plans", planId, "INSERT"));
                }

                for (Document doc : documents) {
                    docPstmt.setInt(1, planId);
                    docPstmt.setString(2, doc.getDocName());
                    docPstmt.setString(3, doc.getFilePath());
                    docPstmt.setBoolean(4, doc.isAttached());
                    docPstmt.setString(5, doc.getDocumentType());
                    docPstmt.addBatch();
                }
                docPstmt.executeBatch();
            }
            conn.commit();
            System.out.println("Plan and documents added successfully.");
        } catch (SQLException e) {
            System.err.println("Error adding plan and documents: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Rollback error: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error resetting auto-commit: " + ex.getMessage());
            }
        }
    }

    public static void addDocument(Document doc) {
        String sql = "INSERT INTO documents (plan_id, doc_name, file_path, is_attached, document_type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, doc.getPlanId());
            pstmt.setString(2, doc.getDocName());
            pstmt.setString(3, doc.getFilePath());
            pstmt.setBoolean(4, doc.isAttached());
            pstmt.setString(5, doc.getDocumentType());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                doc.setId(rs.getInt(1));
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("documents", doc.getId(), "INSERT"));
                }
            }
            System.out.println("Document '" + doc.getDocName() + "' added for plan " + doc.getPlanId());
        } catch (SQLException e) {
            System.err.println("Error adding document: " + e.getMessage());
        }
    }

    public static List<Document> getDocumentsByPlanId(int planId) {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT id, plan_id, doc_name, file_path, is_attached, document_type FROM documents WHERE plan_id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, planId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                documents.add(new Document(
                    rs.getInt("id"),
                    rs.getInt("plan_id"),
                    rs.getString("doc_name"),
                    rs.getString("file_path"),
                    rs.getBoolean("is_attached"),
                    rs.getString("document_type")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting documents by plan ID: " + e.getMessage());
        }
        return documents;
    }

    public static void updatePlanStatus(int planId, String newStatus, String remarks) {
        // Fetch old status and remarks for changelog
        Plan oldPlan = getPlanById(planId);
        String oldStatus = oldPlan != null ? oldPlan.getStatus() : null;
        String oldRemarks = oldPlan != null ? oldPlan.getRemarks() : null;

        String sql = "UPDATE plans SET status = ?, remarks = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, remarks);
            pstmt.setInt(3, planId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Plan " + planId + " status updated to " + newStatus);

            // NEW: Log plan update if using SQLite and values changed
            if (currentDatabaseType == DatabaseType.SQLITE && rowsAffected > 0) {
                if (oldStatus != null && !oldStatus.equals(newStatus)) {
                    addChangelogEntry(new ChangelogEntry("plans", planId, "UPDATE", "status", oldStatus, newStatus));
                }
                if (oldRemarks != null && !oldRemarks.equals(remarks)) {
                    addChangelogEntry(new ChangelogEntry("plans", planId, "UPDATE", "remarks", oldRemarks, remarks));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating plan status: " + e.getMessage());
        }
    }

    public static void addLog(Log log) {
        String sql = "INSERT INTO logs (plan_id, from_role, to_role, action, remarks, date) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, log.getPlanId());
            pstmt.setString(2, log.getFromRole());
            pstmt.setString(3, log.getToRole());
            pstmt.setString(4, log.getAction());
            pstmt.setString(5, log.getRemarks());
            pstmt.setObject(6, log.getDate()); // Use setObject for LocalDate
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int logId = rs.getInt(1);
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("logs", logId, "INSERT"));
                }
            }
            System.out.println("Log added for plan " + log.getPlanId());
        } catch (SQLException e) {
            System.err.println("Error adding log: " + e.getMessage());
        }
    }

    public static List<Log> getRecentLogs(int limit) {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT id, plan_id, from_role, to_role, action, remarks, date FROM logs ORDER BY date DESC, id DESC LIMIT ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(new Log(
                    rs.getInt("id"),
                    rs.getInt("plan_id"),
                    rs.getString("from_role"),
                    rs.getString("to_role"),
                    rs.getString("action"),
                    rs.getString("remarks"),
                    rs.getObject("date", LocalDate.class) // Use getObject for LocalDate
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting recent logs: " + e.getMessage());
        }
        return logs;
    }

    public static List<Log> getAllLogs() {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT id, plan_id, from_role, to_role, action, remarks, date FROM logs ORDER BY date DESC, id DESC";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new Log(
                    rs.getInt("id"),
                    rs.getInt("plan_id"),
                    rs.getString("from_role"),
                    rs.getString("to_role"),
                    rs.getString("action"),
                    rs.getString("remarks"),
                    rs.getObject("date", LocalDate.class) // Use getObject for LocalDate
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all logs: " + e.getMessage());
        }
        return logs;
    }

    public static List<Log> getLogsByPlanId(int planId) {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT id, plan_id, from_role, to_role, action, remarks, date FROM logs WHERE plan_id = ? ORDER BY date DESC, id DESC";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, planId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(new Log(
                    rs.getInt("id"),
                    rs.getInt("plan_id"),
                    rs.getString("from_role"),
                    rs.getString("to_role"),
                    rs.getString("action"),
                    rs.getString("remarks"),
                    rs.getObject("date", LocalDate.class) // Use getObject for LocalDate
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting logs by plan ID: " + e.getMessage());
        }
        return logs;
    }

    public static List<Plan> getPlansByStatus(String status) {
        List<Plan> plans = new ArrayList<>();
        String sql = "SELECT * FROM plans WHERE status = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                plans.add(new Plan(
                    rs.getInt("id"),
                    rs.getString("applicant_name"),
                    rs.getString("contact"),
                    rs.getString("plot_no"),
                    rs.getString("location"),
                    rs.getObject("date_submitted", LocalDate.class), // Use getObject for LocalDate
                    rs.getString("reference_no"),
                    rs.getString("status"),
                    rs.getString("remarks")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting plans by status: " + e.getMessage());
        }
        return plans;
    }

    public static List<Plan> getAllPlans() {
        List<Plan> plans = new ArrayList<>();
        String sql = "SELECT * FROM plans ORDER BY date_submitted DESC";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                plans.add(new Plan(
                    rs.getInt("id"),
                    rs.getString("applicant_name"),
                    rs.getString("contact"),
                    rs.getString("plot_no"),
                    rs.getString("location"),
                    rs.getObject("date_submitted", LocalDate.class), // Use getObject for LocalDate
                    rs.getString("reference_no"),
                    rs.getString("status"),
                    rs.getString("remarks")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all plans: " + e.getMessage());
        }
        return plans;
    }

    public static Plan getPlanById(int planId) {
        String sql = "SELECT * FROM plans WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, planId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Plan(
                    rs.getInt("id"),
                    rs.getString("applicant_name"),
                    rs.getString("contact"),
                    rs.getString("plot_no"),
                    rs.getString("location"),
                    rs.getObject("date_submitted", LocalDate.class), // Use getObject for LocalDate
                    rs.getString("reference_no"),
                    rs.getString("status"),
                    rs.getString("remarks")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting plan by ID: " + e.getMessage());
        }
        return null;
    }

    public static boolean updatePlanReferenceNo(int planId, String referenceNo) {
        // Fetch old reference number for changelog
        Plan oldPlan = getPlanById(planId);
        String oldReferenceNo = oldPlan != null ? oldPlan.getReferenceNo() : null;

        String sql = "UPDATE plans SET reference_no = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, referenceNo);
            pstmt.setInt(2, planId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Database: updatePlanReferenceNo - Plan " + planId + " reference number updated to " + referenceNo + ". Rows affected: " + rowsAffected);
            
            // NEW: Log plan update if using SQLite and value changed
            if (currentDatabaseType == DatabaseType.SQLITE && rowsAffected > 0) {
                if (oldReferenceNo != null && !oldReferenceNo.equals(referenceNo)) {
                    addChangelogEntry(new ChangelogEntry("plans", planId, "UPDATE", "reference_no", oldReferenceNo, referenceNo));
                }
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Database: Error updating plan reference number: " + e.getMessage());
            // Check for unique constraint violation message specific to database type
            if (e.getMessage().contains("UNIQUE constraint failed: plans.reference_no") || e.getSQLState().equals("23000")) { // SQLite or MySQL
                return false;
            }
            return false;
        }
    }

    public static void addBilling(Billing billing) {
        String sql = "INSERT INTO billing (plan_id, amount) VALUES (?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, billing.getPlanId());
            pstmt.setDouble(2, billing.getAmount());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                billing.setId(rs.getInt(1));
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("billing", billing.getId(), "INSERT"));
                }
            }
            System.out.println("Billing added for plan " + billing.getPlanId());
        } catch (SQLException e) {
            System.err.println("Error adding billing: " + e.getMessage());
        }
    }

    public static Billing getBillingByPlanId(int planId) {
        String sql = "SELECT * FROM billing WHERE plan_id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, planId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Billing(
                    rs.getInt("id"),
                    rs.getInt("plan_id"),
                    rs.getDouble("amount"),
                    rs.getString("receipt_no"),
                    rs.getObject("date_paid", LocalDate.class) // Use getObject for LocalDate
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting billing by plan ID: " + e.getMessage());
        }
        return null;
    }

    public static boolean updateBillingPayment(int billingId, String receiptNo) {
        // Fetch old billing for changelog
        // Note: getBillingByPlanId assumes planId, but updateBillingPayment uses billingId.
        // A proper getBillingById method would be better here. For now, we'll fetch all and find.
        Billing oldBilling = null;
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM billing WHERE id = " + billingId)) {
            if (rs.next()) {
                oldBilling = new Billing(
                    rs.getInt("id"),
                    rs.getInt("plan_id"),
                    rs.getDouble("amount"),
                    rs.getString("receipt_no"),
                    rs.getObject("date_paid", LocalDate.class)
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching old billing for changelog: " + e.getMessage());
        }

        String oldReceiptNo = oldBilling != null ? oldBilling.getReceiptNo() : null;
        LocalDate oldDatePaid = oldBilling != null ? oldBilling.getDatePaid() : null;

        String sql = "UPDATE billing SET receipt_no = ?, date_paid = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, receiptNo);
            pstmt.setObject(2, LocalDate.now()); // Use setObject for LocalDate
            pstmt.setInt(3, billingId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Billing " + billingId + " marked as paid with receipt " + receiptNo);

            if (currentDatabaseType == DatabaseType.SQLITE && rowsAffected > 0) {
                if (oldReceiptNo != null && !oldReceiptNo.equals(receiptNo)) {
                    addChangelogEntry(new ChangelogEntry("billing", billingId, "UPDATE", "receipt_no", oldReceiptNo, receiptNo));
                }
                if (oldDatePaid != null && !oldDatePaid.equals(LocalDate.now())) {
                    addChangelogEntry(new ChangelogEntry("billing", billingId, "UPDATE", "date_paid", oldDatePaid.toString(), LocalDate.now().toString()));
                }
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating billing payment: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: billing.receipt_no") || e.getSQLState().equals("23000")) {
                return false;
            }
            return false;
        }
    }

    public static List<Plan> getPlansByApplicantEmailAndStatus(String email, String status) {
        List<Plan> plans = new ArrayList<>();
        String sql = "SELECT p.* FROM plans p JOIN users u ON p.applicant_name = u.name WHERE u.email = ? AND p.status = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, status);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                plans.add(new Plan(
                    rs.getInt("id"),
                    rs.getString("applicant_name"),
                    rs.getString("contact"),
                    rs.getString("plot_no"),
                    rs.getString("location"),
                    rs.getObject("date_submitted", LocalDate.class),
                    rs.getString("reference_no"),
                    rs.getString("status"),
                    rs.getString("remarks")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting plans by applicant email and status: " + e.getMessage());
        }
        return plans;
    }

    public static List<Plan> getPlansByApplicantEmail(String email) {
        List<Plan> plans = new ArrayList<>();
        String sql = "SELECT p.* FROM plans p JOIN users u ON p.applicant_name = u.name WHERE u.email = ? ORDER BY p.date_submitted DESC";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                plans.add(new Plan(
                    rs.getInt("id"),
                    rs.getString("applicant_name"),
                    rs.getString("contact"),
                    rs.getString("plot_no"),
                    rs.getString("location"),
                    rs.getObject("date_submitted", LocalDate.class),
                    rs.getString("reference_no"),
                    rs.getString("status"),
                    rs.getString("remarks")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting plans by applicant email: " + e.getMessage());
        }
        return plans;
    }

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, contact, role FROM users ORDER BY name";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("contact"),
                    rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
        }
        return users;
    }

    // NEW: Method to get all users excluding the 'Developer' role
    public static List<User> getAllNonDeveloperUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, contact, role FROM users WHERE role != 'Developer' ORDER BY name";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("contact"),
                    rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting non-developer users: " + e.getMessage());
        }
        return users;
    }

    // NEW: Method to get users by role
    public static List<User> getUsersByRole(String role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, contact, role FROM users WHERE role = ? ORDER BY name";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("contact"),
                    rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting users by role: " + e.getMessage());
        }
        return users;
    }

    public static User getUserById(int userId) {
        String sql = "SELECT id, name, email, contact, role FROM users WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("contact"), rs.getString("role"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
        }
        return null;
    }

    public static User getUserByEmail(String email) {
        String sql = "SELECT id, name, email, contact, role FROM users WHERE email = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("contact"), rs.getString("role"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
        }
        return null;
    }

    public static boolean addUser(String name, String email, String password, String role) {
        return addUser(name, email, password, role, "");
    }

    public static boolean addUser(String name, String email, String password, String role, String contact) {
        String sql = "INSERT INTO users (name, email, password, role, contact) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            pstmt.setString(5, contact);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int userId = rs.getInt(1);
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("users", userId, "INSERT"));
                }
            }
            System.out.println("User '" + name + "' added successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("users", userId, "DELETE"));
                }
                System.out.println("User " + userId + " deleted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
        return false;
    }

    public static boolean updateUserNameAndEmail(int userId, String newName, String newEmail) {
        User oldUser = getUserById(userId);
        String oldName = oldUser != null ? oldUser.getName() : null;
        String oldEmail = oldUser != null ? oldUser.getEmail() : null;

        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, newEmail);
            pstmt.setInt(3, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    if (oldName != null && !oldName.equals(newName)) {
                        addChangelogEntry(new ChangelogEntry("users", userId, "UPDATE", "name", oldName, newName));
                    }
                    if (oldEmail != null && !oldEmail.equals(newEmail)) {
                        addChangelogEntry(new ChangelogEntry("users", userId, "UPDATE", "email", oldEmail, newEmail));
                    }
                }
                System.out.println("User " + userId + " name and email updated.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user name and email: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: users.email") || e.getSQLState().equals("23000")) {
                JOptionPane.showMessageDialog(null, "Error: The email '" + newEmail + "' is already in use by another user.", "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
        return false;
    }

    public static boolean updateUserContact(int userId, String newContact) {
        User oldUser = getUserById(userId);
        String oldContact = oldUser != null ? oldUser.getContact() : null;

        String sql = "UPDATE users SET contact = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newContact);
            pstmt.setInt(2, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    if (oldContact != null && !oldContact.equals(newContact)) {
                        addChangelogEntry(new ChangelogEntry("users", userId, "UPDATE", "contact", oldContact, newContact));
                    }
                }
                System.out.println("User " + userId + " contact updated.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user contact: " + e.getMessage());
            return false;
        }
        return false;
    }

    public static boolean updateUserPassword(int userId, String newPassword) {
        // Passwords are not retrieved for old value logging for security reasons.
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    // Log password change, but not old/new value
                    addChangelogEntry(new ChangelogEntry("users", userId, "UPDATE", "password", "HIDDEN", "HIDDEN"));
                }
                System.out.println("User " + userId + " password updated.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user password: " + e.getMessage());
        }
        return false;
    }

    public static boolean updateUserRole(int userId, String newRole) {
        User oldUser = getUserById(userId);
        String oldRole = oldUser != null ? oldUser.getRole() : null;

        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newRole);
            pstmt.setInt(2, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    if (oldRole != null && !oldRole.equals(newRole)) {
                        addChangelogEntry(new ChangelogEntry("users", userId, "UPDATE", "role", oldRole, newRole));
                    }
                }
                System.out.println("User " + userId + " role updated to " + newRole + ".");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user role: " + e.getMessage());
        }
        return false;
    }

    public static boolean deletePlanAndRelatedData(int planId) {
        String deleteDocumentsSql = "DELETE FROM documents WHERE plan_id = ?";
        String deleteBillingSql = "DELETE FROM billing WHERE plan_id = ?";
        String deleteLogsSql = "DELETE FROM logs WHERE plan_id = ?";
        String deletePlanSql = "DELETE FROM plans WHERE id = ?";

        Connection conn = getActiveConnection();
        try (PreparedStatement pstmtDocs = conn.prepareStatement(deleteDocumentsSql);
             PreparedStatement pstmtBilling = conn.prepareStatement(deleteBillingSql);
             PreparedStatement pstmtLogs = conn.prepareStatement(deleteLogsSql);
             PreparedStatement pstmtPlan = conn.prepareStatement(deletePlanSql)) {

            conn.setAutoCommit(false);

            pstmtDocs.setInt(1, planId);
            pstmtDocs.executeUpdate();

            pstmtBilling.setInt(1, planId);
            pstmtBilling.executeUpdate();

            pstmtLogs.setInt(1, planId);
            pstmtLogs.executeUpdate();

            pstmtPlan.setInt(1, planId);
            int rowsAffected = pstmtPlan.executeUpdate();

            // NEW: Log plan deletion if using SQLite
            if (currentDatabaseType == DatabaseType.SQLITE && rowsAffected > 0) {
                addChangelogEntry(new ChangelogEntry("plans", planId, "DELETE"));
                // For simplicity, we assume cascading deletes are handled by the central DB
                // and only log the parent deletion. More complex sync would log child deletions too.
            }

            conn.commit();
            if (rowsAffected > 0) {
                System.out.println("Plan " + planId + " and all related data deleted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting plan and related data: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Rollback error: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error resetting auto-commit: " + ex.getMessage());
            }
        }
        return false;
    }

    public static boolean addMeeting(Meeting meeting) {
        String sql = "INSERT INTO meetings (title, date, time, location, agenda, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, meeting.getTitle());
            pstmt.setObject(2, meeting.getDate()); // Use setObject for LocalDate
            pstmt.setString(3, meeting.getTime());
            pstmt.setString(4, meeting.getLocation());
            pstmt.setString(5, meeting.getAgenda());
            pstmt.setString(6, meeting.getStatus());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                meeting.setId(rs.getInt(1));
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("meetings", meeting.getId(), "INSERT"));
                }
            }
            System.out.println("Meeting '" + meeting.getTitle() + "' added successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding meeting: " + e.getMessage());
            return false;
        }
    }

    public static Meeting getMeetingById(int meetingId) {
        String sql = "SELECT * FROM meetings WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, meetingId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Meeting(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getObject("date", LocalDate.class), // Use getObject for LocalDate
                    rs.getString("time"),
                    rs.getString("location"),
                    rs.getString("agenda"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting meeting by ID: " + e.getMessage());
        }
        return null;
    }

    public static List<Meeting> getAllMeetings() {
        List<Meeting> meetings = new ArrayList<>();
        String sql = "SELECT * FROM meetings ORDER BY date DESC, time DESC";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                meetings.add(new Meeting(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getObject("date", LocalDate.class), // Use getObject for LocalDate
                    rs.getString("time"),
                    rs.getString("location"),
                    rs.getString("agenda"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all meetings: " + e.getMessage());
        }
        return meetings;
    }

    public static boolean updateMeeting(Meeting meeting) {
        Meeting oldMeeting = getMeetingById(meeting.getId());
        String oldTitle = oldMeeting != null ? oldMeeting.getTitle() : null;
        LocalDate oldDate = oldMeeting != null ? oldMeeting.getDate() : null;
        String oldTime = oldMeeting != null ? oldMeeting.getTime() : null;
        String oldLocation = oldMeeting != null ? oldMeeting.getLocation() : null;
        String oldAgenda = oldMeeting != null ? oldMeeting.getAgenda() : null;
        String oldStatus = oldMeeting != null ? oldMeeting.getStatus() : null;

        String sql = "UPDATE meetings SET title = ?, date = ?, time = ?, location = ?, agenda = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, meeting.getTitle());
            pstmt.setObject(2, meeting.getDate()); // Use setObject for LocalDate
            pstmt.setString(3, meeting.getTime());
            pstmt.setString(4, meeting.getLocation());
            pstmt.setString(5, meeting.getAgenda());
            pstmt.setString(6, meeting.getStatus());
            pstmt.setInt(7, meeting.getId());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    if (oldTitle != null && !oldTitle.equals(meeting.getTitle())) addChangelogEntry(new ChangelogEntry("meetings", meeting.getId(), "UPDATE", "title", oldTitle, meeting.getTitle()));
                    if (oldDate != null && !oldDate.equals(meeting.getDate())) addChangelogEntry(new ChangelogEntry("meetings", meeting.getId(), "UPDATE", "date", oldDate.toString(), meeting.getDate().toString()));
                    if (oldTime != null && !oldTime.equals(meeting.getTime())) addChangelogEntry(new ChangelogEntry("meetings", meeting.getId(), "UPDATE", "time", oldTime, meeting.getTime()));
                    if (oldLocation != null && !oldLocation.equals(meeting.getLocation())) addChangelogEntry(new ChangelogEntry("meetings", meeting.getId(), "UPDATE", "location", oldLocation, meeting.getLocation()));
                    if (oldAgenda != null && !oldAgenda.equals(meeting.getAgenda())) addChangelogEntry(new ChangelogEntry("meetings", meeting.getId(), "UPDATE", "agenda", oldAgenda, meeting.getAgenda()));
                    if (oldStatus != null && !oldStatus.equals(meeting.getStatus())) addChangelogEntry(new ChangelogEntry("meetings", meeting.getId(), "UPDATE", "status", oldStatus, meeting.getStatus()));
                }
                System.out.println("Meeting " + meeting.getId() + " updated successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating meeting: " + e.getMessage());
        }
        return false;
    }

    public static boolean deleteMeeting(int meetingId) {
        String sql = "DELETE FROM meetings WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, meetingId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("meetings", meetingId, "DELETE"));
                }
                System.out.println("Meeting " + meetingId + " deleted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting meeting: " + e.getMessage());
        }
        return false;
    }

    public static int getUpcomingMeetingsCount() {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM meetings WHERE date >= ? AND status = 'Scheduled'";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setObject(1, LocalDate.now()); // Use setObject for LocalDate
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting upcoming meetings count: " + e.getMessage());
        }
        return count;
    }

    public static boolean addChecklistItem(String itemName, boolean isRequired, boolean requiresFileUpload) {
        String sql = "INSERT INTO document_checklist_items (item_name, is_required, requires_file_upload) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, itemName);
            pstmt.setBoolean(2, isRequired);
            pstmt.setBoolean(3, requiresFileUpload);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int itemId = rs.getInt(1);
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("document_checklist_items", itemId, "INSERT"));
                }
            }
            System.out.println("Checklist item '" + itemName + "' added successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding checklist item: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: document_checklist_items.item_name") || e.getSQLState().equals("23000")) {
                JOptionPane.showMessageDialog(null, "Error: A checklist item with this name already exists.", "Add Item Failed", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
    }

    public static List<DocumentChecklistItem> getAllChecklistItems() {
        List<DocumentChecklistItem> items = new ArrayList<>();
        String sql = "SELECT id, item_name, is_required, requires_file_upload FROM document_checklist_items ORDER BY item_name";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(new DocumentChecklistItem(
                    rs.getInt("id"),
                    rs.getString("item_name"),
                    rs.getBoolean("is_required"),
                    rs.getBoolean("requires_file_upload")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all checklist items: " + e.getMessage());
        }
        return items;
    }

    public static boolean updateChecklistItem(int id, String newItemName, boolean newIsRequired, boolean newRequiresFileUpload) {
        DocumentChecklistItem oldItem = null;
        for (DocumentChecklistItem item : getAllChecklistItems()) { // Fetch old item
            if (item.getId() == id) {
                oldItem = item;
                break;
            }
        }
        String oldItemName = oldItem != null ? oldItem.getItemName() : null;
        boolean oldIsRequired = oldItem != null ? oldItem.isRequired() : false;
        boolean oldRequiresFileUpload = oldItem != null ? oldItem.requiresFileUpload() : false;

        String sql = "UPDATE document_checklist_items SET item_name = ?, is_required = ?, requires_file_upload = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newItemName);
            pstmt.setBoolean(2, newIsRequired);
            pstmt.setBoolean(3, newRequiresFileUpload);
            pstmt.setInt(4, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    if (oldItemName != null && !oldItemName.equals(newItemName)) addChangelogEntry(new ChangelogEntry("document_checklist_items", id, "UPDATE", "item_name", oldItemName, newItemName));
                    if (oldIsRequired != newIsRequired) addChangelogEntry(new ChangelogEntry("document_checklist_items", id, "UPDATE", "is_required", String.valueOf(oldIsRequired), String.valueOf(newIsRequired)));
                    if (oldRequiresFileUpload != newRequiresFileUpload) addChangelogEntry(new ChangelogEntry("document_checklist_items", id, "UPDATE", "requires_file_upload", String.valueOf(oldRequiresFileUpload), String.valueOf(newRequiresFileUpload)));
                }
                System.out.println("Checklist item " + id + " updated successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating checklist item: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: document_checklist_items.item_name") || e.getSQLState().equals("23000")) {
                JOptionPane.showMessageDialog(null, "Error: A checklist item with the name '" + newItemName + "' already exists.", "Update Item Failed", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
        return false;
    }

    public static boolean deleteChecklistItem(int id) {
        String sql = "DELETE FROM document_checklist_items WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("document_checklist_items", id, "DELETE"));
                }
                System.out.println("Checklist item " + id + " deleted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting checklist item: " + e.getMessage());
        }
        return false;
    }

    public static boolean addPeer(Peer peer) {
        String sql = "INSERT INTO peers (ip_address, port, is_trusted, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, peer.getIpAddress());
            pstmt.setInt(2, peer.getPort());
            pstmt.setBoolean(3, peer.isTrusted());
            pstmt.setString(4, peer.getStatus());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                peer.setId(rs.getInt(1));
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("peers", peer.getId(), "INSERT"));
                }
            }
            System.out.println("Peer " + peer.getIpAddress() + ":" + peer.getPort() + " added.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding peer: " + e.getMessage());
            return false;
        }
    }

    public static List<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList<>();
        String sql = "SELECT id, ip_address, port, last_sync_time, is_trusted, status FROM peers ORDER BY ip_address, port";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                peers.add(new Peer(
                    rs.getInt("id"),
                    rs.getString("ip_address"),
                    rs.getInt("port"),
                    rs.getObject("last_sync_time", LocalDateTime.class), // Use getObject for LocalDateTime
                    rs.getBoolean("is_trusted"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all peers: " + e.getMessage());
        }
        return peers;
    }

    public static boolean updatePeer(Peer peer) {
        Peer oldPeer = null;
        for (Peer p : getAllPeers()) { // Fetch old peer
            if (p.getId() == peer.getId()) {
                oldPeer = p;
                break;
            }
        }
        String oldIpAddress = oldPeer != null ? oldPeer.getIpAddress() : null;
        int oldPort = oldPeer != null ? oldPeer.getPort() : 0;
        LocalDateTime oldLastSyncTime = oldPeer != null ? oldPeer.getLastSyncTime() : null;
        boolean oldIsTrusted = oldPeer != null ? oldPeer.isTrusted() : false;
        String oldStatus = oldPeer != null ? oldPeer.getStatus() : null;

        String sql = "UPDATE peers SET ip_address = ?, port = ?, last_sync_time = ?, is_trusted = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, peer.getIpAddress());
            pstmt.setInt(2, peer.getPort());
            pstmt.setObject(3, peer.getLastSyncTime()); // Use setObject for LocalDateTime
            pstmt.setBoolean(4, peer.isTrusted());
            pstmt.setString(5, peer.getStatus());
            pstmt.setInt(6, peer.getId());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    if (oldIpAddress != null && !oldIpAddress.equals(peer.getIpAddress())) addChangelogEntry(new ChangelogEntry("peers", peer.getId(), "UPDATE", "ip_address", oldIpAddress, peer.getIpAddress()));
                    if (oldPort != peer.getPort()) addChangelogEntry(new ChangelogEntry("peers", peer.getId(), "UPDATE", "port", String.valueOf(oldPort), String.valueOf(peer.getPort())));
                    if (oldLastSyncTime != null && !oldLastSyncTime.equals(peer.getLastSyncTime())) addChangelogEntry(new ChangelogEntry("peers", peer.getId(), "UPDATE", "last_sync_time", oldLastSyncTime.toString(), peer.getLastSyncTime().toString()));
                    if (oldIsTrusted != peer.isTrusted()) addChangelogEntry(new ChangelogEntry("peers", peer.getId(), "UPDATE", "is_trusted", String.valueOf(oldIsTrusted), String.valueOf(peer.isTrusted())));
                    if (oldStatus != null && !oldStatus.equals(peer.getStatus())) addChangelogEntry(new ChangelogEntry("peers", peer.getId(), "UPDATE", "status", oldStatus, peer.getStatus()));
                }
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error updating peer: " + e.getMessage());
            return false;
        }
        return false;
    }

    public static boolean deletePeer(int peerId) {
        String sql = "DELETE FROM peers WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, peerId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("peers", peerId, "DELETE"));
                }
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting peer: " + e.getMessage());
            return false;
        }
        return false;
    }

    public static boolean addSystemUpdate(SystemUpdate update) {
        String sql = "INSERT INTO system_updates (version, title, message, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, update.getVersion());
            pstmt.setString(2, update.getTitle());
            pstmt.setString(3, update.getMessage());
            pstmt.setObject(4, update.getCreatedAt()); // Use setObject for LocalDateTime
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                update.setId(rs.getInt(1));
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("system_updates", update.getId(), "INSERT"));
                }
            }
            System.out.println("System update '" + update.getTitle() + "' added.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding system update: " + e.getMessage());
            return false;
        }
    }

    // NEW: Method to update a SystemUpdate
    public static boolean updateSystemUpdate(SystemUpdate update) {
        SystemUpdate oldUpdate = getSystemUpdateById(update.getId()); // Assuming a get method exists
        String oldVersion = oldUpdate != null ? oldUpdate.getVersion() : null;
        String oldTitle = oldUpdate != null ? oldUpdate.getTitle() : null;
        String oldMessage = oldUpdate != null ? oldUpdate.getMessage() : null;

        String sql = "UPDATE system_updates SET version = ?, title = ?, message = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, update.getVersion());
            pstmt.setString(2, update.getTitle());
            pstmt.setString(3, update.getMessage());
            pstmt.setInt(4, update.getId());
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("System update " + update.getId() + " updated successfully.");
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    if (oldVersion != null && !oldVersion.equals(update.getVersion())) addChangelogEntry(new ChangelogEntry("system_updates", update.getId(), "UPDATE", "version", oldVersion, update.getVersion()));
                    if (oldTitle != null && !oldTitle.equals(update.getTitle())) addChangelogEntry(new ChangelogEntry("system_updates", update.getId(), "UPDATE", "title", oldTitle, update.getTitle()));
                    if (oldMessage != null && !oldMessage.equals(update.getMessage())) addChangelogEntry(new ChangelogEntry("system_updates", update.getId(), "UPDATE", "message", oldMessage, update.getMessage()));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error updating system update: " + e.getMessage());
            return false;
        }
    }

    // Helper to get SystemUpdate by ID for logging purposes
    private static SystemUpdate getSystemUpdateById(int id) {
        String sql = "SELECT id, version, title, message, created_at FROM system_updates WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new SystemUpdate(
                    rs.getInt("id"),
                    rs.getString("version"),
                    rs.getString("title"),
                    rs.getString("message"),
                    rs.getObject("created_at", LocalDateTime.class)
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting system update by ID: " + e.getMessage());
        }
        return null;
    }

    // NEW: Method to delete a SystemUpdate
    public static boolean deleteSystemUpdate(int id) {
        String sql = "DELETE FROM system_updates WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("System update " + id + " deleted successfully.");
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("system_updates", id, "DELETE"));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error deleting system update: " + e.getMessage());
            return false;
        }
    }

    public static List<SystemUpdate> getAllSystemUpdates() {
        List<SystemUpdate> updates = new ArrayList<>();
        String sql = "SELECT id, version, title, message, created_at FROM system_updates ORDER BY created_at DESC";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                updates.add(new SystemUpdate(
                    rs.getInt("id"),
                    rs.getString("version"),
                    rs.getString("title"),
                    rs.getString("message"),
                    rs.getObject("created_at", LocalDateTime.class) // Use getObject for LocalDateTime
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all system updates: " + e.getMessage());
        }
        return updates;
    }

    // NEW: Method to get the latest system update
    public static SystemUpdate getLatestSystemUpdate() {
        String sql = "SELECT id, version, title, message, created_at FROM system_updates ORDER BY id DESC LIMIT 1";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new SystemUpdate(
                    rs.getInt("id"),
                    rs.getString("version"),
                    rs.getString("title"),
                    rs.getString("message"),
                    rs.getObject("created_at", LocalDateTime.class)
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting latest system update: " + e.getMessage());
        }
        return null;
    }

    public static MessageTemplate getMessageTemplateByName(String templateName) {
        String sql = "SELECT id, template_name, subject, body, type FROM message_templates WHERE template_name = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, templateName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new MessageTemplate(
                    rs.getInt("id"),
                    rs.getString("template_name"),
                    rs.getString("subject"),
                    rs.getString("body"),
                    rs.getString("type")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting message template by name: " + e.getMessage());
        }
        return null;
    }

    public static List<MessageTemplate> getAllMessageTemplates() {
        List<MessageTemplate> templates = new ArrayList<>();
        String sql = "SELECT id, template_name, subject, body, type FROM message_templates ORDER BY template_name";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                templates.add(new MessageTemplate(
                    rs.getInt("id"),
                    rs.getString("template_name"),
                    rs.getString("subject"),
                    rs.getString("body"),
                    rs.getString("type")
                ));
            }
        }
        catch (SQLException e) {
            System.err.println("Error getting all message templates: " + e.getMessage());
        }
        return templates;
    }

    public static boolean updateMessageTemplate(MessageTemplate template) {
        MessageTemplate oldTemplate = getMessageTemplateByName(template.getTemplateName()); // Assuming name is unique
        String oldSubject = oldTemplate != null ? oldTemplate.getSubject() : null;
        String oldBody = oldTemplate != null ? oldTemplate.getBody() : null;
        String oldType = oldTemplate != null ? oldTemplate.getType() : null;

        String sql = "UPDATE message_templates SET subject = ?, body = ?, type = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, template.getSubject());
            pstmt.setString(2, template.getBody());
            pstmt.setString(3, template.getType());
            pstmt.setInt(4, template.getId());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    if (oldSubject != null && !oldSubject.equals(template.getSubject())) addChangelogEntry(new ChangelogEntry("message_templates", template.getId(), "UPDATE", "subject", oldSubject, template.getSubject()));
                    if (oldBody != null && !oldBody.equals(template.getBody())) addChangelogEntry(new ChangelogEntry("message_templates", template.getId(), "UPDATE", "body", oldBody, template.getBody()));
                    if (oldType != null && !oldType.equals(template.getType())) addChangelogEntry(new ChangelogEntry("message_templates", template.getId(), "UPDATE", "type", oldType, template.getType()));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error updating message template: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteMessageTemplate(int id) {
        String sql = "DELETE FROM message_templates WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("message_templates", id, "DELETE"));
                }
                return rowsAffected > 0;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error deleting message template: " + e.getMessage());
            return false;
        }
    }

    public static boolean addMessageLog(MessageLog log) {
        String sql = "INSERT INTO message_logs (timestamp, recipient, message_type, subject, message, status, details) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, log.getTimestamp()); // Use setObject for LocalDateTime
            pstmt.setString(2, log.getRecipient());
            pstmt.setString(3, log.getMessageType());
            pstmt.setString(4, log.getSubject());
            pstmt.setString(5, log.getMessage());
            pstmt.setString(6, log.getStatus());
            pstmt.setString(7, log.getDetails());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                log.setId(rs.getInt(1));
                if (currentDatabaseType == DatabaseType.SQLITE) {
                    addChangelogEntry(new ChangelogEntry("message_logs", log.getId(), "INSERT"));
                }
            }
            System.out.println("Message log added for recipient: " + log.getRecipient());
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding message log: " + e.getMessage());
            return false;
        }
    }

    public static List<MessageLog> getAllMessageLogs() {
        List<MessageLog> logs = new ArrayList<>();
        String sql = "SELECT id, timestamp, recipient, message_type, subject, message, status, details FROM message_logs ORDER BY timestamp DESC";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new MessageLog(
                    rs.getInt("id"),
                    rs.getObject("timestamp", LocalDateTime.class), // Use getObject for LocalDateTime
                    rs.getString("recipient"),
                    rs.getString("message_type"),
                    rs.getString("subject"),
                    rs.getString("message"),
                    rs.getString("status"),
                    rs.getString("details")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all message logs: " + e.getMessage());
        }
        return logs;
    }

    public static UserPreference getUserPreferences(int userId) {
        String sql = "SELECT id, user_id, email_notifications_enabled, sms_notifications_enabled, last_seen_update_id FROM user_preferences WHERE user_id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new UserPreference(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getBoolean("email_notifications_enabled"),
                    rs.getBoolean("sms_notifications_enabled"),
                    rs.getInt("last_seen_update_id") // NEW: Retrieve last_seen_update_id
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting user preferences for user ID " + userId + ": " + e.getMessage());
        }
        return null;
    }

    public static boolean saveUserPreferences(UserPreference preferences) {
        String sql = "INSERT INTO user_preferences (user_id, email_notifications_enabled, sms_notifications_enabled, last_seen_update_id) VALUES (?, ?, ?, ?) " +
                      "ON CONFLICT(user_id) DO UPDATE SET email_notifications_enabled = ?, sms_notifications_enabled = ?, last_seen_update_id = ?";
        
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, preferences.getUserId());
            pstmt.setBoolean(2, preferences.isEmailNotificationsEnabled());
            pstmt.setBoolean(3, preferences.isSmsNotificationsEnabled());
            pstmt.setInt(4, preferences.getLastSeenUpdateId()); // NEW: Set last_seen_update_id for insert
            pstmt.setBoolean(5, preferences.isEmailNotificationsEnabled());
            pstmt.setBoolean(6, preferences.isSmsNotificationsEnabled());
            pstmt.setInt(7, preferences.getLastSeenUpdateId()); // NEW: Set last_seen_update_id for update
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("User preferences saved for user " + preferences.getUserId() + ". Rows affected: " + rowsAffected);
            
            if (currentDatabaseType == DatabaseType.SQLITE && rowsAffected > 0) {
                // For ON CONFLICT, SQLite returns 0 for rowsAffected if no actual change, 1 if insert, 2 if update.
                // MySQL returns 1 for insert, 2 for update.
                // We'll simplify and just log an UPDATE if rowsAffected > 0, as it's either an insert or update.
                // A more precise approach would involve fetching old values.
                addChangelogEntry(new ChangelogEntry("user_preferences", preferences.getUserId(), "UPDATE")); // Simplified logging
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error saving user preferences: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to check if a document is required by planning.
     * @param docName The name of the document.
     * @param checklistItems A list of all available DocumentChecklistItem objects.
     * @return true if the document name matches a required item in the checklist, false otherwise.
     */
    public static boolean isDocumentRequired(String docName, List<DocumentChecklistItem> checklistItems) {
        return checklistItems.stream()
                             .filter(DocumentChecklistItem::isRequired)
                             .anyMatch(item -> item.getItemName().equalsIgnoreCase(docName));
    }

    /**
     * Helper method to open a file using the system's default application.
     * @param filePath The absolute path to the file.
     * @param parentComponent The parent component for JOptionPane, can be null.
     */
    public static void openDocumentFile(String filePath, java.awt.Component parentComponent) {
        if (filePath == null || filePath.trim().isEmpty() || filePath.equals("N/A (No file upload required)")) {
            JOptionPane.showMessageDialog(parentComponent, "No physical file available for this document or path is invalid.", "File Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(parentComponent, "File not found at: " + filePath, "File Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(parentComponent, "Desktop operations are not supported on this system.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentComponent, "Failed to open file: " + e.getMessage(), "Error Opening File", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error opening file: " + e.getMessage());
        }
    }

    // --- NEW: Changelog Management Methods ---

    /**
     * Adds a new changelog entry to the database.
     * This is called when a modification occurs in the local SQLite database.
     * @param entry The ChangelogEntry object to add.
     * @return true if the entry was added successfully, false otherwise.
     */
    public static boolean addChangelogEntry(ChangelogEntry entry) {
        String sql = "INSERT INTO changelog (table_name, record_id, change_type, column_name, old_value, new_value, timestamp, is_synced) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, entry.getTableName());
            pstmt.setInt(2, entry.getRecordId());
            pstmt.setString(3, entry.getChangeType());
            pstmt.setString(4, entry.getColumnName());
            pstmt.setString(5, entry.getOldValue());
            pstmt.setString(6, entry.getNewValue());
            pstmt.setObject(7, entry.getTimestamp());
            pstmt.setBoolean(8, entry.isSynced());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                entry.setId(rs.getInt(1));
            }
            System.out.println("Changelog: Added entry for " + entry.getTableName() + " ID " + entry.getRecordId() + " (" + entry.getChangeType() + ")");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding changelog entry: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all unsynced changelog entries.
     * @return A list of unsynced ChangelogEntry objects.
     */
    public static List<ChangelogEntry> getUnsyncedChangelogEntries() {
        List<ChangelogEntry> entries = new ArrayList<>();
        String sql = "SELECT id, table_name, record_id, change_type, column_name, old_value, new_value, timestamp, is_synced FROM changelog WHERE is_synced = 0 ORDER BY timestamp ASC";
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                entries.add(new ChangelogEntry(
                    rs.getInt("id"),
                    rs.getString("table_name"),
                    rs.getInt("record_id"),
                    rs.getString("change_type"),
                    rs.getString("column_name"),
                    rs.getString("old_value"),
                    rs.getString("new_value"),
                    rs.getObject("timestamp", LocalDateTime.class),
                    rs.getBoolean("is_synced")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting unsynced changelog entries: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Marks a changelog entry as synced.
     * @param entryId The ID of the changelog entry to mark.
     * @return true if the entry was marked successfully, false otherwise.
     */
    public static boolean markChangelogEntryAsSynced(int entryId) {
        String sql = "UPDATE changelog SET is_synced = 1 WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, entryId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Changelog: Entry " + entryId + " marked as synced.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error marking changelog entry as synced: " + e.getMessage());
        }
        return false;
    }

    /**
     * Applies a single changelog entry to a given database connection.
     * This method is crucial for pushing local changes to the central database.
     * It currently supports 'plans' table for INSERT, UPDATE, DELETE.
     *
     * @param entry The ChangelogEntry to apply.
     * @param targetConn The Connection to the target database (e.g., central DB).
     * @return true if the change was successfully applied, false otherwise.
     */
    public static boolean applyChangelogEntryToCentralDb(ChangelogEntry entry, Connection targetConn) throws SQLException {
        String tableName = entry.getTableName();
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        switch (tableName) {
            case "plans":
                return syncPlan(entry, targetConn);
            case "users":
                return syncUser(entry, targetConn);
            case "documents":
                return syncDocument(entry, targetConn);
            case "billing":
                return syncBilling(entry, targetConn);
            case "logs":
                return syncLog(entry, targetConn);
            case "meetings":
                return syncMeeting(entry, targetConn);
            case "document_checklist_items":
                return syncDocumentChecklistItem(entry, targetConn);
            case "peers":
                return syncPeer(entry, targetConn);
            case "system_updates":
                return syncSystemUpdate(entry, targetConn);
            case "message_templates":
                return syncMessageTemplate(entry, targetConn);
            case "message_logs":
                return syncMessageLog(entry, targetConn);
            case "user_preferences":
                return syncUserPreference(entry, targetConn);
            default:
                System.err.println("Changelog: Unsupported table for sync: " + tableName);
                return false;
        }
    }

    // --- Helper methods for syncing individual tables ---

    private static boolean syncPlan(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            Plan localPlan = getPlanById(recordId);
            if (localPlan == null) { System.err.println("Changelog: Local plan " + recordId + " not found for INSERT. Skipping."); return false; }
            String insertSql = "INSERT INTO plans (id, applicant_name, contact, plot_no, location, date_submitted, reference_no, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localPlan.getId());
                pstmt.setString(2, localPlan.getApplicantName());
                pstmt.setString(3, localPlan.getContact());
                pstmt.setString(4, localPlan.getPlotNo());
                pstmt.setString(5, localPlan.getLocation());
                pstmt.setObject(6, localPlan.getDateSubmitted());
                pstmt.setString(7, localPlan.getReferenceNo());
                pstmt.setString(8, localPlan.getStatus());
                pstmt.setString(9, localPlan.getRemarks());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted plan " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: Plan " + recordId + " already exists in central DB. Attempting UPDATE instead.");
                    return updatePlanInCentralDb(localPlan, targetConn);
                }
                throw e;
            }
        } else if ("UPDATE".equals(changeType)) {
            Plan localPlan = getPlanById(recordId);
            if (localPlan == null) { System.err.println("Changelog: Local plan " + recordId + " not found for UPDATE. Skipping."); return false; }
            return updatePlanInCentralDb(localPlan, targetConn);
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM plans WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted plan " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    private static boolean updatePlanInCentralDb(Plan plan, Connection targetConn) throws SQLException {
        String updateSql = "UPDATE plans SET applicant_name = ?, contact = ?, plot_no = ?, location = ?, date_submitted = ?, reference_no = ?, status = ?, remarks = ? WHERE id = ?";
        try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
            pstmt.setString(1, plan.getApplicantName());
            pstmt.setString(2, plan.getContact());
            pstmt.setString(3, plan.getPlotNo());
            pstmt.setString(4, plan.getLocation());
            pstmt.setObject(5, plan.getDateSubmitted());
            pstmt.setString(6, plan.getReferenceNo());
            pstmt.setString(7, plan.getStatus());
            pstmt.setString(8, plan.getRemarks());
            pstmt.setInt(9, plan.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) { System.out.println("Changelog: Updated full plan " + plan.getId() + " in central DB."); return true; }
            System.out.println("Changelog: Plan " + plan.getId() + " not found in central DB for update. Attempting INSERT instead.");
            return insertPlanIntoCentralDb(plan, targetConn);
        }
    }

    private static boolean insertPlanIntoCentralDb(Plan plan, Connection targetConn) throws SQLException {
        String insertSql = "INSERT INTO plans (id, applicant_name, contact, plot_no, location, date_submitted, reference_no, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
            pstmt.setInt(1, plan.getId());
            pstmt.setString(2, plan.getApplicantName());
            pstmt.setString(3, plan.getContact());
            pstmt.setString(4, plan.getPlotNo());
            pstmt.setString(5, plan.getLocation());
            pstmt.setObject(6, plan.getDateSubmitted());
            pstmt.setString(7, plan.getReferenceNo());
            pstmt.setString(8, plan.getStatus());
            pstmt.setString(9, plan.getRemarks());
            pstmt.executeUpdate();
            System.out.println("Changelog: Inserted full plan " + plan.getId() + " into central DB.");
            return true;
        }
    }

    private static boolean syncUser(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            User localUser = getUserById(recordId);
            if (localUser == null) { System.err.println("Changelog: Local user " + recordId + " not found for INSERT. Skipping."); return false; }
            String insertSql = "INSERT INTO users (id, name, email, password, role, contact) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localUser.getId());
                pstmt.setString(2, localUser.getName());
                pstmt.setString(3, localUser.getEmail());
                pstmt.setString(4, "password"); // Passwords are not synced directly, use a placeholder or re-hash
                pstmt.setString(5, localUser.getRole());
                pstmt.setString(6, localUser.getContact());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted user " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: User " + recordId + " already exists in central DB. Attempting UPDATE instead.");
                    return updateUserInCentralDb(localUser, targetConn);
                }
                throw e;
            }
        } else if ("UPDATE".equals(changeType)) {
            User localUser = getUserById(recordId);
            if (localUser == null) { System.err.println("Changelog: Local user " + recordId + " not found for UPDATE. Skipping."); return false; }
            return updateUserInCentralDb(localUser, targetConn);
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM users WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted user " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    private static boolean updateUserInCentralDb(User user, Connection targetConn) throws SQLException {
        String updateSql = "UPDATE users SET name = ?, email = ?, role = ?, contact = ? WHERE id = ?";
        try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getContact());
            pstmt.setInt(5, user.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) { System.out.println("Changelog: Updated full user " + user.getId() + " in central DB."); return true; }
            System.out.println("Changelog: User " + user.getId() + " not found in central DB for update. Attempting INSERT instead.");
            return insertUserIntoCentralDb(user, targetConn);
        }
    }

    private static boolean insertUserIntoCentralDb(User user, Connection targetConn) throws SQLException {
        String insertSql = "INSERT INTO users (id, name, email, password, role, contact) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
            pstmt.setInt(1, user.getId());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, "password"); // Placeholder password for new users
            pstmt.setString(5, user.getRole());
            pstmt.setString(6, user.getContact());
            pstmt.executeUpdate();
            System.out.println("Changelog: Inserted full user " + user.getId() + " into central DB.");
            return true;
        }
    }

    private static boolean syncDocument(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        // Documents are typically tied to plans. For simplicity, we'll only handle INSERT/DELETE.
        // Updates to documents are less common and would require fetching the full document object.
        if ("INSERT".equals(changeType)) {
            // We need to fetch the full document from the local DB
            String selectSql = "SELECT id, plan_id, doc_name, file_path, is_attached, document_type FROM documents WHERE id = ?";
            Document localDoc = null;
            try (PreparedStatement pstmt = getActiveConnection().prepareStatement(selectSql)) {
                pstmt.setInt(1, recordId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    localDoc = new Document(rs.getInt("id"), rs.getInt("plan_id"), rs.getString("doc_name"),
                                            rs.getString("file_path"), rs.getBoolean("is_attached"), rs.getString("document_type"));
                }
            }
            if (localDoc == null) { System.err.println("Changelog: Local document " + recordId + " not found for INSERT. Skipping."); return false; }

            String insertSql = "INSERT INTO documents (id, plan_id, doc_name, file_path, is_attached, document_type) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localDoc.getId());
                pstmt.setInt(2, localDoc.getPlanId());
                pstmt.setString(3, localDoc.getDocName());
                pstmt.setString(4, localDoc.getFilePath());
                pstmt.setBoolean(5, localDoc.isAttached());
                pstmt.setString(6, localDoc.getDocumentType());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted document " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: Document " + recordId + " already exists in central DB. Skipping INSERT.");
                    return true; // Consider it synced if it already exists
                }
                throw e;
            }
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM documents WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted document " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    private static boolean syncBilling(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            // Need to fetch billing by its ID, not plan ID.
            Billing localBilling = getBillingById(recordId);
            if (localBilling == null) { System.err.println("Changelog: Local billing " + recordId + " not found for INSERT. Skipping."); return false; }
            String insertSql = "INSERT INTO billing (id, plan_id, amount, receipt_no, date_paid) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localBilling.getId());
                pstmt.setInt(2, localBilling.getPlanId());
                pstmt.setDouble(3, localBilling.getAmount());
                pstmt.setString(4, localBilling.getReceiptNo());
                pstmt.setObject(5, localBilling.getDatePaid());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted billing " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: Billing " + recordId + " already exists in central DB. Attempting UPDATE instead.");
                    return updateBillingInCentralDb(localBilling, targetConn);
                }
                throw e;
            }
        } else if ("UPDATE".equals(changeType)) {
            Billing localBilling = getBillingById(recordId);
            if (localBilling == null) { System.err.println("Changelog: Local billing " + recordId + " not found for UPDATE. Skipping."); return false; }
            return updateBillingInCentralDb(localBilling, targetConn);
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM billing WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted billing " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    // Helper to get Billing by ID for logging purposes
    private static Billing getBillingById(int id) {
        String sql = "SELECT id, plan_id, amount, receipt_no, date_paid FROM billing WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Billing(
                    rs.getInt("id"),
                    rs.getInt("plan_id"),
                    rs.getDouble("amount"),
                    rs.getString("receipt_no"),
                    rs.getObject("date_paid", LocalDate.class)
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting billing by ID: " + e.getMessage());
        }
        return null;
    }

    private static boolean updateBillingInCentralDb(Billing billing, Connection targetConn) throws SQLException {
        String updateSql = "UPDATE billing SET plan_id = ?, amount = ?, receipt_no = ?, date_paid = ? WHERE id = ?";
        try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
            pstmt.setInt(1, billing.getPlanId());
            pstmt.setDouble(2, billing.getAmount());
            pstmt.setString(3, billing.getReceiptNo());
            pstmt.setObject(4, billing.getDatePaid());
            pstmt.setInt(5, billing.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) { System.out.println("Changelog: Updated full billing " + billing.getId() + " in central DB."); return true; }
            System.out.println("Changelog: Billing " + billing.getId() + " not found in central DB for update. Attempting INSERT instead.");
            return insertBillingIntoCentralDb(billing, targetConn);
        }
    }

    private static boolean insertBillingIntoCentralDb(Billing billing, Connection targetConn) throws SQLException {
        String insertSql = "INSERT INTO billing (id, plan_id, amount, receipt_no, date_paid) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
            pstmt.setInt(1, billing.getId());
            pstmt.setInt(2, billing.getPlanId());
            pstmt.setDouble(3, billing.getAmount());
            pstmt.setString(4, billing.getReceiptNo());
            pstmt.setObject(5, billing.getDatePaid());
            pstmt.executeUpdate();
            System.out.println("Changelog: Inserted full billing " + billing.getId() + " into central DB.");
            return true;
        }
    }

    private static boolean syncLog(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            // For logs, we need to fetch the specific log entry.
            Log localLog = getLogById(recordId);
            if (localLog == null) { System.err.println("Changelog: Local log " + recordId + " not found for INSERT. Skipping."); return false; }

            String insertSql = "INSERT INTO logs (id, plan_id, from_role, to_role, action, remarks, date) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localLog.getId());
                pstmt.setInt(2, localLog.getPlanId());
                pstmt.setString(3, localLog.getFromRole());
                pstmt.setString(4, localLog.getToRole());
                pstmt.setString(5, localLog.getAction());
                pstmt.setString(6, localLog.getRemarks());
                pstmt.setObject(7, localLog.getDate());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted log " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: Log " + recordId + " already exists in central DB. Skipping INSERT.");
                    return true; // Consider it synced if it already exists
                }
                throw e;
            }
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM logs WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted log " + recordId + " from central DB.");
                return true;
            }
        }
        // Logs are typically append-only, updates are not usually performed.
        return false;
    }

    // Helper to get Log by ID for logging purposes
    private static Log getLogById(int id) {
        String sql = "SELECT id, plan_id, from_role, to_role, action, remarks, date FROM logs WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Log(
                    rs.getInt("id"),
                    rs.getInt("plan_id"),
                    rs.getString("from_role"),
                    rs.getString("to_role"),
                    rs.getString("action"),
                    rs.getString("remarks"),
                    rs.getObject("date", LocalDate.class)
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting log by ID: " + e.getMessage());
        }
        return null;
    }

    private static boolean syncMeeting(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            Meeting localMeeting = getMeetingById(recordId);
            if (localMeeting == null) { System.err.println("Changelog: Local meeting " + recordId + " not found for INSERT. Skipping."); return false; }
            String insertSql = "INSERT INTO meetings (id, title, date, time, location, agenda, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localMeeting.getId());
                pstmt.setString(2, localMeeting.getTitle());
                pstmt.setObject(3, localMeeting.getDate());
                pstmt.setString(4, localMeeting.getTime());
                pstmt.setString(5, localMeeting.getLocation());
                pstmt.setString(6, localMeeting.getAgenda());
                pstmt.setString(7, localMeeting.getStatus());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted meeting " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: Meeting " + recordId + " already exists in central DB. Attempting UPDATE instead.");
                    return updateMeetingInCentralDb(localMeeting, targetConn);
                }
                throw e;
            }
        } else if ("UPDATE".equals(changeType)) {
            Meeting localMeeting = getMeetingById(recordId);
            if (localMeeting == null) { System.err.println("Changelog: Local meeting " + recordId + " not found for UPDATE. Skipping."); return false; }
            return updateMeetingInCentralDb(localMeeting, targetConn);
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM meetings WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted meeting " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    private static boolean updateMeetingInCentralDb(Meeting meeting, Connection targetConn) throws SQLException {
        String updateSql = "UPDATE meetings SET title = ?, date = ?, time = ?, location = ?, agenda = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
            pstmt.setString(1, meeting.getTitle());
            pstmt.setObject(2, meeting.getDate());
            pstmt.setString(3, meeting.getTime());
            pstmt.setString(4, meeting.getLocation());
            pstmt.setString(5, meeting.getAgenda());
            pstmt.setString(6, meeting.getStatus());
            pstmt.setInt(7, meeting.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) { System.out.println("Changelog: Updated full meeting " + meeting.getId() + " in central DB."); return true; }
            System.out.println("Changelog: Meeting " + meeting.getId() + " not found in central DB for update. Attempting INSERT instead.");
            return insertMeetingIntoCentralDb(meeting, targetConn);
        }
    }

    private static boolean insertMeetingIntoCentralDb(Meeting meeting, Connection targetConn) throws SQLException {
        String insertSql = "INSERT INTO meetings (id, title, date, time, location, agenda, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
            pstmt.setInt(1, meeting.getId());
            pstmt.setString(2, meeting.getTitle());
            pstmt.setObject(3, meeting.getDate());
            pstmt.setString(4, meeting.getTime());
            pstmt.setString(5, meeting.getLocation());
            pstmt.setString(6, meeting.getAgenda());
            pstmt.setString(7, meeting.getStatus());
            pstmt.executeUpdate();
            System.out.println("Changelog: Inserted full meeting " + meeting.getId() + " into central DB.");
            return true;
        }
    }

    private static boolean syncDocumentChecklistItem(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            DocumentChecklistItem localItem = getChecklistItemById(recordId);
            if (localItem == null) { System.err.println("Changelog: Local checklist item " + recordId + " not found for INSERT. Skipping."); return false; }
            String insertSql = "INSERT INTO document_checklist_items (id, item_name, is_required, requires_file_upload) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localItem.getId());
                pstmt.setString(2, localItem.getItemName());
                pstmt.setBoolean(3, localItem.isRequired());
                pstmt.setBoolean(4, localItem.requiresFileUpload());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted checklist item " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: Checklist item " + recordId + " already exists in central DB. Attempting UPDATE instead.");
                    return updateDocumentChecklistItemInCentralDb(localItem, targetConn);
                }
                throw e;
            }
        } else if ("UPDATE".equals(changeType)) {
            DocumentChecklistItem localItem = getChecklistItemById(recordId);
            if (localItem == null) { System.err.println("Changelog: Local checklist item " + recordId + " not found for UPDATE. Skipping."); return false; }
            return updateDocumentChecklistItemInCentralDb(localItem, targetConn);
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM document_checklist_items WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted checklist item " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    // Helper to get DocumentChecklistItem by ID for logging purposes
    private static DocumentChecklistItem getChecklistItemById(int id) {
        String sql = "SELECT id, item_name, is_required, requires_file_upload FROM document_checklist_items WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new DocumentChecklistItem(
                    rs.getInt("id"),
                    rs.getString("item_name"),
                    rs.getBoolean("is_required"),
                    rs.getBoolean("requires_file_upload")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting checklist item by ID: " + e.getMessage());
        }
        return null;
    }

    private static boolean updateDocumentChecklistItemInCentralDb(DocumentChecklistItem item, Connection targetConn) throws SQLException {
        String updateSql = "UPDATE document_checklist_items SET item_name = ?, is_required = ?, requires_file_upload = ? WHERE id = ?";
        try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
            pstmt.setString(1, item.getItemName());
            pstmt.setBoolean(2, item.isRequired());
            pstmt.setBoolean(3, item.requiresFileUpload());
            pstmt.setInt(4, item.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) { System.out.println("Changelog: Updated full checklist item " + item.getId() + " in central DB."); return true; }
            System.out.println("Changelog: Checklist item " + item.getId() + " not found in central DB for update. Attempting INSERT instead.");
            return insertDocumentChecklistItemIntoCentralDb(item, targetConn);
        }
    }

    private static boolean insertDocumentChecklistItemIntoCentralDb(DocumentChecklistItem item, Connection targetConn) throws SQLException {
        String insertSql = "INSERT INTO document_checklist_items (id, item_name, is_required, requires_file_upload) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
            pstmt.setInt(1, item.getId());
            pstmt.setString(2, item.getItemName());
            pstmt.setBoolean(3, item.isRequired());
            pstmt.setBoolean(4, item.requiresFileUpload());
            pstmt.executeUpdate();
            System.out.println("Changelog: Inserted full checklist item " + item.getId() + " into central DB.");
            return true;
        }
    }

    private static boolean syncPeer(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            Peer localPeer = getPeerById(recordId);
            if (localPeer == null) { System.err.println("Changelog: Local peer " + recordId + " not found for INSERT. Skipping."); return false; }
            String insertSql = "INSERT INTO peers (id, ip_address, port, last_sync_time, is_trusted, status) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localPeer.getId());
                pstmt.setString(2, localPeer.getIpAddress());
                pstmt.setInt(3, localPeer.getPort());
                pstmt.setObject(4, localPeer.getLastSyncTime());
                pstmt.setBoolean(5, localPeer.isTrusted());
                pstmt.setString(6, localPeer.getStatus());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted peer " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: Peer " + recordId + " already exists in central DB. Attempting UPDATE instead.");
                    return updatePeerInCentralDb(localPeer, targetConn);
                }
                throw e;
            }
        } else if ("UPDATE".equals(changeType)) {
            Peer localPeer = getPeerById(recordId);
            if (localPeer == null) { System.err.println("Changelog: Local peer " + recordId + " not found for UPDATE. Skipping."); return false; }
            return updatePeerInCentralDb(localPeer, targetConn);
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM peers WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted peer " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    // Helper to get Peer by ID for logging purposes
    private static Peer getPeerById(int id) {
        String sql = "SELECT id, ip_address, port, last_sync_time, is_trusted, status FROM peers WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Peer(
                    rs.getInt("id"),
                    rs.getString("ip_address"),
                    rs.getInt("port"),
                    rs.getObject("last_sync_time", LocalDateTime.class),
                    rs.getBoolean("is_trusted"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting peer by ID: " + e.getMessage());
        }
        return null;
    }

    private static boolean updatePeerInCentralDb(Peer peer, Connection targetConn) throws SQLException {
        String updateSql = "UPDATE peers SET ip_address = ?, port = ?, last_sync_time = ?, is_trusted = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
            pstmt.setString(1, peer.getIpAddress());
            pstmt.setInt(2, peer.getPort());
            pstmt.setObject(3, peer.getLastSyncTime());
            pstmt.setBoolean(4, peer.isTrusted());
            pstmt.setString(5, peer.getStatus());
            pstmt.setInt(6, peer.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) { System.out.println("Changelog: Updated full peer " + peer.getId() + " in central DB."); return true; }
            System.out.println("Changelog: Peer " + peer.getId() + " not found in central DB for update. Attempting INSERT instead.");
            return insertPeerIntoCentralDb(peer, targetConn);
        }
    }

    private static boolean insertPeerIntoCentralDb(Peer peer, Connection targetConn) throws SQLException {
        String insertSql = "INSERT INTO peers (id, ip_address, port, last_sync_time, is_trusted, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
            pstmt.setInt(1, peer.getId());
            pstmt.setString(2, peer.getIpAddress());
            pstmt.setInt(3, peer.getPort());
            pstmt.setObject(4, peer.getLastSyncTime());
            pstmt.setBoolean(5, peer.isTrusted());
            pstmt.setString(6, peer.getStatus());
            pstmt.executeUpdate();
            System.out.println("Changelog: Inserted full peer " + peer.getId() + " into central DB.");
            return true;
        }
    }

    private static boolean syncSystemUpdate(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            SystemUpdate localUpdate = getSystemUpdateById(recordId);
            if (localUpdate == null) { System.err.println("Changelog: Local system update " + recordId + " not found for INSERT. Skipping."); return false; }
            String insertSql = "INSERT INTO system_updates (id, version, title, message, created_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localUpdate.getId());
                pstmt.setString(2, localUpdate.getVersion());
                pstmt.setString(3, localUpdate.getTitle());
                pstmt.setString(4, localUpdate.getMessage());
                pstmt.setObject(5, localUpdate.getCreatedAt());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted system update " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: System update " + recordId + " already exists in central DB. Attempting UPDATE instead.");
                    return updateSystemUpdateInCentralDb(localUpdate, targetConn);
                }
                throw e;
            }
        } else if ("UPDATE".equals(changeType)) {
            SystemUpdate localUpdate = getSystemUpdateById(recordId);
            if (localUpdate == null) { System.err.println("Changelog: Local system update " + recordId + " not found for UPDATE. Skipping."); return false; }
            return updateSystemUpdateInCentralDb(localUpdate, targetConn);
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM system_updates WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted system update " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    private static boolean updateSystemUpdateInCentralDb(SystemUpdate update, Connection targetConn) throws SQLException {
        String updateSql = "UPDATE system_updates SET version = ?, title = ?, message = ?, created_at = ? WHERE id = ?";
        try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
            pstmt.setString(1, update.getVersion());
            pstmt.setString(2, update.getTitle());
            pstmt.setString(3, update.getMessage());
            pstmt.setObject(4, update.getCreatedAt());
            pstmt.setInt(5, update.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) { System.out.println("Changelog: Updated full system update " + update.getId() + " in central DB."); return true; }
            System.out.println("Changelog: System update " + update.getId() + " not found in central DB for update. Attempting INSERT instead.");
            return insertSystemUpdateIntoCentralDb(update, targetConn);
        }
    }

    private static boolean insertSystemUpdateIntoCentralDb(SystemUpdate update, Connection targetConn) throws SQLException {
        String insertSql = "INSERT INTO system_updates (id, version, title, message, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
            pstmt.setInt(1, update.getId());
            pstmt.setString(2, update.getVersion());
            pstmt.setString(3, update.getTitle());
            pstmt.setString(4, update.getMessage());
            pstmt.setObject(5, update.getCreatedAt());
            pstmt.executeUpdate();
            System.out.println("Changelog: Inserted full system update " + update.getId() + " into central DB.");
            return true;
        }
    }

    private static boolean syncMessageTemplate(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            MessageTemplate localTemplate = getMessageTemplateById(recordId);
            if (localTemplate == null) { System.err.println("Changelog: Local message template " + recordId + " not found for INSERT. Skipping."); return false; }
            String insertSql = "INSERT INTO message_templates (id, template_name, subject, body, type) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localTemplate.getId());
                pstmt.setString(2, localTemplate.getTemplateName());
                pstmt.setString(3, localTemplate.getSubject());
                pstmt.setString(4, localTemplate.getBody());
                pstmt.setString(5, localTemplate.getType());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted message template " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: Message template " + recordId + " already exists in central DB. Attempting UPDATE instead.");
                    return updateMessageTemplateInCentralDb(localTemplate, targetConn);
                }
                throw e;
            }
        } else if ("UPDATE".equals(changeType)) {
            MessageTemplate localTemplate = getMessageTemplateById(recordId);
            if (localTemplate == null) { System.err.println("Changelog: Local message template " + recordId + " not found for UPDATE. Skipping."); return false; }
            return updateMessageTemplateInCentralDb(localTemplate, targetConn);
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM message_templates WHERE id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted message template " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    // Helper to get MessageTemplate by ID for logging purposes
    private static MessageTemplate getMessageTemplateById(int id) {
        String sql = "SELECT id, template_name, subject, body, type FROM message_templates WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new MessageTemplate(
                    rs.getInt("id"),
                    rs.getString("template_name"),
                    rs.getString("subject"),
                    rs.getString("body"),
                    rs.getString("type")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting message template by ID: " + e.getMessage());
        }
        return null;
    }

    private static boolean updateMessageTemplateInCentralDb(MessageTemplate template, Connection targetConn) throws SQLException {
        String updateSql = "UPDATE message_templates SET template_name = ?, subject = ?, body = ?, type = ? WHERE id = ?";
        try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
            pstmt.setString(1, template.getTemplateName());
            pstmt.setString(2, template.getSubject());
            pstmt.setString(3, template.getBody());
            pstmt.setString(4, template.getType());
            pstmt.setInt(5, template.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) { System.out.println("Changelog: Updated full message template " + template.getId() + " in central DB."); return true; }
            System.out.println("Changelog: Message template " + template.getId() + " not found in central DB for update. Attempting INSERT instead.");
            return insertMessageTemplateIntoCentralDb(template, targetConn);
        }
    }

    private static boolean insertMessageTemplateIntoCentralDb(MessageTemplate template, Connection targetConn) throws SQLException {
        String insertSql = "INSERT INTO message_templates (id, template_name, subject, body, type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
            pstmt.setInt(1, template.getId());
            pstmt.setString(2, template.getTemplateName());
            pstmt.setString(3, template.getSubject());
            pstmt.setString(4, template.getBody());
            pstmt.setString(5, template.getType());
            pstmt.executeUpdate();
            System.out.println("Changelog: Inserted full message template " + template.getId() + " into central DB.");
            return true;
        }
    }

    private static boolean syncMessageLog(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId();
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType)) {
            MessageLog localLog = getMessageLogById(recordId);
            if (localLog == null) { System.err.println("Changelog: Local message log " + recordId + " not found for INSERT. Skipping."); return false; }
            String insertSql = "INSERT INTO message_logs (id, timestamp, recipient, message_type, subject, message, status, details) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                pstmt.setInt(1, localLog.getId());
                pstmt.setObject(2, localLog.getTimestamp());
                pstmt.setString(3, localLog.getRecipient());
                pstmt.setString(4, localLog.getMessageType());
                pstmt.setString(5, localLog.getSubject());
                pstmt.setString(6, localLog.getMessage());
                pstmt.setString(7, localLog.getStatus());
                pstmt.setString(8, localLog.getDetails());
                pstmt.executeUpdate();
                System.out.println("Changelog: Inserted message log " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry") || e.getSQLState().startsWith("23")) {
                    System.out.println("Changelog: Message log " + recordId + " already exists in central DB. Skipping INSERT.");
                    return true; // Message logs are typically append-only, so if it exists, it's synced.
                }
                throw e;
            }
        }
        // Message logs are typically append-only, updates and deletes are not usually performed.
        return false;
    }

    // Helper to get MessageLog by ID for logging purposes
    private static MessageLog getMessageLogById(int id) {
        String sql = "SELECT id, timestamp, recipient, message_type, subject, message, status, details FROM message_logs WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new MessageLog(
                    rs.getInt("id"),
                    rs.getObject("timestamp", LocalDateTime.class),
                    rs.getString("recipient"),
                    rs.getString("message_type"),
                    rs.getString("subject"),
                    rs.getString("message"),
                    rs.getString("status"),
                    rs.getString("details")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting message log by ID: " + e.getMessage());
        }
        return null;
    }

    private static boolean syncUserPreference(ChangelogEntry entry, Connection targetConn) throws SQLException {
        int recordId = entry.getRecordId(); // user_id is the recordId for user_preferences
        String changeType = entry.getChangeType();

        if ("INSERT".equals(changeType) || "UPDATE".equals(changeType)) {
            UserPreference localPreferences = getUserPreferences(recordId);
            if (localPreferences == null) { System.err.println("Changelog: Local user preferences for user " + recordId + " not found. Skipping."); return false; }

            String upsertSql;
            // SQLite's ON CONFLICT is used for local DB. For central DB, we assume MySQL-like UPSERT.
            // If the central DB is also SQLite, this will still work.
            upsertSql = "INSERT INTO user_preferences (user_id, email_notifications_enabled, sms_notifications_enabled, last_seen_update_id) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE email_notifications_enabled = ?, sms_notifications_enabled = ?, last_seen_update_id = ?";
            

            try (PreparedStatement pstmt = targetConn.prepareStatement(upsertSql)) {
                pstmt.setInt(1, localPreferences.getUserId());
                pstmt.setBoolean(2, localPreferences.isEmailNotificationsEnabled());
                pstmt.setBoolean(3, localPreferences.isSmsNotificationsEnabled());
                pstmt.setInt(4, localPreferences.getLastSeenUpdateId());
                pstmt.setBoolean(5, localPreferences.isEmailNotificationsEnabled());
                pstmt.setBoolean(6, localPreferences.isSmsNotificationsEnabled());
                pstmt.setInt(7, localPreferences.getLastSeenUpdateId());
                pstmt.executeUpdate();
                System.out.println("Changelog: Upserted user preferences for user " + recordId + " into central DB.");
                return true;
            } catch (SQLException e) {
                System.err.println("Error upserting user preferences: " + e.getMessage());
                throw e;
            }
        } else if ("DELETE".equals(changeType)) {
            String deleteSql = "DELETE FROM user_preferences WHERE user_id = ?";
            try (PreparedStatement pstmt = targetConn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, recordId);
                pstmt.executeUpdate();
                System.out.println("Changelog: Deleted user preferences for user " + recordId + " from central DB.");
                return true;
            }
        }
        return false;
    }

    // --- NEW: Central to Local DB Synchronization Methods ---

    /**
     * Fetches all users from the central database.
     * @param centralConn The connection to the central database.
     * @return A list of User objects from the central database.
     * @throws SQLException if a database access error occurs.
     */
    public static List<User> getAllUsersFromCentralDb(Connection centralConn) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, contact, role FROM users ORDER BY id";
        try (Statement stmt = centralConn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("contact"),
                    rs.getString("role")
                ));
            }
        }
        return users;
    }

    /**
     * Upserts a list of users into the local SQLite database.
     * @param users The list of User objects to upsert.
     * @throws SQLException if a database access error occurs.
     */
    public static void upsertUsersIntoLocalDb(List<User> users) throws SQLException {
        Connection localConn = getActiveConnection();
        localConn.setAutoCommit(false); // Start transaction for local DB
        String upsertSql = "INSERT INTO users (id, name, email, password, role, contact) VALUES (?, ?, ?, ?, ?, ?) " +
                           "ON CONFLICT(id) DO UPDATE SET name = EXCLUDED.name, email = EXCLUDED.email, password = EXCLUDED.password, role = EXCLUDED.role, contact = EXCLUDED.contact";
        try (PreparedStatement pstmt = localConn.prepareStatement(upsertSql)) {
            for (User user : users) {
                pstmt.setInt(1, user.getId());
                pstmt.setString(2, user.getName());
                pstmt.setString(3, user.getEmail());
                pstmt.setString(4, "password"); // Passwords are not synced directly, use a placeholder or re-hash
                pstmt.setString(5, user.getRole());
                pstmt.setString(6, user.getContact());
                pstmt.executeUpdate();
            }
            localConn.commit();
            System.out.println("Upserted " + users.size() + " users into local SQLite from central DB.");
        } catch (SQLException e) {
            localConn.rollback();
            throw e;
        } finally {
            localConn.setAutoCommit(true);
        }
    }

    /**
     * Fetches all plans from the central database.
     * @param centralConn The connection to the central database.
     * @return A list of Plan objects from the central database.
     * @throws SQLException if a database access error occurs.
     */
    public static List<Plan> getAllPlansFromCentralDb(Connection centralConn) throws SQLException {
        List<Plan> plans = new ArrayList<>();
        String sql = "SELECT id, applicant_name, contact, plot_no, location, date_submitted, reference_no, status, remarks FROM plans ORDER BY id";
        try (Statement stmt = centralConn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                plans.add(new Plan(
                    rs.getInt("id"),
                    rs.getString("applicant_name"),
                    rs.getString("contact"),
                    rs.getString("plot_no"),
                    rs.getString("location"),
                    rs.getObject("date_submitted", LocalDate.class),
                    rs.getString("reference_no"),
                    rs.getString("status"),
                    rs.getString("remarks")
                ));
            }
        }
        return plans;
    }

    /**
     * Upserts a list of plans into the local SQLite database.
     * @param plans The list of Plan objects to upsert.
     * @throws SQLException if a database access error occurs.
     */
    public static void upsertPlansIntoLocalDb(List<Plan> plans) throws SQLException {
        Connection localConn = getActiveConnection();
        localConn.setAutoCommit(false); // Start transaction for local DB
        String upsertSql = "INSERT INTO plans (id, applicant_name, contact, plot_no, location, date_submitted, reference_no, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                           "ON CONFLICT(id) DO UPDATE SET applicant_name = EXCLUDED.applicant_name, contact = EXCLUDED.contact, plot_no = EXCLUDED.plot_no, location = EXCLUDED.location, date_submitted = EXCLUDED.date_submitted, reference_no = EXCLUDED.reference_no, status = EXCLUDED.status, remarks = EXCLUDED.remarks";
        try (PreparedStatement pstmt = localConn.prepareStatement(upsertSql)) {
            for (Plan plan : plans) {
                pstmt.setInt(1, plan.getId());
                pstmt.setString(2, plan.getApplicantName());
                pstmt.setString(3, plan.getContact());
                pstmt.setString(4, plan.getPlotNo());
                pstmt.setString(5, plan.getLocation());
                pstmt.setObject(6, plan.getDateSubmitted());
                pstmt.setString(7, plan.getReferenceNo());
                pstmt.setString(8, plan.getStatus());
                pstmt.setString(9, plan.getRemarks());
                pstmt.executeUpdate();
            }
            localConn.commit();
            System.out.println("Upserted " + plans.size() + " plans into local SQLite from central DB.");
        } catch (SQLException e) {
            localConn.rollback();
            throw e;
        } finally {
            localConn.setAutoCommit(true);
        }
    }
}