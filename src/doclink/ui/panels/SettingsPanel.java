package doclink.ui.panels;

import doclink.Database;
import doclink.models.User;
import doclink.models.UserPreference;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingsPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Retain for interface compatibility

    private JTextField nameField;
    private JTextField emailField;
    private JTextField contactField; // NEW: Added contact field
    private JLabel roleLabel;
    private JPasswordField currentPasswordField; // New field for current password
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton updateProfileButton;
    private JButton changePasswordButton;

    // NEW: Notification Preferences
    private JCheckBox emailNotificationsCheckBox;
    private JCheckBox smsNotificationsCheckBox;
    private JButton saveNotificationPreferencesButton;

    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public SettingsPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel; // Keep reference for interface
        setLayout(new GridBagLayout()); // Main panel uses GridBagLayout to arrange sub-panels
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        createFormComponents();
        loadUserDetails(); // Load user details on initialization
    }

    private void createFormComponents() {
        // Main container panel for vertical stacking
        JPanel mainContentContainer = new JPanel(); // Changed to use BoxLayout
        mainContentContainer.setLayout(new BoxLayout(mainContentContainer, BoxLayout.Y_AXIS)); // Set BoxLayout
        mainContentContainer.setOpaque(false);
        mainContentContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // --- Personal Information Panel ---
        JPanel profilePanel = new JPanel(new GridBagLayout());
        profilePanel.setBackground(Color.WHITE);
        profilePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbcProfile = new GridBagConstraints();
        gbcProfile.insets = new Insets(8, 8, 8, 8);
        gbcProfile.fill = GridBagConstraints.HORIZONTAL;
        gbcProfile.anchor = GridBagConstraints.WEST;

        JLabel profileTitle = new JLabel("Personal Information");
        profileTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        profileTitle.setForeground(DARK_NAVY);
        gbcProfile.gridx = 0;
        gbcProfile.gridy = 0;
        gbcProfile.gridwidth = 2;
        profilePanel.add(profileTitle, gbcProfile);

        gbcProfile.gridwidth = 1; // Reset gridwidth
        int rowProfile = 1;

        gbcProfile.gridx = 0;
        gbcProfile.gridy = rowProfile;
        profilePanel.add(new JLabel("Name:"), gbcProfile);
        gbcProfile.gridx = 1;
        nameField = new JTextField(20); // Reduced width for side-by-side
        profilePanel.add(nameField, gbcProfile);

        rowProfile++;
        gbcProfile.gridx = 0;
        gbcProfile.gridy = rowProfile;
        profilePanel.add(new JLabel("Email:"), gbcProfile);
        gbcProfile.gridx = 1;
        emailField = new JTextField(20); // Reduced width
        profilePanel.add(emailField, gbcProfile);

        rowProfile++;
        gbcProfile.gridx = 0;
        gbcProfile.gridy = rowProfile;
        profilePanel.add(new JLabel("Contact:"), gbcProfile); // Corrected: panel.add -> profilePanel.add
        gbcProfile.gridx = 1;
        contactField = new JTextField(20); // NEW: Add Contact Field
        profilePanel.add(contactField, gbcProfile);

        rowProfile++;
        gbcProfile.gridx = 0;
        gbcProfile.gridy = rowProfile;
        profilePanel.add(new JLabel("Role:"), gbcProfile);
        gbcProfile.gridx = 1;
        roleLabel = new JLabel(); // Role is read-only
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        profilePanel.add(roleLabel, gbcProfile);

        rowProfile++;
        gbcProfile.gridx = 0;
        gbcProfile.gridy = rowProfile;
        gbcProfile.gridwidth = 2;
        updateProfileButton = createStyledButton("Update Profile Details", new Color(0, 123, 255));
        updateProfileButton.addActionListener(e -> updateProfileDetails());
        profilePanel.add(updateProfileButton, gbcProfile);

        // Add vertical glue to push content to top
        gbcProfile.gridx = 0;
        gbcProfile.gridy = rowProfile + 1; // Next row
        gbcProfile.weighty = 1.0; // Make this row expand vertically
        profilePanel.add(Box.createVerticalGlue(), gbcProfile); // Add glue here


        // --- Change Password Panel ---
        JPanel passwordPanel = new JPanel(new GridBagLayout());
        passwordPanel.setBackground(Color.WHITE);
        passwordPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbcPassword = new GridBagConstraints();
        gbcPassword.insets = new Insets(8, 8, 8, 8);
        gbcPassword.fill = GridBagConstraints.HORIZONTAL;
        gbcPassword.anchor = GridBagConstraints.WEST;

        JLabel passwordTitle = new JLabel("Change Password");
        passwordTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        passwordTitle.setForeground(DARK_NAVY);
        gbcPassword.gridx = 0;
        gbcPassword.gridy = 0;
        gbcPassword.gridwidth = 2;
        passwordPanel.add(passwordTitle, gbcPassword);

        gbcPassword.gridwidth = 1; // Reset gridwidth
        int rowPassword = 1;

        // New: Current Password Field
        gbcPassword.gridx = 0;
        gbcPassword.gridy = rowPassword;
        passwordPanel.add(new JLabel("Current Password:"), gbcPassword);
        gbcPassword.gridx = 1;
        currentPasswordField = new JPasswordField(20); // Reduced width
        passwordPanel.add(currentPasswordField, gbcPassword);

        rowPassword++;
        gbcPassword.gridx = 0;
        gbcPassword.gridy = rowPassword;
        passwordPanel.add(new JLabel("New Password:"), gbcPassword);
        gbcPassword.gridx = 1;
        newPasswordField = new JPasswordField(20); // Reduced width
        passwordPanel.add(newPasswordField, gbcPassword);

        rowPassword++;
        gbcPassword.gridx = 0;
        gbcPassword.gridy = rowPassword;
        passwordPanel.add(new JLabel("Confirm New Password:"), gbcPassword);
        gbcPassword.gridx = 1;
        confirmPasswordField = new JPasswordField(20); // Reduced width
        passwordPanel.add(confirmPasswordField, gbcPassword);

        rowPassword++;
        gbcPassword.gridx = 0;
        gbcPassword.gridy = rowPassword;
        gbcPassword.gridwidth = 2;
        changePasswordButton = createStyledButton("Change Password", new Color(255, 165, 0)); // Orange
        changePasswordButton.addActionListener(e -> changePassword());
        passwordPanel.add(changePasswordButton, gbcPassword);

        // Add vertical glue to push content to top
        gbcPassword.gridx = 0;
        gbcPassword.gridy = rowPassword + 1;
        gbcPassword.weighty = 1.0;
        passwordPanel.add(Box.createVerticalGlue(), gbcPassword);

        // --- NEW: Notification Preferences Panel (below password panel) ---
        JPanel notificationPanel = new JPanel(new GridBagLayout());
        notificationPanel.setBackground(Color.WHITE);
        notificationPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbcNotif = new GridBagConstraints();
        gbcNotif.insets = new Insets(8, 8, 8, 8);
        gbcNotif.fill = GridBagConstraints.HORIZONTAL;
        gbcNotif.anchor = GridBagConstraints.WEST;

        JLabel notifTitle = new JLabel("Notification Preferences");
        notifTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        notifTitle.setForeground(DARK_NAVY);
        gbcNotif.gridx = 0;
        gbcNotif.gridy = 0;
        gbcNotif.gridwidth = 2;
        notificationPanel.add(notifTitle, gbcNotif);

        int rowNotif = 1;
        gbcNotif.gridwidth = 2; // Checkboxes span 2 columns

        gbcNotif.gridx = 0;
        gbcNotif.gridy = rowNotif++;
        emailNotificationsCheckBox = new JCheckBox("Receive Email Notifications");
        emailNotificationsCheckBox.setBackground(Color.WHITE);
        notificationPanel.add(emailNotificationsCheckBox, gbcNotif);

        gbcNotif.gridx = 0;
        gbcNotif.gridy = rowNotif++;
        smsNotificationsCheckBox = new JCheckBox("Receive SMS Notifications");
        smsNotificationsCheckBox.setBackground(Color.WHITE);
        notificationPanel.add(smsNotificationsCheckBox, gbcNotif);

        gbcNotif.gridx = 0;
        gbcNotif.gridy = rowNotif++;
        saveNotificationPreferencesButton = createStyledButton("Save Preferences", new Color(40, 167, 69)); // Green
        saveNotificationPreferencesButton.addActionListener(e -> saveNotificationPreferences());
        notificationPanel.add(saveNotificationPreferencesButton, gbcNotif);

        // Add vertical glue to push content to top
        gbcNotif.gridx = 0;
        gbcNotif.gridy = rowNotif++;
        gbcNotif.weighty = 1.0;
        notificationPanel.add(Box.createVerticalGlue(), gbcNotif);

        // Add the panels to the main content container in desired order
        mainContentContainer.add(profilePanel);
        mainContentContainer.add(Box.createVerticalStrut(20)); // Add some spacing
        mainContentContainer.add(passwordPanel);
        mainContentContainer.add(Box.createVerticalStrut(20)); // Add some spacing
        mainContentContainer.add(notificationPanel);

        // Add the main content container to this SettingsPanel, centered
        GridBagConstraints panelGbc = new GridBagConstraints();
        panelGbc.fill = GridBagConstraints.BOTH;
        panelGbc.weightx = 1.0;
        panelGbc.weighty = 1.0;
        add(mainContentContainer, panelGbc);
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

    private void loadUserDetails() {
        // Re-fetch user from DB to ensure latest data, in case it was updated elsewhere
        // Note: This assumes the password stored in DB is 'password' for re-authentication purposes
        // In a real application, you'd likely use a session token or more robust re-authentication.
        User latestUser = Database.authenticateUser(currentUser.getEmail(), "password"); 
        if (latestUser != null) {
            this.currentUser = latestUser; // Update current user object
        }
        nameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        contactField.setText(currentUser.getContact()); // NEW: Populate contact field
        roleLabel.setText(currentUser.getRole());
        currentPasswordField.setText(""); // Clear current password field
        newPasswordField.setText("");
        confirmPasswordField.setText("");

        // Load notification preferences
        UserPreference preferences = Database.getUserPreferences(currentUser.getId());
        if (preferences != null) {
            emailNotificationsCheckBox.setSelected(preferences.isEmailNotificationsEnabled());
            smsNotificationsCheckBox.setSelected(preferences.isSmsNotificationsEnabled());
        } else {
            // Default to enabled if no preferences found
            emailNotificationsCheckBox.setSelected(true);
            smsNotificationsCheckBox.setSelected(true);
        }
    }

    private void updateProfileDetails() {
        String newName = nameField.getText().trim();
        String newEmail = emailField.getText().trim();
        String newContact = contactField.getText().trim(); // NEW: Get new contact

        if (newName.isEmpty() || newEmail.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Email cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean nameEmailChanged = !newName.equals(currentUser.getName()) || !newEmail.equals(currentUser.getEmail());
        boolean contactChanged = !newContact.equals(currentUser.getContact());

        if (!nameEmailChanged && !contactChanged) {
            JOptionPane.showMessageDialog(this, "No changes detected in profile details.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean success = true;
        if (nameEmailChanged) {
            if (Database.updateUserNameAndEmail(currentUser.getId(), newName, newEmail)) {
                currentUser.setName(newName);
                currentUser.setEmail(newEmail);
            } else {
                success = false;
            }
        }
        if (success && contactChanged) { // Only try to update contact if name/email update was successful or not attempted
            if (Database.updateUserContact(currentUser.getId(), newContact)) {
                currentUser.setContact(newContact);
            } else {
                success = false;
            }
        }

        if (success) {
            JOptionPane.showMessageDialog(this, "Profile details updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            // Refresh the dashboard header if it displays user name/email
            parentDashboard.revalidate();
            parentDashboard.repaint();
            loadUserDetails(); // Reload to ensure UI reflects changes
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update profile details. The email might already be in use or another error occurred.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changePassword() {
        String currentPass = new String(currentPasswordField.getPassword()); // Get current password
        String newPass = new String(newPasswordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());

        if (currentPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your current password.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Authenticate current user with provided current password
        User authenticatedUser = Database.authenticateUser(currentUser.getEmail(), currentPass);
        if (authenticatedUser == null) {
            JOptionPane.showMessageDialog(this, "Incorrect current password.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "New password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "New password and confirm password do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newPass.equals(currentPass)) {
            JOptionPane.showMessageDialog(this, "New password cannot be the same as the current password.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (Database.updateUserPassword(currentUser.getId(), newPass)) {
            JOptionPane.showMessageDialog(this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            currentPasswordField.setText("");
            newPasswordField.setText("");
            confirmPasswordField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update password.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveNotificationPreferences() {
        boolean emailEnabled = emailNotificationsCheckBox.isSelected();
        boolean smsEnabled = smsNotificationsCheckBox.isSelected();

        UserPreference preferences = Database.getUserPreferences(currentUser.getId()); // Try to load existing
        if (preferences != null) {
            // Update existing preferences, preserving lastSeenUpdateId
            preferences.setEmailNotificationsEnabled(emailEnabled);
            preferences.setSmsNotificationsEnabled(smsEnabled);
            // lastSeenUpdateId remains unchanged as this panel doesn't manage it
        } else {
            // Create new preferences with default lastSeenUpdateId = 0
            preferences = new UserPreference(currentUser.getId(), emailEnabled, smsEnabled, 0);
        }

        if (Database.saveUserPreferences(preferences)) {
            JOptionPane.showMessageDialog(this, "Notification preferences saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to save notification preferences.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refreshData() {
        // This panel doesn't use cards, but the interface requires it.
        // We can simply reload user details to ensure the form is up-to-date.
        loadUserDetails();
        // No card updates needed for this panel
    }
}