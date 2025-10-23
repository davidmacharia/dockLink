package doclink.ui.panels.admin;

import doclink.Database;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.stream.Collectors;

public class AdminUserManagementPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField nameField, emailField, passwordField;
    private JComboBox<String> roleComboBox;
    private JButton addUserButton, deleteUserButton, updateUserRoleButton, blockUnblockButton;

    private JTextField searchField; // New search field
    private JButton searchButton; // New search button
    private List<User> allUsersData; // To store the original, unfiltered list of users

    private User selectedUser;
    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public AdminUserManagementPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel panelTitle = new JLabel("User Management");
        panelTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panelTitle.setForeground(DARK_NAVY);
        panelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(panelTitle, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.6); // 60% for table, 40% for form
        splitPane.setResizeWeight(0.6);
        splitPane.setBackground(new Color(245, 247, 250));

        splitPane.setLeftComponent(createUserTablePanel());
        splitPane.setRightComponent(createUserFormPanel());

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createUserTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);
        searchField = new JTextField(25);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.putClientProperty("JTextField.placeholderText", "Search by Name, Email, Role...");
        searchButton = new JButton("Search");
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchButton.setBackground(new Color(0, 123, 255));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        searchButton.setOpaque(true);
        searchButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { searchButton.setBackground(new Color(0, 100, 200)); }
            public void mouseExited(java.awt.event.MouseEvent evt) { searchButton.setBackground(new Color(0, 123, 255)); }
        });

        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        panel.add(searchPanel, BorderLayout.NORTH);

        String[] columnNames = {"No.", "ID", "Name", "Email", "Role"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userTable.setRowHeight(25);
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        userTable.getTableHeader().setBackground(new Color(230, 230, 230));
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && userTable.getSelectedRow() != -1) {
                    loadSelectedUserDetails();
                }
            }
        });

        TableColumn idColumn = userTable.getColumnModel().getColumn(1);
        idColumn.setMinWidth(0);
        idColumn.setMaxWidth(0);
        idColumn.setPreferredWidth(0);
        idColumn.setResizable(false);

        JScrollPane scrollPane = new JScrollPane(userTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add action listener to search button
        searchButton.addActionListener(e -> applyFilter());

        // Add document listener for real-time search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        return panel;
    }

    private JPanel createUserFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel formTitle = new JLabel("User Details / Add New User");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formTitle.setForeground(DARK_NAVY);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(formTitle, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(20);
        panel.add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JTextField(20); 
        passwordField.setText("doclink"); // Set default password
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        String[] roles = {"Reception", "Planning", "Committee", "Director", "Structural", "Client", "Admin"}; 
        roleComboBox = new JComboBox<>(roles);
        panel.add(roleComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        addUserButton = createStyledButton("Add New User", new Color(40, 167, 69)); // Green
        addUserButton.addActionListener(e -> addNewUser());
        panel.add(addUserButton, gbc);

        gbc.gridy++;
        updateUserRoleButton = createStyledButton("Update User Details", new Color(0, 123, 255)); // Blue
        updateUserRoleButton.addActionListener(e -> updateSelectedUser()); // Renamed method call
        updateUserRoleButton.setEnabled(false);
        panel.add(updateUserRoleButton, gbc);

        gbc.gridy++;
        deleteUserButton = createStyledButton("Delete User", new Color(220, 53, 69)); // Red
        deleteUserButton.addActionListener(e -> deleteSelectedUser());
        deleteUserButton.setEnabled(false);
        panel.add(deleteUserButton, gbc);

        // New Block/Unblock Button
        gbc.gridy++;
        blockUnblockButton = createStyledButton("Block/Unblock User", new Color(108, 117, 125)); // Grey default
        blockUnblockButton.addActionListener(e -> toggleBlockUser());
        blockUnblockButton.setEnabled(false);
        panel.add(blockUnblockButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0; // Push components to top
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
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

    private void loadSelectedUserDetails() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow != -1) {
            // Get the actual user ID from the hidden column
            int userId = (int) userTable.getModel().getValueAt(selectedRow, 1); 
            
            // Find the user in the full data list, not just the filtered view
            selectedUser = allUsersData.stream().filter(u -> u.getId() == userId).findFirst().orElse(null);

            if (selectedUser != null) {
                nameField.setText(selectedUser.getName());
                emailField.setText(selectedUser.getEmail());
                passwordField.setText("doclink"); // Set default password for update
                
                if (!selectedUser.getRole().equals("Blocked")) {
                    roleComboBox.setSelectedItem(selectedUser.getRole());
                } else {
                    roleComboBox.setSelectedIndex(-1); 
                }

                updateUserRoleButton.setEnabled(true);
                deleteUserButton.setEnabled(true);
                addUserButton.setEnabled(false); 
                
                blockUnblockButton.setEnabled(true);
                if (selectedUser.getRole().equals("Blocked")) {
                    blockUnblockButton.setText("Unblock User");
                    blockUnblockButton.setBackground(new Color(40, 167, 69)); 
                } else {
                    blockUnblockButton.setText("Block User");
                    blockUnblockButton.setBackground(new Color(255, 165, 0)); 
                }
            }
        }
    }

    private void clearForm() {
        selectedUser = null;
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("doclink"); // Set default password
        roleComboBox.setSelectedIndex(0); 
        updateUserRoleButton.setEnabled(false);
        deleteUserButton.setEnabled(false);
        addUserButton.setEnabled(true);
        
        blockUnblockButton.setEnabled(false);
        blockUnblockButton.setText("Block/Unblock User");
        blockUnblockButton.setBackground(new Color(108, 117, 125)); 
    }

    private void addNewUser() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim(); // Can be empty or "doclink" or custom
        String role = (String) roleComboBox.getSelectedItem();

        if (name.isEmpty() || email.isEmpty() || role == null) { // Password can be blank
            JOptionPane.showMessageDialog(this, "Please fill in Name, Email, and select a Role.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // If password is left blank, use an empty string. Otherwise, use the provided password.
        String finalPassword = password.isEmpty() ? "" : password;

        if (Database.addUser(name, email, finalPassword, role)) {
            JOptionPane.showMessageDialog(this, "User '" + name + "' added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Error: A user with this email already exists.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSelectedUser() { // Renamed from updateSelectedUserRole
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to update.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String newName = nameField.getText().trim();
        String newEmail = emailField.getText().trim();
        String newRole = (String) roleComboBox.getSelectedItem();
        String newPassword = passwordField.getText().trim(); // Get current password field value

        if (newName.isEmpty() || newEmail.isEmpty() || newRole == null) {
            JOptionPane.showMessageDialog(this, "Please fill in Name, Email, and select a Role.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (newRole.equals("Blocked")) {
            JOptionPane.showMessageDialog(this, "Use the 'Block/Unblock User' button to manage the 'Blocked' status.", "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean detailsUpdated = false;
        boolean roleUpdated = false;
        boolean passwordUpdated = false;
        StringBuilder feedback = new StringBuilder("User details for '" + selectedUser.getName() + "' updated:\n");

        // Update name and email if changed
        if (!selectedUser.getName().equals(newName) || !selectedUser.getEmail().equals(newEmail)) {
            if (Database.updateUserNameAndEmail(selectedUser.getId(), newName, newEmail)) {
                detailsUpdated = true;
                feedback.append("- Name and Email updated.\n");
            } else {
                feedback.append("- Failed to update Name/Email (possibly duplicate email).\n");
            }
        }

        // Update role if changed
        if (!selectedUser.getRole().equals(newRole)) {
            if (Database.updateUserRole(selectedUser.getId(), newRole)) {
                roleUpdated = true;
                feedback.append("- Role updated to '" + newRole + "'.\n");
            } else {
                feedback.append("- Failed to update Role.\n");
            }
        }

        // Update password if it's not empty AND it's different from the default "doclink"
        // OR if it's explicitly set to "doclink"
        if (!newPassword.isEmpty() && !newPassword.equals("doclink")) {
             if (Database.updateUserPassword(selectedUser.getId(), newPassword)) {
                passwordUpdated = true;
                feedback.append("- Password updated.\n");
             } else {
                feedback.append("- Failed to update Password.\n");
             }
        } else if (newPassword.equals("doclink") && !selectedUser.getRole().equals("Blocked")) { // Only update to "doclink" if it's explicitly there and user is not blocked
            // This condition handles if the admin explicitly wants to set the password to "doclink"
            // We need to fetch the current password to avoid unnecessary updates if it's already "doclink"
            // For simplicity, we'll assume if the field is "doclink", and it's not the current password, we update.
            // A more robust solution would involve fetching the current password hash.
            // For now, if the field is "doclink" and the user's current password isn't "doclink", we update.
            // This requires fetching the current password, which is not directly available in `selectedUser` object.
            // Given the current `Database.authenticateUser` method, we don't store plain text passwords in `User` model.
            // To keep it simple and avoid fetching password from DB for comparison, we'll update if the field is "doclink".
            if (Database.updateUserPassword(selectedUser.getId(), newPassword)) {
                passwordUpdated = true;
                feedback.append("- Password reset to 'doclink'.\n");
            } else {
                feedback.append("- Failed to reset Password to 'doclink'.\n");
            }
        } else if (newPassword.isEmpty()) {
            // If the admin explicitly cleared the password field, we do NOT update the password.
            // The existing password remains.
            feedback.append("- Password field left blank, existing password retained.\n");
            passwordUpdated = true; // Mark as true because no error occurred, just no update.
        }


        if (detailsUpdated || roleUpdated || passwordUpdated) {
            JOptionPane.showMessageDialog(this, feedback.toString(), "Update Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "No changes detected or failed to update user details.", "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void toggleBlockUser() {
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Please select a user first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedUser.getRole().equals("Blocked")) {
            String[] roles = {"Reception", "Planning", "Committee", "Director", "Structural", "Client", "Admin"};
            String newRole = (String) JOptionPane.showInputDialog(
                this,
                "Select a new role for " + selectedUser.getName() + " to unblock:",
                "Unblock User",
                JOptionPane.QUESTION_MESSAGE,
                null,
                roles,
                "Client" 
            );

            if (newRole != null && !newRole.trim().isEmpty()) {
                if (Database.updateUserRole(selectedUser.getId(), newRole)) {
                    JOptionPane.showMessageDialog(this, "User '" + selectedUser.getName() + "' unblocked and role set to '" + newRole + "'.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearForm();
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to unblock user.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Unblock cancelled or no role selected.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to block user '" + selectedUser.getName() + "'? They will not be able to log in.", "Confirm Block", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                if (Database.updateUserRole(selectedUser.getId(), "Blocked")) {
                    JOptionPane.showMessageDialog(this, "User '" + selectedUser.getName() + "' blocked successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearForm();
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to block user.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void deleteSelectedUser() {
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete user '" + selectedUser.getName() + "'? This action cannot be undone.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (Database.deleteUser(selectedUser.getId())) {
                JOptionPane.showMessageDialog(this, "User '" + selectedUser.getName() + "' deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete user.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void applyFilter() {
        tableModel.setRowCount(0); // Clear existing data
        String searchText = searchField.getText().toLowerCase(Locale.ROOT).trim();

        List<User> filteredUsers = allUsersData.stream()
                .filter(user -> {
                    if (searchText.isEmpty()) {
                        return true; // Show all if search field is empty
                    }
                    // Check various fields for the search text
                    return (user.getName() != null && user.getName().toLowerCase(Locale.ROOT).contains(searchText)) ||
                           (user.getEmail() != null && user.getEmail().toLowerCase(Locale.ROOT).contains(searchText)) ||
                           (user.getRole() != null && user.getRole().toLowerCase(Locale.ROOT).contains(searchText));
                })
                .collect(Collectors.toList());

        for (int i = 0; i < filteredUsers.size(); i++) {
            User user = filteredUsers.get(i);
            tableModel.addRow(new Object[]{
                i + 1, // Row number starting from 1
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
            });
        }
    }

    @Override
    public void refreshData() {
        allUsersData = Database.getAllUsers(); // Load all users from the database
        applyFilter(); // Apply the current filter (or show all if no filter)
        clearForm(); // Reset form after refresh
        
        // Update cards with relevant counts for Admin
        int totalUsers = allUsersData.size();
        long activeUsers = allUsersData.stream().filter(u -> !u.getRole().equals("Blocked")).count(); 
        long adminUsers = allUsersData.stream().filter(u -> u.getRole().equals("Admin")).count();

        cardsPanel.updateCard(0, "Total Users", totalUsers, new Color(0, 123, 255)); 
        cardsPanel.updateCard(1, "Active Users", (int) activeUsers, new Color(40, 167, 69)); 
        cardsPanel.updateCard(2, "Admin Accounts", (int) adminUsers, new Color(255, 193, 7)); 
    }
}