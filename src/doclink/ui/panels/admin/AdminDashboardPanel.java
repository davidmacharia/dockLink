package doclink.ui.panels.admin;

import doclink.Database;
import doclink.models.Log;
import doclink.models.Plan;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

public class AdminDashboardPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private JTable logTable;
    private DefaultTableModel logTableModel;
    private JTextField searchField;
    private JButton searchButton;
    private List<Plan> allPlansData; // Now stores all plans, not logs
    private List<Plan> filteredPlansData; // Plans currently displayed in the table

    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public AdminDashboardPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getName() + " (" + currentUser.getRole() + ")");
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(welcomeLabel);

        topPanel.add(Box.createVerticalStrut(15));

        JLabel activitiesTitle = new JLabel("All Activities:");
        activitiesTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        activitiesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(activitiesTitle);

        topPanel.add(Box.createVerticalStrut(10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);
        searchField = new JTextField(30);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.putClientProperty("JTextField.placeholderText", "Search by Reference No, Applicant, Status...");
        searchButton = createStyledButton("Search", new Color(0, 123, 255));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        topPanel.add(searchPanel);

        topPanel.add(Box.createVerticalStrut(10));
        
        add(topPanel, BorderLayout.NORTH);

        // Updated column names for consolidated view
        String[] columnNames = {"No.", "Plan ID", "Reference No", "Applicant", "Current Status", "Total Activities", "Last Activity Date", "Action"};
        logTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // "Action" column is now at index 7
            }
        };
        logTable = new JTable(logTableModel);
        logTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        logTable.setRowHeight(25);
        logTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        logTable.getTableHeader().setBackground(new Color(230, 230, 230));
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        // Set up button renderer and editor for the "Action" column (now at index 7)
        logTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        logTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor());

        // Hide the "Plan ID" column (index 1)
        logTable.getColumnModel().getColumn(1).setMinWidth(0);
        logTable.getColumnModel().getColumn(1).setMaxWidth(0);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(0);
        logTable.getColumnModel().getColumn(1).setResizable(false);

        // Set preferred column widths for better layout
        logTable.getColumnModel().getColumn(0).setPreferredWidth(40);  // No.
        logTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Reference No
        logTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Applicant
        logTable.getColumnModel().getColumn(4).setPreferredWidth(180); // Current Status
        logTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Total Activities
        logTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Last Activity Date
        logTable.getColumnModel().getColumn(7).setPreferredWidth(120); // Action Button

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        
        add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> applyFilter());

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

        refreshData();
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

    private void loadAllPlansAndSummarizeActivities() {
        allPlansData = Database.getAllPlans(); // Get all plans
        applyFilter(); // Apply filter to populate table
    }

    private void applyFilter() {
        logTableModel.setRowCount(0); // Clear existing data
        String searchText = searchField.getText().toLowerCase(Locale.ROOT).trim();

        filteredPlansData = allPlansData.stream()
                .filter(plan -> {
                    if (searchText.isEmpty()) {
                        return true; // Show all if search field is empty
                    }
                    // Check various fields for the search text
                    return (plan.getReferenceNo() != null && plan.getReferenceNo().toLowerCase(Locale.ROOT).contains(searchText)) ||
                           (plan.getApplicantName() != null && plan.getApplicantName().toLowerCase(Locale.ROOT).contains(searchText)) ||
                           (plan.getPlotNo() != null && plan.getPlotNo().toLowerCase(Locale.ROOT).contains(searchText)) ||
                           (plan.getLocation() != null && plan.getLocation().toLowerCase(Locale.ROOT).contains(searchText)) ||
                           (plan.getStatus() != null && plan.getStatus().toLowerCase(Locale.ROOT).contains(searchText)) ||
                           (plan.getRemarks() != null && plan.getRemarks().toLowerCase(Locale.ROOT).contains(searchText));
                })
                .collect(Collectors.toList());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < filteredPlansData.size(); i++) {
            Plan plan = filteredPlansData.get(i);
            List<Log> logsForPlan = Database.getLogsByPlanId(plan.getId());
            
            int totalActivities = logsForPlan.size();
            String lastActivityDate = "N/A";
            if (!logsForPlan.isEmpty()) {
                // Logs are ordered by date DESC, so the first one is the latest
                lastActivityDate = logsForPlan.get(0).getDate().format(formatter);
            }

            logTableModel.addRow(new Object[]{
                i + 1, // No.
                plan.getId(),
                plan.getReferenceNo() != null ? plan.getReferenceNo() : "N/A",
                plan.getApplicantName(),
                plan.getStatus(),
                totalActivities,
                lastActivityDate,
                "View Details" // Text for the button
            });
        }
    }

    @Override
    public void refreshData() {
        List<User> allNonDeveloperUsers = Database.getAllNonDeveloperUsers(); // Get all users excluding developers
        int totalUsers = allNonDeveloperUsers.size();
        long activeUsers = allNonDeveloperUsers.stream().filter(u -> !u.getRole().equals("Blocked")).count();
        long adminUsers = allNonDeveloperUsers.stream().filter(u -> u.getRole().equals("Admin")).count();

        cardsPanel.updateCard(0, "Total Users ", totalUsers, new Color(0, 123, 255));
        cardsPanel.updateCard(1, "Active Users ", (int) activeUsers, new Color(40, 167, 69));
        cardsPanel.updateCard(2, "Admin Accounts", (int) adminUsers, new Color(255, 193, 7));

        loadAllPlansAndSummarizeActivities();
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBackground(new Color(0, 123, 255));
            setForeground(Color.WHITE);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private String label;
        private int clickedPlanId;

        public ButtonEditor() {
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setBackground(new Color(0, 123, 255));
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.addActionListener(this);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            clickedPlanId = (int) table.getModel().getValueAt(row, 1); // Get Plan ID from the hidden column (index 1)
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return label;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
            
            Plan plan = Database.getPlanById(clickedPlanId);
            if (plan != null) {
                StringBuilder detailsBuilder = new StringBuilder();
                // Removed: detailsBuilder.append(String.format("Plan ID: %d\n", plan.getId()));
                detailsBuilder.append(String.format("Reference No: %s\n", plan.getReferenceNo() != null ? plan.getReferenceNo() : "N/A"));
                detailsBuilder.append(String.format("Applicant: %s\n", plan.getApplicantName()));
                detailsBuilder.append(String.format("Plot No: %s\n", plan.getPlotNo()));
                detailsBuilder.append(String.format("Location: %s\n", plan.getLocation()));
                detailsBuilder.append(String.format("Status: %s\n", plan.getStatus()));
                detailsBuilder.append(String.format("Remarks: %s\n\n", plan.getRemarks() != null ? plan.getRemarks() : "N/A"));

                detailsBuilder.append("--- Activity Log for this Plan ---\n\n");
                List<Log> logsForPlan = Database.getLogsByPlanId(clickedPlanId);
                DateTimeFormatter logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                if (logsForPlan.isEmpty()) {
                    detailsBuilder.append("No activities recorded for this plan.\n");
                } else {
                    for (Log log : logsForPlan) {
                        detailsBuilder.append(String.format("Date: %s\n", log.getDate().atStartOfDay().format(logFormatter)));
                        detailsBuilder.append(String.format("  From: %s, To: %s\n", log.getFromRole(), log.getToRole()));
                        detailsBuilder.append(String.format("  Action: %s\n", log.getAction()));
                        detailsBuilder.append(String.format("  Remarks: %s\n\n", log.getRemarks() != null && !log.getRemarks().isEmpty() ? log.getRemarks() : "N/A"));
                    }
                }

                JTextArea textArea = new JTextArea(detailsBuilder.toString());
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(600, 400)); // Adjust size as needed

                Object[] options = {"Re-route Plan", "Delete Plan", "Close"};
                int choice = JOptionPane.showOptionDialog(
                    AdminDashboardPanel.this,
                    scrollPane,
                    "Plan Details & Activities for ID: " + plan.getId(),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[2]
                );

                if (choice == JOptionPane.YES_OPTION) { // Re-route Plan
                    String[] roles = {"Reception", "Planning", "Committee", "Director", "Structural", "Client"};
                    String selectedRole = (String) JOptionPane.showInputDialog(
                        AdminDashboardPanel.this,
                        "Select department to re-route to:",
                        "Re-route Plan",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        roles,
                        "Planning"
                    );

                    if (selectedRole != null && !selectedRole.trim().isEmpty()) {
                        String newStatus;
                        String remarks = "Re-routed by Admin from '" + plan.getStatus() + "' to " + selectedRole + ". Original remarks: " + (plan.getRemarks() != null ? plan.getRemarks() : "N/A");

                        switch (selectedRole) {
                            case "Reception":
                                newStatus = "Submitted";
                                break;
                            case "Planning":
                                newStatus = "Under Review (Planning)";
                                break;
                            case "Committee":
                                newStatus = "Under Review (Committee)";
                                break;
                            case "Director":
                                newStatus = "Under Review (Director)";
                                break;
                            case "Structural":
                                newStatus = "Under Review (Structural)";
                                break;
                            case "Client":
                                newStatus = "Client Notified (Awaiting Resubmission)";
                                break;
                            default:
                                newStatus = plan.getStatus();
                                break;
                        }

                        int confirmReRoute = JOptionPane.showConfirmDialog(AdminDashboardPanel.this, "Are you sure you want to re-route this plan to " + selectedRole + " with status '" + newStatus + "'?", "Confirm Re-route", JOptionPane.YES_NO_OPTION);
                        if (confirmReRoute == JOptionPane.YES_OPTION) {
                            Database.updatePlanStatus(plan.getId(), newStatus, remarks);
                            Database.addLog(new Log(plan.getId(), currentUser.getRole(), selectedRole, "Re-routed by Admin", "Plan re-routed to " + selectedRole + " for review."));
                            JOptionPane.showMessageDialog(AdminDashboardPanel.this, "Plan re-routed to " + selectedRole + " successfully. Status updated to '" + newStatus + "'.", "Success", JOptionPane.INFORMATION_MESSAGE);
                            refreshData();
                        }
                    } else {
                        JOptionPane.showMessageDialog(AdminDashboardPanel.this, "Re-route cancelled or no department selected.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else if (choice == JOptionPane.NO_OPTION) { // Delete Plan
                    int confirm = JOptionPane.showConfirmDialog(AdminDashboardPanel.this, "Are you sure you want to permanently delete plan ID " + plan.getId() + " and all its associated data (documents, billing, logs)? This action cannot be undone.", "Confirm Permanent Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION) {
                        if (Database.deletePlanAndRelatedData(plan.getId())) {
                            JOptionPane.showMessageDialog(AdminDashboardPanel.this, "Plan ID " + plan.getId() + " and all related data deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                            refreshData();
                        } else {
                            JOptionPane.showMessageDialog(AdminDashboardPanel.this, "Failed to delete plan and related data.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(AdminDashboardPanel.this, "Could not retrieve details for Plan ID: " + clickedPlanId, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}