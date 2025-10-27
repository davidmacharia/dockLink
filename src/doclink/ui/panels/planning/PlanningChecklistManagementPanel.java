package doclink.ui.panels.planning;

import doclink.Database;
import doclink.models.DocumentChecklistItem;
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

public class PlanningChecklistManagementPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private JTable checklistTable;
    private DefaultTableModel tableModel;
    private JTextField itemNameField;
    private JCheckBox isRequiredCheckBox;
    private JCheckBox requiresFileUploadCheckBox; // NEW: Checkbox for requiresFileUpload
    private JButton addChecklistItemButton, updateChecklistItemButton, deleteChecklistItemButton;

    private DocumentChecklistItem selectedItem;
    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public PlanningChecklistManagementPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel panelTitle = new JLabel("Document Checklist Management");
        panelTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panelTitle.setForeground(DARK_NAVY);
        panelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(panelTitle, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.6); // 60% for table, 40% for form
        splitPane.setResizeWeight(0.6);
        splitPane.setBackground(new Color(245, 247, 250));

        splitPane.setLeftComponent(createChecklistTablePanel());
        splitPane.setRightComponent(createChecklistFormPanel());

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createChecklistTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel tableTitle = new JLabel("All Checklist Items");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(DARK_NAVY);
        tableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(tableTitle, BorderLayout.NORTH);

        String[] columnNames = {"No.", "ID", "Item Name", "Required", "File Upload Needed"}; // NEW Column: "No."
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 3 || column == 4) return Boolean.class; // "Required" and "File Upload Needed" columns are booleans
                return super.getColumnClass(column);
            }
        };
        checklistTable = new JTable(tableModel);
        checklistTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        checklistTable.setRowHeight(25);
        checklistTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        checklistTable.getTableHeader().setBackground(new Color(230, 230, 230));
        checklistTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        checklistTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && checklistTable.getSelectedRow() != -1) {
                    loadSelectedChecklistItemDetails();
                }
            }
        });

        // Hide the "ID" column (now at index 1)
        TableColumn idColumn = checklistTable.getColumnModel().getColumn(1);
        idColumn.setMinWidth(0);
        idColumn.setMaxWidth(0);
        idColumn.setPreferredWidth(0);
        idColumn.setResizable(false);

        // Set preferred width for "No." column
        checklistTable.getColumnModel().getColumn(0).setPreferredWidth(40);

        JScrollPane scrollPane = new JScrollPane(checklistTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createChecklistFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel formTitle = new JLabel("Item Details / Add New Item");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formTitle.setForeground(DARK_NAVY);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(formTitle, gbc);

        gbc.gridwidth = 1; // Reset gridwidth
        int row = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1;
        itemNameField = new JTextField(20);
        panel.add(itemNameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Is Required:"), gbc);
        gbc.gridx = 1;
        isRequiredCheckBox = new JCheckBox();
        isRequiredCheckBox.setBackground(Color.WHITE); // Ensure checkbox background matches panel
        panel.add(isRequiredCheckBox, gbc);

        row++; // NEW: Add row for requiresFileUpload
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Requires File Upload:"), gbc);
        gbc.gridx = 1;
        requiresFileUploadCheckBox = new JCheckBox();
        requiresFileUploadCheckBox.setBackground(Color.WHITE);
        panel.add(requiresFileUploadCheckBox, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        addChecklistItemButton = createStyledButton("Add New Item", new Color(40, 167, 69)); // Green
        addChecklistItemButton.addActionListener(e -> addChecklistItem());
        panel.add(addChecklistItemButton, gbc);

        row++;
        gbc.gridy = row;
        updateChecklistItemButton = createStyledButton("Update Item", new Color(0, 123, 255)); // Blue
        updateChecklistItemButton.addActionListener(e -> updateChecklistItem());
        updateChecklistItemButton.setEnabled(false);
        panel.add(updateChecklistItemButton, gbc);

        row++;
        gbc.gridy = row;
        deleteChecklistItemButton = createStyledButton("Delete Item", new Color(220, 53, 69)); // Red
        deleteChecklistItemButton.addActionListener(e -> deleteChecklistItem());
        deleteChecklistItemButton.setEnabled(false);
        panel.add(deleteChecklistItemButton, gbc);

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

    private void loadSelectedChecklistItemDetails() {
        int selectedRow = checklistTable.getSelectedRow();
        if (selectedRow != -1) {
            int itemId = (int) checklistTable.getValueAt(selectedRow, 1); // ID is now at index 1
            // Retrieve the full object from the database to ensure all properties are correct
            List<DocumentChecklistItem> allItems = Database.getAllChecklistItems();
            selectedItem = allItems.stream().filter(item -> item.getId() == itemId).findFirst().orElse(null);

            if (selectedItem != null) {
                itemNameField.setText(selectedItem.getItemName());
                isRequiredCheckBox.setSelected(selectedItem.isRequired());
                requiresFileUploadCheckBox.setSelected(selectedItem.requiresFileUpload()); // NEW: Set checkbox state

                addChecklistItemButton.setEnabled(false);
                updateChecklistItemButton.setEnabled(true);
                deleteChecklistItemButton.setEnabled(true);
            }
        }
    }

    private void clearForm() {
        selectedItem = null;
        itemNameField.setText("");
        isRequiredCheckBox.setSelected(false);
        requiresFileUploadCheckBox.setSelected(false); // NEW: Clear checkbox state

        addChecklistItemButton.setEnabled(true);
        updateChecklistItemButton.setEnabled(false);
        deleteChecklistItemButton.setEnabled(false);
    }

    private void addChecklistItem() {
        String itemName = itemNameField.getText().trim();
        boolean isRequired = isRequiredCheckBox.isSelected();
        boolean requiresFileUpload = requiresFileUploadCheckBox.isSelected(); // NEW: Get state

        if (itemName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an item name.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (Database.addChecklistItem(itemName, isRequired, requiresFileUpload)) { // NEW: Pass requiresFileUpload
            JOptionPane.showMessageDialog(this, "Checklist item '" + itemName + "' added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshData();
        } else {
            // Error message handled by Database.addChecklistItem for unique constraint
        }
    }

    private void updateChecklistItem() {
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Please select an item to update.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String newItemName = itemNameField.getText().trim();
        boolean newIsRequired = isRequiredCheckBox.isSelected();
        boolean newRequiresFileUpload = requiresFileUploadCheckBox.isSelected(); // NEW: Get state

        if (newItemName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an item name.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (Database.updateChecklistItem(selectedItem.getId(), newItemName, newIsRequired, newRequiresFileUpload)) { // NEW: Pass newRequiresFileUpload
            JOptionPane.showMessageDialog(this, "Checklist item '" + newItemName + "' updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshData();
        } else {
            // Error message handled by Database.updateChecklistItem for unique constraint
        }
    }

    private void deleteChecklistItem() {
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete checklist item '" + selectedItem.getItemName() + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (Database.deleteChecklistItem(selectedItem.getId())) {
                JOptionPane.showMessageDialog(this, "Checklist item deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete checklist item.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Planning (re-using existing logic)
        int pendingPlanning = Database.getPlansByStatus("Under Review (Planning)").size();
        int awaitingPayment = Database.getPlansByStatus("Awaiting Payment").size();
        int paymentReceived = Database.getPlansByStatus("Payment Received").size();
        int returnedFromDirectorOrCommitteeOrStructural = Database.getPlansByStatus("Rejected (to Planning)").size() +
                                             Database.getPlansByStatus("Deferred (to Planning)").size() +
                                             Database.getPlansByStatus("Rejected").size() +
                                             Database.getPlansByStatus("Deferred").size() +
                                             Database.getPlansByStatus("Rejected by Structural (to Planning)").size();

        cardsPanel.updateCard(0, "New Plans for Review", pendingPlanning, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Awaiting Payment", awaitingPayment, new Color(255, 165, 0)); // Orange
        cardsPanel.updateCard(2, "Payment Received / Returned", paymentReceived + returnedFromDirectorOrCommitteeOrStructural, new Color(40, 167, 69)); // Green

        // Update table with all checklist items
        tableModel.setRowCount(0); // Clear existing data
        List<DocumentChecklistItem> items = Database.getAllChecklistItems();
        for (int i = 0; i < items.size(); i++) {
            DocumentChecklistItem item = items.get(i);
            tableModel.addRow(new Object[]{
                i + 1, // No. column
                item.getId(),
                item.getItemName(),
                item.isRequired(),
                item.requiresFileUpload() // NEW: Add requiresFileUpload to table
            });
        }
        clearForm(); // Clear form after refresh
    }
}