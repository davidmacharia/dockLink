package doclink.ui.views;

import doclink.Database;
import doclink.models.Billing;
import doclink.models.Log;
import doclink.models.Plan;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.DashboardTablePanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class PlanningView extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;

    private DashboardCardsPanel cardsPanel;
    private DashboardTablePanel tablePanel;

    // Plan details panel
    private JLabel planIdLabel, applicantNameLabel, plotNoLabel, statusLabel;
    private JTextField referenceNoField, billingAmountField;
    private JButton assignRefNoButton, sendBillingButton, forwardToCommitteeButton; // Added forwardToCommitteeButton

    private Plan selectedPlan;

    public PlanningView(User user, Dashboard parentDashboard) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        cardsPanel = new DashboardCardsPanel();
        add(cardsPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7); // 70% for table, 30% for details
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(new Color(245, 247, 250));

        tablePanel = new DashboardTablePanel();
        tablePanel.getPlansTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && tablePanel.getPlansTable().getSelectedRow() != -1) {
                    loadSelectedPlanDetails();
                }
            }
        });
        splitPane.setLeftComponent(tablePanel);

        JPanel detailsPanel = createDetailsPanel();
        splitPane.setRightComponent(detailsPanel);

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Plan Details & Actions");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(26, 35, 126));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Plan ID:"), gbc);
        gbc.gridx = 1;
        planIdLabel = new JLabel("N/A");
        panel.add(planIdLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Applicant:"), gbc);
        gbc.gridx = 1;
        applicantNameLabel = new JLabel("N/A");
        panel.add(applicantNameLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Plot No:"), gbc);
        gbc.gridx = 1;
        plotNoLabel = new JLabel("N/A");
        panel.add(plotNoLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        statusLabel = new JLabel("N/A");
        panel.add(statusLabel, gbc);

        // Reference Number
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Reference No:"), gbc);
        gbc.gridx = 1;
        referenceNoField = new JTextField(15);
        panel.add(referenceNoField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        assignRefNoButton = createStyledButton("Assign Reference No");
        assignRefNoButton.addActionListener(e -> assignReferenceNumber());
        panel.add(assignRefNoButton, gbc);

        // Billing
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Billing Amount:"), gbc);
        gbc.gridx = 1;
        billingAmountField = new JTextField(15);
        billingAmountField.setEditable(false); // Make billing amount read-only
        panel.add(billingAmountField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        sendBillingButton = createStyledButton("Send Billing to Reception");
        sendBillingButton.addActionListener(e -> sendBillingToReception());
        panel.add(sendBillingButton, gbc);

        // Forward to Committee Button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        forwardToCommitteeButton = createStyledButton("Forward to Technical Committee");
        forwardToCommitteeButton.addActionListener(e -> forwardToCommittee());
        forwardToCommitteeButton.setEnabled(false); // Initially disabled
        panel.add(forwardToCommitteeButton, gbc);

        // Add some vertical glue to push components to the top
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1.0; // Make this row expand vertically
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(new Color(0, 123, 255));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { button.setBackground(new Color(0, 100, 200)); }
            public void mouseExited(java.awt.event.MouseEvent evt) { button.setBackground(new Color(0, 123, 255)); }
        });
        return button;
    }

    private void loadSelectedPlanDetails() {
        int selectedRow = tablePanel.getPlansTable().getSelectedRow();
        if (selectedRow != -1) {
            int planId = (int) tablePanel.getPlansTable().getValueAt(selectedRow, 0);
            selectedPlan = Database.getPlanById(planId);

            if (selectedPlan != null) {
                planIdLabel.setText(String.valueOf(selectedPlan.getId()));
                applicantNameLabel.setText(selectedPlan.getApplicantName());
                plotNoLabel.setText(selectedPlan.getPlotNo());
                statusLabel.setText(selectedPlan.getStatus());
                referenceNoField.setText(selectedPlan.getReferenceNo() != null ? selectedPlan.getReferenceNo() : "");

                // Enable/disable buttons based on status
                boolean isUnderReviewPlanning = selectedPlan.getStatus().equals("Under Review (Planning)");
                boolean isAwaitingPayment = selectedPlan.getStatus().equals("Awaiting Payment");
                boolean isPaymentReceived = selectedPlan.getStatus().equals("Payment Received");

                assignRefNoButton.setEnabled(isUnderReviewPlanning);
                sendBillingButton.setEnabled(isUnderReviewPlanning && selectedPlan.getReferenceNo() != null && !selectedPlan.getReferenceNo().isEmpty()); // Can only send billing if ref no is assigned
                forwardToCommitteeButton.setEnabled(isPaymentReceived); // Enable if payment is received

                Billing existingBilling = Database.getBillingByPlanId(planId);
                if (existingBilling != null) {
                    billingAmountField.setText(String.format("%.2f", existingBilling.getAmount()));
                    sendBillingButton.setEnabled(false); // Already billed
                } else {
                    billingAmountField.setText("");
                }
            }
        }
    }

    private void assignReferenceNumber() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Planning)")) {
            JOptionPane.showMessageDialog(this, "Reference number can only be assigned to plans 'Under Review (Planning)'.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String refNo = referenceNoField.getText().trim();
        if (refNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a reference number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanReferenceNo(selectedPlan.getId(), refNo);
        selectedPlan.setReferenceNo(refNo); // Update local object
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), currentUser.getRole(), "Assigned Reference No", "Reference number " + refNo + " assigned."));
        JOptionPane.showMessageDialog(this, "Reference number assigned successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        refreshData();
        loadSelectedPlanDetails(); // Reload to update UI and button states
    }

    private void sendBillingToReception() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Planning)")) {
            JOptionPane.showMessageDialog(this, "Billing can only be sent for plans 'Under Review (Planning)'.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedPlan.getReferenceNo() == null || selectedPlan.getReferenceNo().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please assign a reference number before sending billing.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String amountStr = JOptionPane.showInputDialog(this, "Enter billing amount for Plan ID " + selectedPlan.getId() + ":", "Billing Amount", JOptionPane.QUESTION_MESSAGE);
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return; // User cancelled or entered empty
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive billing amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Billing newBilling = new Billing(selectedPlan.getId(), amount);
        Database.addBilling(newBilling);
        Database.updatePlanStatus(selectedPlan.getId(), "Awaiting Payment", "Billing sent to Reception for client notification.");
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Reception", "Sent Billing Details", "Billing amount: " + amount));

        JOptionPane.showMessageDialog(this, "Billing details sent to Reception. Plan status updated to 'Awaiting Payment'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        refreshData();
        loadSelectedPlanDetails(); // Reload to update UI
        parentDashboard.showRoleDashboard("Planning"); // Refresh current view
    }

    private void forwardToCommittee() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Payment Received")) {
            JOptionPane.showMessageDialog(this, "Plan must have 'Payment Received' status to be forwarded to Committee.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to forward Plan ID " + selectedPlan.getId() + " to the Technical Committee?", "Confirm Forward", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Database.updatePlanStatus(selectedPlan.getId(), "Under Review (Committee)", "Forwarded to Technical Committee for review.");
            Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Committee", "Forwarded for Review", "Payment confirmed, forwarded to Technical Committee."));

            JOptionPane.showMessageDialog(this, "Plan forwarded to Technical Committee successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
            loadSelectedPlanDetails(); // Reload to update UI and button states
            parentDashboard.showRoleDashboard("Planning"); // Refresh current view
        }
    }

    @Override
    public void refreshData() {
        // Update cards
        int pendingPlanning = Database.getPlansByStatus("Under Review (Planning)").size();
        int awaitingPayment = Database.getPlansByStatus("Awaiting Payment").size();
        int paymentReceived = Database.getPlansByStatus("Payment Received").size();

        cardsPanel.updateCard(0, "New Plans for Review", pendingPlanning, new Color(255, 193, 7));
        cardsPanel.updateCard(1, "Awaiting Payment", awaitingPayment, new Color(255, 165, 0)); // Orange
        cardsPanel.updateCard(2, "Payment Received", paymentReceived, new Color(40, 167, 69)); // Green

        // Update table with plans relevant to Planning (Under Review, Awaiting Payment, Payment Received)
        List<Plan> planningPlans = Database.getPlansByStatus("Under Review (Planning)");
        planningPlans.addAll(Database.getPlansByStatus("Awaiting Payment"));
        planningPlans.addAll(Database.getPlansByStatus("Payment Received"));
        tablePanel.updateTable(planningPlans);

        // Clear details if no plan is selected or if selected plan is no longer relevant
        if (selectedPlan != null && !planningPlans.stream().anyMatch(p -> p.getId() == selectedPlan.getId())) {
            selectedPlan = null;
            planIdLabel.setText("N/A");
            applicantNameLabel.setText("N/A");
            plotNoLabel.setText("N/A");
            statusLabel.setText("N/A");
            referenceNoField.setText("");
            billingAmountField.setText("");
            assignRefNoButton.setEnabled(false);
            sendBillingButton.setEnabled(false);
            forwardToCommitteeButton.setEnabled(false);
        } else if (selectedPlan != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanDetails();
        }
    }
}