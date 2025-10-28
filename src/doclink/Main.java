package doclink;

import doclink.ui.LoginFrame;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Ensure the database is initialized before starting the UI
        Database.initializeDatabase();
        Database.addDemoUsers();
        // Database.addDemoChecklistItems(); // Already called inside initializeDatabase
        // Database.addDemoMessageTemplates(); // Already called inside initializeDatabase

        // Add a shutdown hook to close the database connection when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Application is shutting down. Closing database connection.");
            Database.closeConnection();
        }));

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
