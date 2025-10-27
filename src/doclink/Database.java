package doclink;

import doclink.models.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane; // Added import

public class Database {
    private static final String DB_URL = "jdbc:sqlite:data/doclink.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
        }
        return conn;
    }

    public static void initializeDatabase() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "name TEXT NOT NULL," +
                         "email TEXT UNIQUE NOT NULL," +
                         "password TEXT NOT NULL," +
                         "role TEXT NOT NULL)"); // Reception, Planning, Committee, Director, Structural, Client, Admin, Blocked

            // Create plans table
            stmt.execute("CREATE TABLE IF NOT EXISTS plans (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "applicant_name TEXT NOT NULL," +
                         "contact TEXT NOT NULL," +
                         "plot_no TEXT NOT NULL," +
                         "location TEXT NOT NULL," +
                         "date_submitted TEXT NOT NULL," +
                         "reference_no TEXT UNIQUE," +
                         "status TEXT NOT NULL," + // e.g., Submitted, Awaiting Payment, Under Review (Planning), Under Review (Committee), Under Review (Director), Under Review (Structural), Approved, Rejected, Deferred
                         "remarks TEXT)");

            // Create documents table first
            stmt.execute("CREATE TABLE IF NOT EXISTS documents (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "plan_id INTEGER NOT NULL," +
                         "doc_name TEXT NOT NULL," +
                         "file_path TEXT," +
                         "is_attached BOOLEAN NOT NULL," +
                         "document_type TEXT NOT NULL DEFAULT 'Submitted'," +
                         "FOREIGN KEY (plan_id) REFERENCES plans(id))");

            // Then, add document_type column if it doesn't exist (this check is now redundant if the table is always created with it, but good for robustness)
            // However, keeping it for now as it was part of the original intent.
            if (!columnExists(conn, "documents", "document_type")) {
                stmt.execute("ALTER TABLE documents ADD COLUMN document_type TEXT DEFAULT 'Submitted'");
            }

            // Create billing table
            stmt.execute("CREATE TABLE IF NOT EXISTS billing (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "plan_id INTEGER NOT NULL," +
                         "amount REAL NOT NULL," +
                         "receipt_no TEXT UNIQUE," +
                         "date_paid TEXT," +
                         "FOREIGN KEY (plan_id) REFERENCES plans(id))");

            // Create logs table
            stmt.execute("CREATE TABLE IF NOT EXISTS logs (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "plan_id INTEGER NOT NULL," +
                         "from_role TEXT NOT NULL," +
                         "to_role TEXT NOT NULL," +
                         "action TEXT NOT NULL," +
                         "remarks TEXT," +
                         "date TEXT NOT NULL," +
                         "FOREIGN KEY (plan_id) REFERENCES plans(id))");

            // NEW: Create meetings table
            stmt.execute("CREATE TABLE IF NOT EXISTS meetings (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "title TEXT NOT NULL," +
                         "date TEXT NOT NULL," +
                         "time TEXT NOT NULL," +
                         "location TEXT NOT NULL," +
                         "agenda TEXT," +
                         "status TEXT NOT NULL)"); // Scheduled, Cancelled, Completed

            // NEW: Create document_checklist_items table
            stmt.execute("CREATE TABLE IF NOT EXISTS document_checklist_items (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "item_name TEXT UNIQUE NOT NULL," +
                         "is_required BOOLEAN NOT NULL DEFAULT 1," +
                         "requires_file_upload BOOLEAN NOT NULL DEFAULT 0)"); // NEW: Default to not requiring file upload

            // Add column if it doesn't exist (for existing databases)
            if (!columnExists(conn, "document_checklist_items", "requires_file_upload")) {
                stmt.execute("ALTER TABLE document_checklist_items ADD COLUMN requires_file_upload BOOLEAN NOT NULL DEFAULT 0");
            }

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
        // Ensure demo checklist items are added after table creation
        addDemoChecklistItems();
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getColumns(null, null, tableName, columnName);
        return rs.next();
    }

    public static void addDemoUsers() {
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(
             "INSERT OR IGNORE INTO users (name, email, password, role) VALUES (?, ?, ?, ?)")) {

            // Demo users (password is 'password' for all)
            String[][] users = {
                {"Receptionist", "reception@doclink.com", "password", "Reception"},
                {"Planning Officer", "planning@doclink.com", "password", "Planning"},
                {"Committee Member", "committee@doclink.com", "password", "Committee"},
                {"Director", "director@doclink.com", "password", "Director"},
                {"Structural Engineer", "structural@doclink.com", "password", "Structural"},
                {"Client User", "client@doclink.com", "password", "Client"},
                {"Admin User", "admin@doclink.com", "password", "Admin"},
                {"Developer User", "developer@doclink.com", "password", "Developer"} // NEW: Developer User
            };

            for (String[] user : users) {
                pstmt.setString(1, user[0]);
                pstmt.setString(2, user[1]);
                pstmt.setString(3, user[2]);
                pstmt.setString(4, user[3]);
                pstmt.executeUpdate();
            }
            System.out.println("Demo users added/ensured.");
        } catch (SQLException e) {
            System.err.println("Error adding demo users: " + e.getMessage());
        }
    }

    // NEW: Add demo checklist items
    public static void addDemoChecklistItems() {
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(
             "INSERT OR IGNORE INTO document_checklist_items (item_name, is_required, requires_file_upload) VALUES (?, ?, ?)")) {

            // Updated demo items with requires_file_upload
            Object[][] items = {
                {"Site Plan", true, true},
                {"Title Deed", true, true},
                {"Architectural Drawings", true, true},
                {"Structural Drawings", false, true}, // Optional, but requires file if provided
                {"Environmental Impact Assessment", false, false}, // Optional, no file needed (e.g., verbal confirmation)
                {"Fire Safety Report", false, false} // Optional, no file needed
            };

            for (Object[] item : items) {
                pstmt.setString(1, (String) item[0]);
                pstmt.setBoolean(2, (Boolean) item[1]);
                pstmt.setBoolean(3, (Boolean) item[2]); // NEW: Set requires_file_upload
                pstmt.executeUpdate();
            }
            System.out.println("Demo checklist items added/ensured.");
        } catch (SQLException e) {
            System.err.println("Error adding demo checklist items: " + e.getMessage());
        }
    }

    public static User authenticateUser(String email, String password) {
        String sql = "SELECT id, name, email, role FROM users WHERE email = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("role"));
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return null;
    }

    public static void addPlan(Plan plan, List<Document> documents) {
        String planSql = "INSERT INTO plans (applicant_name, contact, plot_no, location, date_submitted, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String docSql = "INSERT INTO documents (plan_id, doc_name, file_path, is_attached, document_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement planPstmt = conn.prepareStatement(planSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement docPstmt = conn.prepareStatement(docSql)) {

            conn.setAutoCommit(false); // Start transaction

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
                plan.setId(planId); // Set the generated ID back to the plan object

                for (Document doc : documents) {
                    docPstmt.setInt(1, planId);
                    docPstmt.setString(2, doc.getDocName());
                    docPstmt.setString(3, doc.getFilePath());
                    docPstmt.setBoolean(4, doc.isAttached());
                    docPstmt.setString(5, doc.getDocumentType()); // Set document type
                    docPstmt.addBatch();
                }
                docPstmt.executeBatch();
            }
            conn.commit(); // Commit transaction
            System.out.println("Plan and documents added successfully.");
        } catch (SQLException e) {
            System.err.println("Error adding plan and documents: " + e.getMessage());
            try (Connection conn = connect()) {
                if (conn != null) conn.rollback(); // Rollback on error
            } catch (SQLException ex) {
                System.err.println("Rollback error: " + ex.getMessage());
            }
        }
    }

    public static void addDocument(Document doc) {
        String sql = "INSERT INTO documents (plan_id, doc_name, file_path, is_attached, document_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    // New method to get all logs
    public static List<Log> getAllLogs() {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT id, plan_id, from_role, to_role, action, remarks, date FROM logs ORDER BY date DESC, id DESC";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
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

    // New method to get all logs for a specific plan ID
    public static List<Log> getLogsByPlanId(int planId) {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT id, plan_id, from_role, to_role, action, remarks, date FROM logs WHERE plan_id = ? ORDER BY date DESC, id DESC";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, referenceNo);
            pstmt.setInt(2, planId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Database: updatePlanReferenceNo - Plan " + planId + " reference number updated to " + referenceNo + ". Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Database: Error updating plan reference number: " + e.getMessage());
            // Check for unique constraint violation specifically
            if (e.getMessage().contains("UNIQUE constraint failed: plans.reference_no")) {
                return false; // Indicate failure due to unique constraint
            }
            return false; // General failure
        }
    }

    public static void addBilling(Billing billing) {
        String sql = "INSERT INTO billing (plan_id, amount) VALUES (?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiptNo);
            pstmt.setString(2, LocalDate.now().toString());
            pstmt.setInt(3, billingId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Billing " + billingId + " marked as paid with receipt " + receiptNo);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating billing payment: " + e.getMessage());
            if (e.getMessage().contains("UNIQUE constraint failed: billing.receipt_no")) {
                return false; // Indicate failure due to unique receipt number
            }
            return false; // General failure
        }
    }

    public static List<Plan> getPlansByApplicantEmailAndStatus(String email, String status) {
        List<Plan> plans = new ArrayList<>();
        String sql = "SELECT p.* FROM plans p JOIN users u ON p.applicant_name = u.name WHERE u.email = ? AND p.status = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    // --- Admin Panel Database Methods ---

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, role FROM users ORDER BY name";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
        }
        return users;
    }

    public static boolean addUser(String name, String email, String password, String role) {
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
            System.out.println("User '" + name + "' added successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
            // The calling UI component will handle displaying the message
            return false;
        }
    }

    public static boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    public static boolean updateUserPassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    public static boolean updateUserNameAndEmail(int userId, String newName, String newEmail) {
        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    public static boolean deletePlanAndRelatedData(int planId) {
        String deleteDocumentsSql = "DELETE FROM documents WHERE plan_id = ?";
        String deleteBillingSql = "DELETE FROM billing WHERE plan_id = ?";
        String deleteLogsSql = "DELETE FROM logs WHERE plan_id = ?";
        String deletePlanSql = "DELETE FROM plans WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmtDocs = conn.prepareStatement(deleteDocumentsSql);
             PreparedStatement pstmtBilling = conn.prepareStatement(deleteBillingSql);
             PreparedStatement pstmtLogs = conn.prepareStatement(deleteLogsSql);
             PreparedStatement pstmtPlan = conn.prepareStatement(deletePlanSql)) {

            conn.setAutoCommit(false); // Start transaction

            pstmtDocs.setInt(1, planId);
            pstmtDocs.executeUpdate();

            pstmtBilling.setInt(1, planId);
            pstmtBilling.executeUpdate();

            pstmtLogs.setInt(1, planId);
            pstmtLogs.executeUpdate();

            pstmtPlan.setInt(1, planId);
            int rowsAffected = pstmtPlan.executeUpdate();

            conn.commit(); // Commit transaction
            if (rowsAffected > 0) {
                System.out.println("Plan " + planId + " and all related data deleted successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting plan and related data: " + e.getMessage());
            try (Connection conn = connect()) {
                if (conn != null) conn.rollback(); // Rollback on error
            } catch (SQLException ex) {
                System.err.println("Rollback error: " + ex.getMessage());
            }
        }
        return false;
    }

    // --- Meeting Management Methods ---

    public static boolean addMeeting(Meeting meeting) {
        String sql = "INSERT INTO meetings (title, date, time, location, agenda, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    // --- Document Checklist Item Management Methods ---

    public static boolean addChecklistItem(String itemName, boolean isRequired, boolean requiresFileUpload) {
        String sql = "INSERT INTO document_checklist_items (item_name, is_required, requires_file_upload) VALUES (?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemName);
            pstmt.setBoolean(2, isRequired);
            pstmt.setBoolean(3, requiresFileUpload); // NEW: Set requires_file_upload
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
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(new DocumentChecklistItem(
                    rs.getInt("id"),
                    rs.getString("item_name"),
                    rs.getBoolean("is_required"),
                    rs.getBoolean("requires_file_upload") // NEW: Retrieve requires_file_upload
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all checklist items: " + e.getMessage());
        }
        return items;
    }

    public static boolean updateChecklistItem(int id, String newItemName, boolean newIsRequired, boolean newRequiresFileUpload) {
        String sql = "UPDATE document_checklist_items SET item_name = ?, is_required = ?, requires_file_upload = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newItemName);
            pstmt.setBoolean(2, newIsRequired);
            pstmt.setBoolean(3, newRequiresFileUpload); // NEW: Update requires_file_upload
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
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
}