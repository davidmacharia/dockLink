package doclink;

import doclink.ui.LoginFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Ensure the database is initialized before starting the UI
        Database.initializeDatabase();
        Database.addDemoUsers();

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}