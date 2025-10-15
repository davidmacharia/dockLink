package doclink.ui;

import doclink.models.Plan;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DashboardTablePanel extends JPanel {
    private JTable plansTable;
    private DefaultTableModel tableModel;
    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public DashboardTablePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Reverted to light grey
        setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20)); // Padding

        JLabel tableTitle = new JLabel("Recent Plans");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        tableTitle.setForeground(DARK_NAVY); // Changed to DARK_NAVY for contrast
        tableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        add(tableTitle, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Reference No", "Applicant", "Status", "Department", "Date", "Remarks"};
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
        plansTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Only one row can be selected
        plansTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS); // Changed to AUTO_RESIZE_SUBSEQUENT_COLUMNS for horizontal resizing

        // Custom renderer for Status column
        plansTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
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
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // Ensure vertical scrollbar is always visible
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateTable(List<Plan> plans) {
        tableModel.setRowCount(0); // Clear existing data
        for (Plan plan : plans) {
            // Determine 'Department' based on status for display purposes
            String department = "N/A";
            String status = plan.getStatus();

            if (status.equals("Completed")) {
                department = "Client"; // Explicitly set to Client for completed plans
            } else if (status.contains("Reception") || status.contains("Client Notified") || status.contains("Awaiting Client Pickup") || status.equals("Awaiting Payment")) {
                department = "Reception";
            } else if (status.contains("Planning") || status.equals("Payment Received") ||
                       status.equals("Rejected") || status.equals("Deferred") || // These statuses, when returned from Committee/Director, are handled by Planning
                       status.equals("Rejected by Structural (to Planning)") ||
                       status.equals("Rejected (to Planning)") || status.equals("Deferred (to Planning)")) { // Explicitly added for clarity
                department = "Planning";
            } else if (status.contains("Committee") || status.equals("Approved by Structural (to Committee)")) { // Added status for Committee
                department = "Committee";
            } else if (status.contains("Director")) {
                department = "Director";
            } else if (status.contains("Structural")) {
                department = "Structural";
            }
            
            tableModel.addRow(new Object[]{
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