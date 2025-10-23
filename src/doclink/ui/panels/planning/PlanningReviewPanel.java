package doclink.ui.panels.planning;

import doclink.Database;
import doclink.models.Billing;
import doclink.models.Document;
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
import java.util.ArrayList;

public class PlanningReviewPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    private DashboardTablePanel tablePanel;

    // Plan details panel
    private JLabel planIdLabel, applicantNameLabel, plotNoLabel, statusLabel;
    private JTextField referenceNoField, billingAmountField;
    private JButton assignRefNoButton, sendBillingButton, forwardToCommitteeButton, forwardRejectedToReceptionButton, viewDocumentsButton;

    private Plan selectedPlan;
    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public PlanningReviewPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Reverted to light grey

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7); // 70% for table, 30% for details
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(new Color(245, 247, 250)); // Reverted to light grey

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
        title.setForeground(DARK_NAVY);
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
        forwardToCommitteeButton.addActionListener(e -> forwardToTechnicalCommittee());
        panel.add(forwardToCommitteeButton, gbc);

        // Forward Rejected to Reception Button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        forwardRejectedToReceptionButton = createStyledButton("Forward Rejected to Reception (for Client)");
        forwardRejectedToReceptionButton.setBackground(new Color(220, 53, 69)); // Red for rejected
        forwardRejectedToReceptionButton.addActionListener(e -> forwardRejectedToReception());
        panel.add(forwardRejectedToReceptionButton, gbc);

        // View Documents Button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        viewDocumentsButton = createStyledButton("View Documents", new Color(108, 117, 125)); // Grey
        viewDocumentsButton.addActionListener(e -> viewPlanDocuments());
        viewDocumentsButton.setEnabled(false); // Initially disabled
        panel.add(viewDocumentsButton, gbc);

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

    private void loadSelectedPlanDetails() {
        int selectedRow = tablePanel.getPlansTable().getSelectedRow();
        if (selectedRow != -1) {
            // Corrected: Get the actual plan ID from the hidden column (index 1)
            int planId = (int) tablePanel.getPlansTable().getValueAt(selectedRow, 1);
            selectedPlan = Database.getPlanById(planId);

            if (selectedPlan != null) {
                planIdLabel.setText(String.valueOf(selectedPlan.getId()));
                applicantNameLabel.setText(selectedPlan.getApplicantName());
                plotNoLabel.setText(selectedPlan.getPlotNo());
                statusLabel.setText(selectedPlan.getStatus());
                referenceNoField.setText(selectedPlan.getReferenceNo() != null ? selectedPlan.getReferenceNo() : "");
                System.out.println("PlanningReviewPanel: Loaded plan ID " + planId + ", Reference No: " + selectedPlan.getReferenceNo());

                // Enable/disable buttons based on status
                boolean isUnderReviewPlanning = selectedPlan.getStatus().equals("Under Review (Planning)");
                boolean isAwaitingPayment = selectedPlan.getStatus().equals("Awaiting Payment");
                boolean isPaymentReceived = selectedPlan.getStatus().equals("PaymentReceived"); // This status is set after reception processes payment
                boolean isRejectedByDirector = selectedPlan.getStatus().equals("Rejected (to Planning)");
                boolean isDeferredByDirector = selectedPlan.getStatus().equals("Deferred (to Planning)");
                boolean isRejectedByCommittee = selectedPlan.getStatus().equals("Rejected"); // From Committee
                boolean isDeferredByCommittee = selectedPlan.getStatus().equals("Deferred"); // From Committee
                boolean isRejectedByStructural = selectedPlan.getStatus().equals("Rejected by Structural (to Planning)");
                boolean hasReferenceNo = selectedPlan.getReferenceNo() != null && !selectedPlan.getReferenceNo().isEmpty();
                
                // Determine if the plan has already been billed or payment confirmed
                boolean hasPaymentConfirmedRemark = selectedPlan.getRemarks() != null && selectedPlan.getRemarks().contains("Payment received with receipt:");
                boolean hasBeenBilled = isAwaitingPayment || isPaymentReceived || (isUnderReviewPlanning && hasPaymentConfirmedRemark);

                // Assign Ref No button enabled for 'Under Review (Planning)' or 'Payment Received' or returned plans
                assignRefNoButton.setEnabled(isUnderReviewPlanning || isPaymentReceived || isRejectedByDirector || isDeferredByDirector || isRejectedByCommittee || isDeferredByCommittee || isRejectedByStructural);
                
                // Send Billing button enabled for 'Under Review (Planning)' or returned plans, but not if already billed
                sendBillingButton.setEnabled((isUnderReviewPlanning || isRejectedByDirector || isDeferredByDirector || isRejectedByCommittee || isDeferredByCommittee || isRejectedByStructural) && !hasBeenBilled);
                
                // Forward to Committee button enabled if payment is received, OR if under planning review AND has a reference number, OR if it's a returned plan for re-evaluation
                forwardToCommitteeButton.setEnabled(isPaymentReceived || (isUnderReviewPlanning && hasReferenceNo) || isRejectedByDirector || isDeferredByDirector || isRejectedByCommittee || isDeferredByCommittee || isRejectedByStructural);
                
                // Forward Rejected to Reception button enabled if rejected by Director, Committee, Structural, OR if currently under Planning review
                // AND NOT if it's already billed
                forwardRejectedToReceptionButton.setEnabled((isRejectedByDirector || isRejectedByCommittee || isRejectedByStructural || isUnderReviewPlanning) && !hasBeenBilled);
                viewDocumentsButton.setEnabled(true); // Always enable view documents if a plan is selected


                Billing existingBilling = Database.getBillingByPlanId(planId);
                if (existingBilling != null) {
                    billingAmountField.setText(String.valueOf(existingBilling.getAmount()));
                    // If already billed and awaiting payment, disable send billing button
                    if (isAwaitingPayment) {
                        sendBillingButton.setEnabled(false);
                    }
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
        // Allow assigning reference number if status is 'Under Review (Planning)' or 'Payment Received' or returned plans
        if (!selectedPlan.getStatus().equals("Under Review (Planning)") && !selectedPlan.getStatus().equals("Payment Received") &&
            !selectedPlan.getStatus().equals("Rejected (to Planning)") && !selectedPlan.getStatus().equals("Deferred (to Planning)") &&
            !selectedPlan.getStatus().equals("Rejected") && !selectedPlan.getStatus().equals("Deferred") &&
            !selectedPlan.getStatus().equals("Rejected by Structural (to Planning)")) {
            JOptionPane.showMessageDialog(this, "Reference number can only be assigned to plans 'Under Review (Planning)', 'Payment Received', or returned plans.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String refNo = referenceNoField.getText().trim();
        if (refNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a reference number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Attempt to update the reference number in the database
        boolean success = Database.updatePlanReferenceNo(selectedPlan.getId(), refNo);

        if (success) {
            Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), currentUser.getRole(), "Assigned Reference No", "Reference number " + refNo + " assigned."));
            JOptionPane.showMessageDialog(this, "Reference number assigned successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshData(); // This will trigger table update and then loadSelectedPlanDetails
        } else {
            JOptionPane.showMessageDialog(this, "The reference number '" + refNo + "' is already taken. Please choose a different one.", "Input Error", JOptionPane.ERROR_MESSAGE);
            // Do not refresh data or clear details on failure, allow user to correct input
        }
    }

    private void sendBillingToReception() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isUnderReviewPlanning = selectedPlan.getStatus().equals("Under Review (Planning)");
        boolean isAwaitingPayment = selectedPlan.getStatus().equals("Awaiting Payment");
        boolean isPaymentReceived = selectedPlan.getStatus().equals("Payment Received");
        boolean hasPaymentConfirmedRemark = selectedPlan.getRemarks() != null && selectedPlan.getRemarks().contains("Payment received with receipt:");

        if (isAwaitingPayment || isPaymentReceived || (isUnderReviewPlanning && hasPaymentConfirmedRemark)) {
            JOptionPane.showMessageDialog(this, "This plan has already been billed or payment has been confirmed. Cannot send billing again.", "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Allow sending billing for 'Under Review (Planning)' or returned plans
        if (!isUnderReviewPlanning &&
            !selectedPlan.getStatus().equals("Rejected (to Planning)") && !selectedPlan.getStatus().equals("Deferred (to Planning)") &&
            !selectedPlan.getStatus().equals("Rejected") && !selectedPlan.getStatus().equals("Deferred") &&
            !selectedPlan.getStatus().equals("Rejected by Structural (to Planning)")) {
            JOptionPane.showMessageDialog(this, "Billing can only be sent for plans 'Under Review (Planning)' or returned plans.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(billingAmountField.getText());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive billing amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Billing existingBilling = Database.getBillingByPlanId(selectedPlan.getId());
        if (existingBilling != null) {
            int confirm = JOptionPane.showConfirmDialog(this, "Billing already exists for this plan. Do you want to update the amount and resend?", "Confirm Re-billing", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.NO_OPTION) {
                return;
            }
            // For simplicity, we'll just add a new billing entry or update the existing one.
            // In a real system, you might have a more robust billing history.
            // For now, let's assume we're creating a new one if it's a re-submission.
            // Or, if the existing one is unpaid, we can update its amount.
            // For this scenario, let's assume if it's a returned plan, we can create a new billing or update.
            // For simplicity, I'll just add a new one if the status is not 'Awaiting Payment'
            if (!selectedPlan.getStatus().equals("Awaiting Payment")) {
                 Database.addBilling(new Billing(selectedPlan.getId(), amount));
            } else {
                // If it's already awaiting payment, we might want to update the existing billing amount
                // This would require a Database.updateBillingAmount method. For now, we'll proceed as if it's a new billing.
                Database.addBilling(new Billing(selectedPlan.getId(), amount));
            }
        } else {
            Database.addBilling(new Billing(selectedPlan.getId(), amount));
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Awaiting Payment", "Billing sent to Reception for client notification.");
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Reception", "Sent Billing Details", "Billing amount: " + amount));

        JOptionPane.showMessageDialog(this, "Billing details sent to Reception. Plan status updated to 'Awaiting Payment'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        refreshData();
        loadSelectedPlanDetails(); // Reload to update UI
    }

    private void forwardToTechnicalCommittee() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Allow forwarding if payment received, OR if under planning review AND has a reference number, OR if it's a returned plan for re-evaluation
        if (!selectedPlan.getStatus().equals("Payment Received") &&
            !(selectedPlan.getStatus().equals("Under Review (Planning)") && selectedPlan.getReferenceNo() != null && !selectedPlan.getReferenceNo().isEmpty()) &&
            !selectedPlan.getStatus().equals("Rejected (to Planning)") && !selectedPlan.getStatus().equals("Deferred (to Planning)") &&
            !selectedPlan.getStatus().equals("Rejected") && !selectedPlan.getStatus().equals("Deferred") &&
            !selectedPlan.getStatus().equals("Rejected by Structural (to Planning)")) {
            JOptionPane.showMessageDialog(this, "This plan is not ready to be forwarded to the Technical Committee (Payment not received, not under planning review with reference number, or not a returned plan for re-evaluation).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedPlan.getReferenceNo() == null || selectedPlan.getReferenceNo().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please assign a reference number before forwarding to the Technical Committee.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Under Review (Committee)", "Forwarded to Technical Committee after payment received or re-evaluation.");
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Committee", "Forwarded for Review", "Payment confirmed or re-evaluation requested."));

        JOptionPane.showMessageDialog(this, "Plan forwarded to Technical Committee successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        refreshData();
        loadSelectedPlanDetails(); // Reload to update UI
    }

    private void forwardRejectedToReception() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if the plan has already been billed
        boolean isUnderReviewPlanning = selectedPlan.getStatus().equals("Under Review (Planning)");
        boolean isAwaitingPayment = selectedPlan.getStatus().equals("Awaiting Payment");
        boolean isPaymentReceived = selectedPlan.getStatus().equals("Payment Received");
        boolean hasPaymentConfirmedRemark = selectedPlan.getRemarks() != null && selectedPlan.getRemarks().contains("Payment received with receipt:");
        boolean hasBeenBilled = isAwaitingPayment || isPaymentReceived || (isUnderReviewPlanning && hasPaymentConfirmedRemark);

        if (hasBeenBilled) {
            JOptionPane.showMessageDialog(this, "This plan has already been billed. It cannot be forwarded to Reception as rejected.", "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Only allow forwarding if the plan is in a rejected state from Director, Committee, or Structural, OR if currently under Planning review
        if (!selectedPlan.getStatus().equals("Rejected (to Planning)") &&
            !selectedPlan.getStatus().equals("Rejected") &&
            !selectedPlan.getStatus().equals("Rejected by Structural (to Planning)") &&
            !selectedPlan.getStatus().equals("Under Review (Planning)")) { // Added this condition
            JOptionPane.showMessageDialog(this, "This plan is not in a state to be forwarded to Reception as rejected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Rejected (to Reception for Client)", "Rejected plan forwarded to Reception for client communication.");
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Reception", "Forwarded Rejected to Client", "Plan rejected, client needs to be notified."));

        JOptionPane.showMessageDialog(this, "Rejected plan forwarded to Reception for client communication.", "Success", JOptionPane.INFORMATION_MESSAGE);
        refreshData();
        loadSelectedPlanDetails(); // Reload to update UI
    }

    private void viewPlanDocuments() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Document> documents = Database.getDocumentsByPlanId(selectedPlan.getId());
        if (documents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No documents found for this plan.", "Documents", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder docList = new StringBuilder("Documents for Plan ID: " + selectedPlan.getId() + "\n\n");
        for (Document doc : documents) {
            docList.append("Name: ").append(doc.getDocName())
                   .append(" (Type: ").append(doc.getDocumentType()).append(")\n")
                   .append("Path: ").append(doc.getFilePath() != null ? doc.getFilePath() : "N/A").append("\n\n");
        }

        JTextArea textArea = new JTextArea(docList.toString());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Plan Documents", JOptionPane.PLAIN_MESSAGE);
    }

    private void clearDetails() {
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
        forwardRejectedToReceptionButton.setEnabled(false);
        viewDocumentsButton.setEnabled(false);
    }

    @Override
    public void refreshData() {
        // Define statuses relevant for Planning Department's immediate action
        List<String> planningActionableStatuses = List.of(
            "Under Review (Planning)",
            "Awaiting Payment",
            "Payment Received",
            "Rejected (to Planning)",
            "Deferred (to Planning)",
            "Rejected", // From Committee
            "Deferred", // From Committee
            "Rejected by Structural (to Planning)"
        );

        // Filter all plans to get only those relevant to Planning's immediate actions
        List<Plan> allPlans = Database.getAllPlans();
        List<Plan> plansForPlanningAction = allPlans.stream()
                                                .filter(p -> planningActionableStatuses.contains(p.getStatus()))
                                                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        // Update cards based on these actionable statuses
        int newPlansForReview = Database.getPlansByStatus("Under Review (Planning)").size();
        int awaitingPayment = Database.getPlansByStatus("Awaiting Payment").size();
        
        // Count plans returned from other departments
        int returnedPlans = Database.getPlansByStatus("Rejected (to Planning)").size() +
                            Database.getPlansByStatus("Deferred (to Planning)").size() +
                            Database.getPlansByStatus("Rejected").size() + // From Committee
                            Database.getPlansByStatus("Deferred").size() + // From Committee
                            Database.getPlansByStatus("Rejected by Structural (to Planning)").size();

        cardsPanel.updateCard(0, "New Plans for Review", newPlansForReview, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Awaiting Payment", awaitingPayment, new Color(255, 165, 0)); // Orange
        cardsPanel.updateCard(2, "Returned Plans", returnedPlans, new Color(220, 53, 69)); // Red

        // Update table with only plans requiring Planning's action
        tablePanel.updateTable(plansForPlanningAction);

        // Clear details if no plan is selected or if selected plan is no longer relevant
        if (selectedPlan != null && !plansForPlanningAction.stream().anyMatch(p -> p.getId() == selectedPlan.getId())) {
            clearDetails();
        } else if (selectedPlan != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanDetails();
        }
    }
}