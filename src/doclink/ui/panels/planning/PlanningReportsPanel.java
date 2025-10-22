package doclink.ui.panels.planning;

import doclink.Database;
import doclink.models.Billing;
import doclink.models.Plan;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanningReportsPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private JButton generateStatusReportButton, generateProcessingTimeReportButton, generateRejectionReportButton, generateBillingReportButton;
    private JTextArea reportOutputArea;

    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public PlanningReportsPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel Title
        JLabel panelTitle = new JLabel("Planning Department Reports");
        panelTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panelTitle.setForeground(DARK_NAVY);
        panelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(panelTitle, BorderLayout.NORTH);

        // Main content panel for buttons and output
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        // Report Generation Section Title
        JLabel generateTitle = new JLabel("Generate New Report:");
        generateTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        content.add(generateTitle, gbc);

        // Button Panel for inline buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); // Left aligned, 10px horizontal gap, 5px vertical gap
        buttonPanel.setOpaque(false); // Inherit background from parent

        generateStatusReportButton = createStyledButton("Status Report", new Color(40, 167, 69));
        generateStatusReportButton.addActionListener(e -> generateStatusReport());
        buttonPanel.add(generateStatusReportButton);

        generateProcessingTimeReportButton = createStyledButton("Processing Time Report", new Color(0, 123, 255));
        generateProcessingTimeReportButton.addActionListener(e -> generateProcessingTimeReport());
        buttonPanel.add(generateProcessingTimeReportButton);

        generateRejectionReportButton = createStyledButton("Rejection/Deferral Report", new Color(220, 53, 69));
        generateRejectionReportButton.addActionListener(e -> generateRejectionDeferralReport());
        buttonPanel.add(generateRejectionReportButton);

        generateBillingReportButton = createStyledButton("Billing Overview Report", new Color(255, 165, 0));
        generateBillingReportButton.addActionListener(e -> generateBillingOverviewReport());
        buttonPanel.add(generateBillingReportButton);

        // Add the button panel to the main content panel
        gbc.gridy++; // Move to the next row
        gbc.gridx = 0;
        gbc.gridwidth = 2; // Button panel spans two columns
        gbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontally
        content.add(buttonPanel, gbc);

        // Separator
        gbc.gridy++;
        JSeparator separator = new JSeparator();
        separator.setPreferredSize(new Dimension(0, 1));
        content.add(separator, gbc);

        // Report Output Area Title
        gbc.gridy++;
        JLabel outputTitle = new JLabel("Report Output:");
        outputTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        content.add(outputTitle, gbc);

        // Report Output Area
        gbc.gridy++;
        gbc.weighty = 1.0; // Allow this area to expand vertically
        gbc.fill = GridBagConstraints.BOTH; // Fill both horizontally and vertically
        reportOutputArea = new JTextArea(20, 80); // Increased initial size for better visibility
        reportOutputArea.setEditable(false);
        reportOutputArea.setLineWrap(true);
        reportOutputArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(reportOutputArea);
        content.add(scrollPane, gbc);

        add(content, BorderLayout.CENTER);

        refreshData(); // Initial data load for cards
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

    private void generateStatusReport() {
        reportOutputArea.setText("--- Planning Department: Overall Workflow Status Report ---\n");
        reportOutputArea.append("Date: " + LocalDate.now() + "\n\n");

        List<Plan> allPlans = Database.getAllPlans();
        Map<String, Long> statusCounts = allPlans.stream()
                .collect(Collectors.groupingBy(Plan::getStatus, Collectors.counting()));

        reportOutputArea.append(String.format("Total Plans in System: %d\n\n", allPlans.size()));
        reportOutputArea.append("Plans by Status:\n");
        statusCounts.forEach((status, count) ->
                reportOutputArea.append(String.format("  - %s: %d\n", status, count))
        );
        JOptionPane.showMessageDialog(this, "Overall Workflow Status Report Generated!", "Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void generateProcessingTimeReport() {
        reportOutputArea.setText("--- Planning Department: Average Processing Time Report ---\n");
        reportOutputArea.append("Date: " + LocalDate.now() + "\n\n");

        List<Plan> allPlans = Database.getAllPlans();
        long totalProcessingDays = 0;
        int completedPlansCount = 0;

        for (Plan plan : allPlans) {
            // Consider "Approved", "Completed", "Rejected", "Deferred" as final states for processing time calculation
            if (plan.getStatus().equals("Approved") ||
                plan.getStatus().equals("Approved (Awaiting Client Pickup)") ||
                plan.getStatus().equals("Completed") ||
                plan.getStatus().equals("Rejected") ||
                plan.getStatus().equals("Deferred") ||
                plan.getStatus().equals("Rejected (to Planning)") ||
                plan.getStatus().equals("Deferred (to Planning)") ||
                plan.getStatus().equals("Rejected by Structural (to Planning)") ||
                plan.getStatus().equals("Deferred by Structural (Awaiting Clarification)") ||
                plan.getStatus().equals("Rejected (to Reception for Client)") ||
                plan.getStatus().equals("Client Notified (Awaiting Resubmission)")) {

                long days = ChronoUnit.DAYS.between(plan.getDateSubmitted(), LocalDate.now());
                totalProcessingDays += days;
                completedPlansCount++;
            }
        }

        if (completedPlansCount > 0) {
            double averageDays = (double) totalProcessingDays / completedPlansCount;
            reportOutputArea.append(String.format("Total Plans with Final Status: %d\n", completedPlansCount));
            reportOutputArea.append(String.format("Average Days from Submission to Final Status: %.2f days\n", averageDays));
        } else {
            reportOutputArea.append("No plans have reached a final status yet to calculate average processing time.\n");
        }
        JOptionPane.showMessageDialog(this, "Average Processing Time Report Generated!", "Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void generateRejectionDeferralReport() {
        reportOutputArea.setText("--- Planning Department: Rejection/Deferral Analysis Report ---\n");
        reportOutputArea.append("Date: " + LocalDate.now() + "\n\n");

        List<String> rejectedDeferredStatuses = List.of(
                "Rejected", "Deferred", "Rejected (to Planning)", "Deferred (to Planning)",
                "Rejected by Structural (to Planning)", "Deferred by Structural (Awaiting Clarification)",
                "Rejected (to Reception for Client)", "Client Notified (Awaiting Resubmission)"
        );

        List<Plan> rejectedDeferredPlans = Database.getAllPlans().stream()
                .filter(plan -> rejectedDeferredStatuses.contains(plan.getStatus()))
                .collect(Collectors.toList());

        if (rejectedDeferredPlans.isEmpty()) {
            reportOutputArea.append("No plans currently in rejected or deferred statuses.\n");
        } else {
            reportOutputArea.append(String.format("Total Rejected/Deferred Plans: %d\n\n", rejectedDeferredPlans.size()));
            for (Plan plan : rejectedDeferredPlans) {
                reportOutputArea.append(String.format("Plan ID: %d\n", plan.getId()));
                reportOutputArea.append(String.format("  Applicant: %s\n", plan.getApplicantName()));
                reportOutputArea.append(String.format("  Status: %s\n", plan.getStatus()));
                reportOutputArea.append(String.format("  Remarks: %s\n\n", plan.getRemarks() != null && !plan.getRemarks().isEmpty() ? plan.getRemarks() : "No specific remarks."));
            }
        }
        JOptionPane.showMessageDialog(this, "Rejection/Deferral Analysis Report Generated!", "Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void generateBillingOverviewReport() {
        reportOutputArea.setText("--- Planning Department: Billing Overview Report ---\n");
        reportOutputArea.append("Date: " + LocalDate.now() + "\n\n");

        List<Plan> allPlans = Database.getAllPlans();
        double totalBilledAmount = 0;
        double totalPaidAmount = 0;
        int billedPlansCount = 0;

        StringBuilder billingDetails = new StringBuilder();

        for (Plan plan : allPlans) {
            Billing billing = Database.getBillingByPlanId(plan.getId());
            if (billing != null) {
                billedPlansCount++;
                totalBilledAmount += billing.getAmount();
                boolean isPaid = billing.getReceiptNo() != null && !billing.getReceiptNo().isEmpty();
                if (isPaid) {
                    totalPaidAmount += billing.getAmount();
                }

                billingDetails.append(String.format("Plan ID: %d (Ref No: %s)\n", plan.getId(), plan.getReferenceNo() != null ? plan.getReferenceNo() : "N/A"));
                billingDetails.append(String.format("  Applicant: %s\n", plan.getApplicantName()));
                billingDetails.append(String.format("  Amount: %.2f\n", billing.getAmount()));
                billingDetails.append(String.format("  Status: %s\n", isPaid ? "Paid" : "Pending Payment"));
                if (isPaid) {
                    billingDetails.append(String.format("  Receipt No: %s (Date: %s)\n", billing.getReceiptNo(), billing.getDatePaid()));
                }
                billingDetails.append("\n");
            }
        }

        if (billedPlansCount > 0) {
            reportOutputArea.append(String.format("Total Plans with Billing: %d\n", billedPlansCount));
            reportOutputArea.append(String.format("Total Billed Amount: %.2f\n", totalBilledAmount));
            reportOutputArea.append(String.format("Total Paid Amount: %.2f\n\n", totalPaidAmount));
            reportOutputArea.append("--- Billing Details ---\n\n");
            reportOutputArea.append(billingDetails.toString());
        } else {
            reportOutputArea.append("No billing records found for any plans.\n");
        }
        JOptionPane.showMessageDialog(this, "Billing Overview Report Generated!", "Report", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Planning
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
    }
}