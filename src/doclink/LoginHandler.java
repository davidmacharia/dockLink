package doclink;

import javax.swing.*;
import java.awt.event.*;

public class LoginHandler implements ActionListener {
    private final JTextField emailField;
    private final JPasswordField passwordField;

    public LoginHandler(JTextField emailField, JPasswordField passwordField) {
        this.emailField = emailField;
        this.passwordField = passwordField;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // --- Validation ---
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "Please fill in all fields.",
                "Missing Information",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            // --- Database verification ---
            boolean isValid = Database.validateUser(email, password);

            if (isValid) {
                // Close the login frame safely
                Object source = e.getSource();
                if (source instanceof JButton btn) {
                    JFrame loginFrame = (JFrame) SwingUtilities.getWindowAncestor(btn);
                    if (loginFrame != null) loginFrame.dispose();
                }

                // Show dashboard
                Dashboard dashboard = new Dashboard(email, password);
                dashboard.showFrame("Dashboard");
                dashboard.showDashboard();

            } else {
                JOptionPane.showMessageDialog(
                    null,
                    "Invalid email or password. Please try again.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "An error occurred while connecting to the database.\n" + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
