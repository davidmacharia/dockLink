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

    public DashboardTablePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Very light grey
        setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20)); // Padding

        JLabel tableTitle = new JLabel("Recent Plans");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        tableTitle.setForeground(new Color(26, 35, 126)); // Dark navy
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
                        label.setBackground(new Color(40, 167, 69)); // Green
                        break;
                    case "Pending":
                    case "Awaiting Payment":
                    case "Under Review (Planning)":
                    case "Under Review (Committee)":
                    case "Under Review (Director)":
                    case "Under Review (Structural)":
                        label.setBackground(new Color(255, 193, 7)); // Orange/Yellow
                        break;
                    case "Rejected":
                    case "Deferred":
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
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateTable(List<Plan> plans) {
        tableModel.setRowCount(0); // Clear existing data
        for (Plan plan : plans) {
            // Determine 'Department' based on status for display purposes
            String department = "N/A";
            if (plan.getStatus().contains("Reception")) department = "Reception";
            else if (plan.getStatus().contains("Planning")) department = "Planning";
            else if (plan.getStatus().contains("Committee")) department = "Committee";
            else if (plan.getStatus().contains("Director")) department = "Director";
            else if (plan.getStatus().contains("Structural")) department = "Structural";
            else if (plan.getStatus().contains("Awaiting Payment")) department = "Reception"; // Reception handles payment notification

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