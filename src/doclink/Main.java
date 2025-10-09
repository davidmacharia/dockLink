package doclink;


import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Always run Swing UI inside Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Use system look & feel for modern UI (Windows/Mac/Linux)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Could not set LookAndFeel: " + e.getMessage());
            }

            // Initialize the database and ensure demo user exists
            Database.insertDemoUser();

            // Launch the main app window
            AppFrame app = new AppFrame();
            app.showFrame("DocLink Login");
            app.showLogo();
            app.showLogin();
        });
    }
}
