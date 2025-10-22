package doclink.ui;

import doclink.Database;
import doclink.models.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JCheckBox showPasswordCheckBox; // New checkbox

    public LoginFrame() {
        setTitle("DocLink - Login");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 247, 250)); // Very light grey

        // Left panel for logo (dark blue)
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(new Color(26, 35, 126)); // Dark navy
        logoPanel.setPreferredSize(new Dimension(300, 0));
        logoPanel.setLayout(new GridBagLayout()); // Center content
        JLabel logoLabel = new JLabel("DocLink");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        logoLabel.setForeground(Color.WHITE);
        logoPanel.add(logoLabel);
        mainPanel.add(logoPanel, BorderLayout.WEST);

        // Right panel for login form (light grey)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(245, 247, 250)); // Very light grey
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Login to DocLink");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(26, 35, 126)); // Dark navy
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1; // Reset gridwidth

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(emailLabel, gbc);

        emailField = new JTextField(20);
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 1;
        formPanel.add(emailField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 2;
        formPanel.add(passwordField, gbc);

        // New: Show Password Checkbox
        showPasswordCheckBox = new JCheckBox("Show Password");
        showPasswordCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPasswordCheckBox.setBackground(new Color(245, 247, 250));
        showPasswordCheckBox.setFocusPainted(false);
        gbc.gridx = 1; // Place it in the second column
        gbc.gridy = 3; // Below the password field
        gbc.gridwidth = 1; // Only one column wide
        gbc.anchor = GridBagConstraints.EAST; // Align to the right
        gbc.insets = new Insets(0, 10, 10, 10); // Adjust top padding to be closer to password field
        formPanel.add(showPasswordCheckBox, gbc);

        showPasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (showPasswordCheckBox.isSelected()) {
                    passwordField.setEchoChar((char) 0); // Show characters
                } else {
                    passwordField.setEchoChar('*'); // Hide characters
                }
            }
        });

        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginButton.setBackground(new Color(0, 123, 255)); // Primary blue
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setOpaque(true);
        loginButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        loginButton.putClientProperty("JButton.buttonType", "roundRect"); // For some L&Fs to round corners
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(new Color(0, 100, 200)); // Darker blue on hover
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(new Color(0, 123, 255));
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 4; // Adjusted gridy to accommodate the new checkbox
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10); // Top padding
        gbc.anchor = GridBagConstraints.CENTER; // Center the button
        formPanel.add(loginButton, gbc);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptLogin();
            }
        });

        mainPanel.add(formPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    private void attemptLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        User user = Database.authenticateUser(email, password);

        if (user != null) {
            if (user.getRole().equals("Blocked")) {
                JOptionPane.showMessageDialog(this, "Your account has been blocked. Please contact an administrator.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return; // Prevent login for blocked users
            }
            JOptionPane.showMessageDialog(this, "Login successful! Welcome, " + user.getName(), "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose(); // Close login window
            Dashboard dashboard = new Dashboard(user);
            dashboard.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid email or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}