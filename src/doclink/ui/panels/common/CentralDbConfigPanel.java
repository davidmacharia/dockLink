package doclink.ui.panels.common;

import doclink.AppConfig;
import doclink.Database;
import doclink.ui.Dashboard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class CentralDbConfigPanel extends JPanel implements Dashboard.Refreshable {
    private JTextField centralDbUrlInputField; 
    private JLabel currentConfiguredUrlLabel;
    private JButton saveDbConfigButton;
    private JButton testDbConnectionButton;
    private JButton clearDbConfigButton;
    private Consumer<String> logConsumer;

    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public CentralDbConfigPanel(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE); 
        setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel dbConfigTitle = new JLabel("Central Database Configuration:");
        dbConfigTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        dbConfigTitle.setForeground(DARK_NAVY);
        add(dbConfigTitle, gbc);

        gbc.gridy++;
        add(new JLabel("Current Configured URL:"), gbc);
        gbc.gridy++;
        currentConfiguredUrlLabel = new JLabel("N/A");
        currentConfiguredUrlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        add(currentConfiguredUrlLabel, gbc);

        gbc.gridy++;
        add(new JLabel("New Central DB URL (JDBC format):"), gbc);
        gbc.gridy++;
        centralDbUrlInputField = new JTextField(50);
        centralDbUrlInputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        centralDbUrlInputField.putClientProperty("JTextField.placeholderText", "e.g., jdbc:mysql://host:port/database?user=u&password=p");
        add(centralDbUrlInputField, gbc);

        gbc.gridy++;
        JPanel dbButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        dbButtonsPanel.setOpaque(false);

        saveDbConfigButton = createStyledButton("Save Configuration", new Color(40, 167, 69)); 
        saveDbConfigButton.addActionListener(e -> saveCentralDbConfig());
        dbButtonsPanel.add(saveDbConfigButton);

        testDbConnectionButton = createStyledButton("Test Connection", new Color(0, 123, 255)); 
        testDbConnectionButton.addActionListener(e -> testCentralDbConnection());
        dbButtonsPanel.add(testDbConnectionButton);

        clearDbConfigButton = createStyledButton("Clear Configuration", new Color(255, 165, 0)); 
        clearDbConfigButton.addActionListener(e -> clearCentralDbConfig());
        dbButtonsPanel.add(clearDbConfigButton);

        add(dbButtonsPanel, gbc);

        gbc.gridy++;
        JLabel restartNote = new JLabel("<html><i>Note: Application restart is required for new database configurations to take effect.</i></html>");
        restartNote.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        restartNote.setForeground(Color.GRAY);
        add(restartNote, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0; 
        add(Box.createVerticalGlue(), gbc);

        updateCentralDbUrlDisplay();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private void logMessage(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
    }

    private void updateCentralDbUrlDisplay() {
        String currentUrl = AppConfig.getProperty(AppConfig.CENTRAL_DB_URL_KEY);
        if (currentUrl != null && !currentUrl.trim().isEmpty()) {
            currentConfiguredUrlLabel.setText(currentUrl);
            centralDbUrlInputField.setText(currentUrl); 
        } else {
            currentConfiguredUrlLabel.setText("Not configured (using local SQLite)");
            centralDbUrlInputField.setText("");
        }
    }

    private void saveCentralDbConfig() {
        String newUrl = centralDbUrlInputField.getText().trim();
        String oldUrl = AppConfig.getProperty(AppConfig.CENTRAL_DB_URL_KEY, "");

        if (newUrl.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this, "The URL field is empty. This will clear the central database configuration and the app will use local SQLite. Continue?", "Confirm Clear Configuration", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                AppConfig.removeProperty(AppConfig.CENTRAL_DB_URL_KEY);
                logMessage("Central DB URL configuration cleared.");
                
                int confirmRestart = JOptionPane.showConfirmDialog(this, 
                    "Central database configuration cleared. Restart the application for changes to take effect. Restart now?", 
                    "Restart Application", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);

                if (confirmRestart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            } else {
                return; 
            }
        } else {
            AppConfig.setProperty(AppConfig.CENTRAL_DB_URL_KEY, newUrl);
            logMessage("Central DB URL configuration saved: " + newUrl);
            
            if (!oldUrl.equals(newUrl)) {
                int confirmRestart = JOptionPane.showConfirmDialog(this, 
                    "Central database URL saved. For these changes to take full effect, the application needs to restart. Restart now?", 
                    "Restart Application", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);

                if (confirmRestart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Central database URL saved. No restart needed as URL did not change.", "Configuration Saved", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        updateCentralDbUrlDisplay();
    }

    private void testCentralDbConnection() {
        String testUrl = centralDbUrlInputField.getText().trim();
        if (testUrl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a URL to test.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        logMessage("Testing connection to: " + testUrl);
        boolean connected = Database.testConnection(testUrl);

        if (connected) {
            JOptionPane.showMessageDialog(this, "Connection successful!", "Test Result", JOptionPane.INFORMATION_MESSAGE);
            logMessage("Connection test successful.");
        } else {
            JOptionPane.showMessageDialog(this, "Connection failed. Please check the URL and ensure the database is accessible.", "Test Result", JOptionPane.ERROR_MESSAGE);
            logMessage("Connection test failed.");
        }
    }

    private void clearCentralDbConfig() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear the central database configuration? The application will use local SQLite on next restart.", "Confirm Clear", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            AppConfig.removeProperty(AppConfig.CENTRAL_DB_URL_KEY);
            logMessage("Central DB URL configuration cleared.");
            
            int confirmRestart = JOptionPane.showConfirmDialog(this, 
                "Central database configuration cleared. For these changes to take full effect, the application needs to restart. Restart now?", 
                "Restart Application", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE);

            if (confirmRestart == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        }
        updateCentralDbUrlDisplay();
    }

    @Override
    public void refreshData() {
        updateCentralDbUrlDisplay();
    }
}