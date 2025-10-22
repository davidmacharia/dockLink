package doclink;

import doclink.models.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:doclink.db";

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
                         "role TEXT NOT NULL)"); // Reception, Planning, Committee, Director, Structural, Client, Admin

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
                         "document_type TEXT NOT NULL DEFAULT 'Submitted'," + // Added document_type
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

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
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
                {"Admin User", "admin@doclink.com", "password", "Admin"} // New Admin User
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

    // --- New Admin Panel Database Methods ---

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
}