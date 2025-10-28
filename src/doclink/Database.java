package doclink;

import doclink.models.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:data/doclink.db";

    private static Connection activeConnection = null;

    private Database() {}

    private static Connection createConnection(String url) throws SQLException {
        System.out.println("Attempting to connect to: " + url);
        return DriverManager.getConnection(url);
    }

    public static void initializeDatabase() {
        String centralDbUrl = AppConfig.getProperty(AppConfig.CENTRAL_DB_URL_KEY);

        try {
            if (centralDbUrl != null && !centralDbUrl.trim().isEmpty()) {
                try {
                    activeConnection = createConnection(centralDbUrl);
                    System.out.println("Successfully connected to central database.");
                } catch (SQLException e) {
                    System.err.println("Failed to connect to central database (" + centralDbUrl + "): " + e.getMessage());
                    System.out.println("Falling back to local SQLite database.");
                    activeConnection = createConnection(DB_URL);
                    System.out.println("Successfully connected to local SQLite database.");
                }
            } else {
                System.out.println("No central database URL configured. Connecting to local SQLite database.");
                activeConnection = createConnection(DB_URL);
                System.out.println("Successfully connected to local SQLite database.");
            }

            try (Statement stmt = activeConnection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "name TEXT NOT NULL," +
                             "email TEXT UNIQUE NOT NULL," +
                             "password TEXT NOT NULL," +
                             "role TEXT NOT NULL," +
                             "contact TEXT)"); // NEW: Added contact column

                if (!columnExists(activeConnection, "users", "contact")) { // Check if contact column exists
                    stmt.execute("ALTER TABLE users ADD COLUMN contact TEXT");
                }

                stmt.execute("CREATE TABLE IF NOT EXISTS plans (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "applicant_name TEXT NOT NULL," +
                             "contact TEXT NOT NULL," +
                             "plot_no TEXT NOT NULL," +
                             "location TEXT NOT NULL," +
                             "date_submitted TEXT NOT NULL," +
                             "reference_no TEXT UNIQUE," +
                             "status TEXT NOT NULL," +
                             "remarks TEXT)");

                stmt.execute("CREATE TABLE IF NOT EXISTS documents (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "plan_id INTEGER NOT NULL," +
                             "doc_name TEXT NOT NULL," +
                             "file_path TEXT," +
                             "is_attached BOOLEAN NOT NULL," +
                             "document_type TEXT NOT NULL DEFAULT 'Submitted'," +
                             "FOREIGN KEY (plan_id) REFERENCES plans(id))");

                if (!columnExists(activeConnection, "documents", "document_type")) {
                    stmt.execute("ALTER TABLE documents ADD COLUMN document_type TEXT DEFAULT 'Submitted'");
                }

                stmt.execute("CREATE TABLE IF NOT EXISTS billing (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "plan_id INTEGER NOT NULL," +
                             "amount REAL NOT NULL," +
                             "receipt_no TEXT UNIQUE," +
                             "date_paid TEXT," +
                             "FOREIGN KEY (plan_id) REFERENCES plans(id))");

                stmt.execute("CREATE TABLE IF NOT EXISTS logs (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "plan_id INTEGER NOT NULL," +
                             "from_role TEXT NOT NULL," +
                             "to_role TEXT NOT NULL," +
                             "action TEXT NOT NULL," +
                             "remarks TEXT," +
                             "date TEXT NOT NULL," +
                             "FOREIGN KEY (plan_id) REFERENCES plans(id))");

                stmt.execute("CREATE TABLE IF NOT EXISTS meetings (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "title TEXT NOT NULL," +
                             "date TEXT NOT NULL," +
                             "time TEXT NOT NULL," +
                             "location TEXT NOT NULL," +
                             "agenda TEXT," +
                             "status TEXT NOT NULL)");

                stmt.execute("CREATE TABLE IF NOT EXISTS document_checklist_items (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "item_name TEXT UNIQUE NOT NULL," +
                             "is_required BOOLEAN NOT NULL DEFAULT 1," +
                             "requires_file_upload BOOLEAN NOT NULL DEFAULT 0)");

                if (!columnExists(activeConnection, "document_checklist_items", "requires_file_upload")) {
                    stmt.execute("ALTER TABLE document_checklist_items ADD COLUMN requires_file_upload BOOLEAN NOT NULL DEFAULT 0");
                }

                stmt.execute("CREATE TABLE IF NOT EXISTS peers (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "ip_address TEXT NOT NULL," +
                             "port INTEGER NOT NULL," +
                             "last_sync_time TEXT," +
                             "is_trusted BOOLEAN NOT NULL DEFAULT 0," +
                             "status TEXT NOT NULL DEFAULT 'Unknown'," +
                             "UNIQUE(ip_address, port))");

                stmt.execute("CREATE TABLE IF NOT EXISTS system_updates (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "version TEXT," +
                             "title TEXT NOT NULL," +
                             "message TEXT NOT NULL," +
                             "created_at TEXT NOT NULL)");

                stmt.execute("CREATE TABLE IF NOT EXISTS message_templates (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "template_name TEXT UNIQUE NOT NULL," +
                             "subject TEXT," +
                             "body TEXT NOT NULL," +
                             "type TEXT NOT NULL)");

                stmt.execute("CREATE TABLE IF NOT EXISTS message_logs (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "timestamp TEXT NOT NULL," +
                             "recipient TEXT NOT NULL," +
                             "message_type TEXT NOT NULL," +
                             "subject TEXT," +
                             "message TEXT NOT NULL," +
                             "status TEXT NOT NULL," +
                             "details TEXT)");

                stmt.execute("CREATE TABLE IF NOT EXISTS user_preferences (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "user_id INTEGER UNIQUE NOT NULL," +
                             "email_notifications_enabled BOOLEAN NOT NULL DEFAULT 1," +
                             "sms_notifications_enabled BOOLEAN NOT NULL DEFAULT 1," +
                             "FOREIGN KEY (user_id) REFERENCES users(id))");

                System.out.println("Database schema initialized successfully.");
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

    public static boolean testConnection(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try (Connection testConn = DriverManager.getConnection(url)) {
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
        // Removed try-with-resources for 'conn' to prevent premature closing of the activeConnection
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(
             "INSERT OR IGNORE INTO users (name, email, password, role, contact) VALUES (?, ?, ?, ?, ?)")) { // NEW: Added contact

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
                pstmt.setString(5, user[4]); // NEW: Set contact
                pstmt.executeUpdate();
            }
            System.out.println("Demo users added/ensured.");
        } catch (SQLException e) {
            System.err.println("Error adding demo users: " + e.getMessage());
        }
    }

    public static void addDemoChecklistItems() {
        // Removed try-with-resources for 'conn' to prevent premature closing of the activeConnection
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(
             "INSERT OR IGNORE INTO document_checklist_items (item_name, is_required, requires_file_upload) VALUES (?, ?, ?)")) {

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
        // Removed try-with-resources for 'conn' to prevent premature closing of the activeConnection
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(
             "INSERT OR IGNORE INTO message_templates (template_name, subject, body, type) VALUES (?, ?, ?, ?)")) {

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
        String sql = "SELECT id, name, email, contact, role FROM users WHERE email = ? AND password = ?"; // NEW: Added contact
        Connection conn = getActiveConnection(); // Get the active connection outside try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role"); // Get the role
                System.out.println("Database: Authenticated user role: '" + role + "'"); // Debug print
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("contact"), role); // NEW: Added contact
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return null;
    }

    public static void addPlan(Plan plan, List<Document> documents) {
        String planSql = "INSERT INTO plans (applicant_name, contact, plot_no, location, date_submitted, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String docSql = "INSERT INTO documents (plan_id, doc_name, file_path, is_attached, document_type) VALUES (?, ?, ?, ?, ?)";
        Connection conn = getActiveConnection(); // Get the active connection
        try (PreparedStatement planPstmt = conn.prepareStatement(planSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement docPstmt = conn.prepareStatement(docSql)) {

            conn.setAutoCommit(false);

            planPstmt.setString(1, plan.getApplicantName());
            planPstmt.setString(2, plan.getContact());
            planPstmt.setString(3, plan.getPlotNo());
            planPstmt.setString(4, plan.getLocation());
            planPstmt.setString(5, plan.getDateSubmitted().toString());
            planPstmt.setString(6, plan.getStatus());
            planPstmt.setString(7, plan.getRemarks());
            planPstmt.executeUpdate();

            ResultSet rs = planPstmt.getGeneratedKeys();
            if (rs.next()) {
                int planId = rs.getInt(1);
                plan.setId(planId);

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
        String sql = "UPDATE plans SET status = ?, remarks = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, remarks);
            pstmt.setInt(3, planId);
            pstmt.executeUpdate();
            System.out.println("Plan " + planId + " status updated to " + newStatus);
        } catch (SQLException e) {
            System.err.println("Error updating plan status: " + e.getMessage());
        }
    }

    public static void addLog(Log log) {
        String sql = "INSERT INTO logs (plan_id, from_role, to_role, action, remarks, date) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, log.getPlanId());
            pstmt.setString(2, log.getFromRole());
            pstmt.setString(3, log.getToRole());
            pstmt.setString(4, log.getAction());
            pstmt.setString(5, log.getRemarks());
            pstmt.setString(6, log.getDate().toString());
            pstmt.executeUpdate();
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
                    LocalDate.parse(rs.getString("date"))
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
                    LocalDate.parse(rs.getString("date"))
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
                    LocalDate.parse(rs.getString("date"))
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
                    LocalDate.parse(rs.getString("date_submitted")),
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
                    LocalDate.parse(rs.getString("date_submitted")),
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
                    LocalDate.parse(rs.getString("date_submitted")),
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
        String sql = "UPDATE plans SET reference_no = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, referenceNo);
            pstmt.setInt(2, planId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Database: updatePlanReferenceNo - Plan " + planId + " reference number updated to " + referenceNo + ". Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Database: Error updating plan reference number: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: plans.reference_no")) {
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
                    rs.getString("date_paid") != null ? LocalDate.parse(rs.getString("date_paid")) : null
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting billing by plan ID: " + e.getMessage());
        }
        return null;
    }

    public static boolean updateBillingPayment(int billingId, String receiptNo) {
        String sql = "UPDATE billing SET receipt_no = ?, date_paid = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, receiptNo);
            pstmt.setString(2, LocalDate.now().toString());
            pstmt.setInt(3, billingId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Billing " + billingId + " marked as paid with receipt " + receiptNo);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating billing payment: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: billing.receipt_no")) {
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
                    LocalDate.parse(rs.getString("date_submitted")),
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
                    LocalDate.parse(rs.getString("date_submitted")),
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
        String sql = "SELECT id, name, email, contact, role FROM users ORDER BY name"; // NEW: Added contact
        try (Statement stmt = getActiveConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("contact"), // NEW: Added contact
                    rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
        }
        return users;
    }

    public static User getUserById(int userId) {
        String sql = "SELECT id, name, email, contact, role FROM users WHERE id = ?"; // NEW: Added contact
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("contact"), rs.getString("role")); // NEW: Added contact
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
        }
        return null;
    }

    public static User getUserByEmail(String email) {
        String sql = "SELECT id, name, email, contact, role FROM users WHERE email = ?"; // NEW: Added contact
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("contact"), rs.getString("role")); // NEW: Added contact
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
        }
        return null;
    }

    public static List<User> getUsersByRole(String role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, contact, role FROM users WHERE role = ? ORDER BY name"; // NEW: Added contact
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("contact"), // NEW: Added contact
                    rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting users by role: " + e.getMessage());
        }
        return users;
    }

    public static boolean addUser(String name, String email, String password, String role) {
        // Default contact to empty string if not provided
        return addUser(name, email, password, role, ""); 
    }

    public static boolean addUser(String name, String email, String password, String role, String contact) { // NEW: Overloaded method with contact
        String sql = "INSERT INTO users (name, email, password, role, contact) VALUES (?, ?, ?, ?, ?)"; // NEW: Added contact
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            pstmt.setString(5, contact); // NEW: Set contact
            pstmt.executeUpdate();
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
                System.out.println("User " + userId + " deleted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
        return false;
    }

    public static boolean updateUserRole(int userId, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newRole);
            pstmt.setInt(2, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User " + userId + " role updated to " + newRole);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user role: " + e.getMessage());
        }
        return false;
    }

    public static boolean updateUserNameAndEmail(int userId, String newName, String newEmail) {
        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, newEmail);
            pstmt.setInt(3, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User " + userId + " name and email updated.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user name and email: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: users.email")) {
                JOptionPane.showMessageDialog(null, "Error: The email '" + newEmail + "' is already in use by another user.", "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
        return false;
    }

    public static boolean updateUserContact(int userId, String newContact) { // NEW: Method to update contact
        String sql = "UPDATE users SET contact = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newContact);
            pstmt.setInt(2, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User " + userId + " contact updated.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user contact: " + e.getMessage());
            return false;
        }
        return false; // Added missing return statement
    }

    public static boolean updateUserPassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User " + userId + " password updated.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user password: " + e.getMessage());
        }
        return false;
    }

    public static boolean deletePlanAndRelatedData(int planId) {
        String deleteDocumentsSql = "DELETE FROM documents WHERE plan_id = ?";
        String deleteBillingSql = "DELETE FROM billing WHERE plan_id = ?";
        String deleteLogsSql = "DELETE FROM logs WHERE plan_id = ?";
        String deletePlanSql = "DELETE FROM plans WHERE id = ?";

        Connection conn = getActiveConnection(); // Get the active connection
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
            pstmt.setString(2, meeting.getDate().toString());
            pstmt.setString(3, meeting.getTime());
            pstmt.setString(4, meeting.getLocation());
            pstmt.setString(5, meeting.getAgenda());
            pstmt.setString(6, meeting.getStatus());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                meeting.setId(rs.getInt(1));
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
                    LocalDate.parse(rs.getString("date")),
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
                    LocalDate.parse(rs.getString("date")),
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
        String sql = "UPDATE meetings SET title = ?, date = ?, time = ?, location = ?, agenda = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, meeting.getTitle());
            pstmt.setString(2, meeting.getDate().toString());
            pstmt.setString(3, meeting.getTime());
            pstmt.setString(4, meeting.getLocation());
            pstmt.setString(5, meeting.getAgenda());
            pstmt.setString(6, meeting.getStatus());
            pstmt.setInt(7, meeting.getId());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
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
            pstmt.setString(1, LocalDate.now().toString());
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
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, itemName);
            pstmt.setBoolean(2, isRequired);
            pstmt.setBoolean(3, requiresFileUpload);
            pstmt.executeUpdate();
            System.out.println("Checklist item '" + itemName + "' added successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding checklist item: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: document_checklist_items.item_name")) {
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
        String sql = "UPDATE document_checklist_items SET item_name = ?, is_required = ?, requires_file_upload = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, newItemName);
            pstmt.setBoolean(2, newIsRequired);
            pstmt.setBoolean(3, newRequiresFileUpload);
            pstmt.setInt(4, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Checklist item " + id + " updated successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating checklist item: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: document_checklist_items.item_name")) {
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
                    rs.getString("last_sync_time") != null ? LocalDateTime.parse(rs.getString("last_sync_time")) : null,
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
        String sql = "UPDATE peers SET ip_address = ?, port = ?, last_sync_time = ?, is_trusted = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, peer.getIpAddress());
            pstmt.setInt(2, peer.getPort());
            pstmt.setString(3, peer.getLastSyncTime() != null ? peer.getLastSyncTime().toString() : null);
            pstmt.setBoolean(4, peer.isTrusted());
            pstmt.setString(5, peer.getStatus());
            pstmt.setInt(6, peer.getId());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating peer: " + e.getMessage());
            return false;
        }
    }

    public static boolean deletePeer(int peerId) {
        String sql = "DELETE FROM peers WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, peerId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting peer: " + e.getMessage());
            return false;
        }
    }

    public static boolean addSystemUpdate(SystemUpdate update) {
        String sql = "INSERT INTO system_updates (version, title, message, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, update.getVersion());
            pstmt.setString(2, update.getTitle());
            pstmt.setString(3, update.getMessage());
            pstmt.setString(4, update.getCreatedAt().toString());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                update.setId(rs.getInt(1));
            }
            System.out.println("System update '" + update.getTitle() + "' added.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding system update: " + e.getMessage());
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
                    LocalDateTime.parse(rs.getString("created_at"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all system updates: " + e.getMessage());
        }
        return updates;
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
        } catch (SQLException e) {
            System.err.println("Error getting all message templates: " + e.getMessage());
        }
        return templates;
    }

    public static boolean updateMessageTemplate(MessageTemplate template) {
        String sql = "UPDATE message_templates SET subject = ?, body = ?, type = ? WHERE id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setString(1, template.getSubject());
            pstmt.setString(2, template.getBody());
            pstmt.setString(3, template.getType());
            pstmt.setInt(4, template.getId());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating message template: " + e.getMessage());
            return false;
        }
    }

    public static boolean addMessageLog(MessageLog log) {
        String sql = "INSERT INTO message_logs (timestamp, recipient, message_type, subject, message, status, details) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, log.getTimestamp().toString());
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
                    LocalDateTime.parse(rs.getString("timestamp")),
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
        String sql = "SELECT id, user_id, email_notifications_enabled, sms_notifications_enabled FROM user_preferences WHERE user_id = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new UserPreference(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getBoolean("email_notifications_enabled"),
                    rs.getBoolean("sms_notifications_enabled")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting user preferences for user ID " + userId + ": " + e.getMessage());
        }
        return null;
    }

    public static boolean saveUserPreferences(UserPreference preferences) {
        String sql = "INSERT INTO user_preferences (user_id, email_notifications_enabled, sms_notifications_enabled) VALUES (?, ?, ?) " +
                     "ON CONFLICT(user_id) DO UPDATE SET email_notifications_enabled = ?, sms_notifications_enabled = ?";
        try (PreparedStatement pstmt = getActiveConnection().prepareStatement(sql)) {
            pstmt.setInt(1, preferences.getUserId());
            pstmt.setBoolean(2, preferences.isEmailNotificationsEnabled());
            pstmt.setBoolean(3, preferences.isSmsNotificationsEnabled());
            pstmt.setBoolean(4, preferences.isEmailNotificationsEnabled());
            pstmt.setBoolean(5, preferences.isSmsNotificationsEnabled());
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("User preferences saved for user " + preferences.getUserId() + ". Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error saving user preferences: " + e.getMessage());
            return false;
        }
    }
}