package doclink.ui.panels.admin;

import doclink.Database;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class AdminUserManagementPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField nameField, emailField, passwordField;
    private JComboBox<String> roleComboBox;
    private JButton addUserButton, deleteUserButton, updateUserRoleButton, blockUnblockButton; // Added blockUnblockButton

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
        passwordField = new JTextField(20); // Using JTextField for simplicity, JPasswordField for production
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        String[] roles = {"Reception", "Planning", "Committee", "Director", "Structural", "Client", "Admin"}; // Removed "Blocked" from dropdown
        roleComboBox = new JComboBox<>(roles);
        panel.add(roleComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        addUserButton = createStyledButton("Add New User", new Color(40, 167, 69)); // Green
        addUserButton.addActionListener(e -> addNewUser());
        panel.add(addUserButton, gbc);

        gbc.gridy++;
        updateUserRoleButton = createStyledButton("Update User Role", new Color(0, 123, 255)); // Blue
        updateUserRoleButton.addActionListener(e -> updateSelectedUserRole());
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
            int userId = (int) tableModel.getValueAt(selectedRow, 1); 
            List<User> allUsers = Database.getAllUsers();
            selectedUser = allUsers.stream().filter(u -> u.getId() == userId).findFirst().orElse(null);

            if (selectedUser != null) {
                nameField.setText(selectedUser.getName());
                emailField.setText(selectedUser.getEmail());
                passwordField.setText(""); 
                
                // Only set role if it's not "Blocked" to avoid issues with dropdown
                if (!selectedUser.getRole().equals("Blocked")) {
                    roleComboBox.setSelectedItem(selectedUser.getRole());
                } else {
                    roleComboBox.setSelectedIndex(-1); // Clear selection if blocked
                }

                updateUserRoleButton.setEnabled(true);
                deleteUserButton.setEnabled(true);
                addUserButton.setEnabled(false); 
                
                // Configure Block/Unblock button
                blockUnblockButton.setEnabled(true);
                if (selectedUser.getRole().equals("Blocked")) {
                    blockUnblockButton.setText("Unblock User");
                    blockUnblockButton.setBackground(new Color(40, 167, 69)); // Green for unblock
                } else {
                    blockUnblockButton.setText("Block User");
                    blockUnblockButton.setBackground(new Color(255, 165, 0)); // Orange for block
                }
            }
        }
    }

    private void clearForm() {
        selectedUser = null;
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        roleComboBox.setSelectedIndex(0); 
        updateUserRoleButton.setEnabled(false);
        deleteUserButton.setEnabled(false);
        addUserButton.setEnabled(true);
        
        blockUnblockButton.setEnabled(false);
        blockUnblockButton.setText("Block/Unblock User");
        blockUnblockButton.setBackground(new Color(108, 117, 125)); // Reset to grey
    }

    private void addNewUser() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String role = (String) roleComboBox.getSelectedItem();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields to add a new user.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (Database.addUser(name, email, password, role)) {
            JOptionPane.showMessageDialog(this, "User '" + name + "' added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Error: A user with this email already exists.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSelectedUserRole() {
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to update.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String newRole = (String) roleComboBox.getSelectedItem();
        if (newRole == null) {
            JOptionPane.showMessageDialog(this, "Please select a role.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Prevent changing to "Blocked" via this button, use the dedicated block/unblock button
        if (newRole.equals("Blocked")) {
            JOptionPane.showMessageDialog(this, "Use the 'Block/Unblock User' button to manage the 'Blocked' status.", "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (Database.updateUserRole(selectedUser.getId(), newRole)) {
            JOptionPane.showMessageDialog(this, "Role for user '" + selectedUser.getName() + "' updated to '" + newRole + "'.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update user role.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleBlockUser() {
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Please select a user first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedUser.getRole().equals("Blocked")) {
            // Unblock user
            String[] roles = {"Reception", "Planning", "Committee", "Director", "Structural", "Client", "Admin"};
            String newRole = (String) JOptionPane.showInputDialog(
                this,
                "Select a new role for " + selectedUser.getName() + " to unblock:",
                "Unblock User",
                JOptionPane.QUESTION_MESSAGE,
                null,
                roles,
                "Client" // Default selection
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
            // Block user
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

    @Override
    public void refreshData() {
        tableModel.setRowCount(0); // Clear existing data
        List<User> users = Database.getAllUsers();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            tableModel.addRow(new Object[]{
                i + 1, // Sequential row number
                user.getId(), // Actual ID (hidden)
                user.getName(),
                user.getEmail(),
                user.getRole()
            });
        }
        clearForm(); // Reset form after refresh
        
        // Update cards with relevant counts for Admin
        List<User> allUsers = Database.getAllUsers();
        int totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(u -> !u.getRole().equals("Blocked")).count(); // Exclude 'Blocked' users from active count
        long adminUsers = allUsers.stream().filter(u -> u.getRole().equals("Admin")).count();

        cardsPanel.updateCard(0, "Total Users", totalUsers, new Color(0, 123, 255)); // Blue
        cardsPanel.updateCard(1, "Active Users", (int) activeUsers, new Color(40, 167, 69)); // Green
        cardsPanel.updateCard(2, "Admin Accounts", (int) adminUsers, new Color(255, 193, 7)); // Yellow
    }
}