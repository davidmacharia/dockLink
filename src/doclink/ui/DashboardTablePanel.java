package doclink.ui;

import doclink.models.Plan;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardTablePanel extends JPanel {
    private JTable plansTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchButton;
    private List<Plan> allPlansData; // To store the original, unfiltered list of plans

    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public DashboardTablePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Reverted to light grey
        setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20)); // Padding

        // Header Panel for Title and Search
        JPanel headerContainerPanel = new JPanel(new BorderLayout());
        headerContainerPanel.setOpaque(false);
        headerContainerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0)); // Padding below search bar

        JLabel tableTitle = new JLabel("Recent Plans");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        tableTitle.setForeground(DARK_NAVY); // Changed to DARK_NAVY for contrast
        tableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0)); // Padding below title
        headerContainerPanel.add(tableTitle, BorderLayout.NORTH);

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);
        searchField = new JTextField(30);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.putClientProperty("JTextField.placeholderText", "Search by Reference No, Applicant, Plot No, Status...");
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
        headerContainerPanel.add(searchPanel, BorderLayout.CENTER);

        add(headerContainerPanel, BorderLayout.NORTH);

        String[] columnNames = {"No.", "ID", "Reference No", "Applicant", "Status", "Department", "Date", "Remarks"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        plansTable = new JTable(tableModel);
        plansTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        plansTable.setRowHeight(30);
        plansTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        plansTable.getTableHeader().setBackground(new Color(230, 230, 230));
        plansTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        plansTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        // Hide the "ID" column (index 1)
        TableColumn idColumn = plansTable.getColumnModel().getColumn(1);
        idColumn.setMinWidth(0);
        idColumn.setMaxWidth(0);
        idColumn.setPreferredWidth(0);
        idColumn.setResizable(false);

        // Custom renderer for Status column (now at index 4)
        plansTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                label.setForeground(Color.WHITE);

                String status = (String) value;
                switch (status) {
                    case "Approved":
                    case "Completed":
                    case "Approved (Awaiting Client Pickup)":
                        label.setBackground(new Color(40, 167, 69)); // Green
                        break;
                    case "Pending":
                    case "Awaiting Payment":
                    case "Under Review (Planning)":
                    case "Under Review (Committee)":
                    case "Under Review (Director)":
                    case "Under Review (Structural)":
                    case "Payment Received":
                    case "Approved by Director (to Reception for Structural)":
                    case "Approved by Structural (to Committee)":
                    case "Deferred by Structural (Awaiting Clarification)":
                    case "Client Notified (Awaiting Resubmission)":
                    case "Submitted": // Added Submitted status here
                        label.setBackground(new Color(255, 193, 7)); // Orange/Yellow
                        break;
                    case "Rejected":
                    case "Deferred":
                    case "Rejected (to Planning)":
                    case "Deferred (to Planning)":
                    case "Rejected by Structural (to Planning)":
                    case "Rejected (to Reception for Client)":
                        label.setBackground(new Color(220, 53, 69)); // Red
                        break;
                    default:
                        label.setBackground(Color.GRAY);
                        break;
                }
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(plansTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        add(scrollPane, BorderLayout.CENTER);

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
    }

    public void updateTable(List<Plan> plans) {
        this.allPlansData = new ArrayList<>(plans); // Store the full list
        applyFilter(); // Apply filter immediately when new data is set
    }

    private void applyFilter() {
        tableModel.setRowCount(0); // Clear existing data
        String searchText = searchField.getText().toLowerCase(Locale.ROOT).trim();

        List<Plan> filteredPlans = allPlansData.stream()
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

        for (int i = 0; i < filteredPlans.size(); i++) {
            Plan plan = filteredPlans.get(i);
            String department = "N/A";
            String status = plan.getStatus();

            if (status.equals("Completed") || status.equals("Client Notified (Awaiting Resubmission)")) {
                department = "Client";
            } else if (status.equals("Submitted") || status.contains("Reception") || status.contains("Awaiting Client Pickup") || status.equals("Awaiting Payment")) {
                department = "Reception";
            } else if (status.contains("Planning") || status.equals("Payment Received") ||
                       status.equals("Rejected") || status.equals("Deferred") ||
                       status.equals("Rejected by Structural (to Planning)") ||
                       status.equals("Rejected (to Planning)") || status.equals("Deferred (to Planning)")) {
                department = "Planning";
            } else if (status.contains("Committee") || status.equals("Approved by Structural (to Committee)")) {
                department = "Committee";
            } else if (status.contains("Director")) {
                department = "Director";
            } else if (status.contains("Structural")) {
                department = "Structural";
            }
            
            tableModel.addRow(new Object[]{
                i + 1, // Row number starting from 1
                plan.getId(),
                plan.getReferenceNo() != null ? plan.getReferenceNo() : "N/A",
                plan.getApplicantName(),
                plan.getStatus(),
                department,
                plan.getDateSubmitted(),
                plan.getRemarks()
            });
        }
    }

    public JTable getPlansTable() {
        return plansTable;
    }
}