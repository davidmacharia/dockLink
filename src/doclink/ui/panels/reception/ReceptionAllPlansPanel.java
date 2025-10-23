package doclink.ui.panels.reception;

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
import java.util.ArrayList;
import java.util.List;

public class ReceptionAllPlansPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private DashboardTablePanel allPlansTablePanel;

    // Plan details panel components (always visible)
    private JLabel planIdLabel, applicantNameLabel, plotNoLabel, statusLabel;
    private JTextArea planRemarksDisplayArea; // To display existing remarks of the plan (read-only)

    // Dynamic action components
    private JLabel remarksForActionLabel; // Direct reference to the label
    private JTextArea remarksForActionArea; // For new remarks related to the current action
    private JScrollPane scrollPaneActionRemarks; // Direct reference to the scroll pane
    private JLabel billingAmountLabel;
    private JTextField billingAmountField; // Read-only, displays existing billing amount
    private JLabel receiptNoLabel;
    private JTextField receiptNoField; // Editable for entering receipt number

    private JButton forwardToPlanningButton;
    private JButton processPaymentButton;
    private JButton forwardToStructuralButton;
    private JButton releaseToClientButton;
    private JButton notifyClientForResubmissionButton;
    private JButton viewDocumentsButton;

    private Plan selectedPlan;
    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public ReceptionAllPlansPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7);
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(new Color(245, 247, 250));

        allPlansTablePanel = new DashboardTablePanel();
        allPlansTablePanel.getPlansTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && allPlansTablePanel.getPlansTable().getSelectedRow() != -1) {
                    loadSelectedPlanDetails();
                }
            }
        });
        splitPane.setLeftComponent(allPlansTablePanel);

        JPanel detailsPanel = createDetailsAndActionsPanel();
        splitPane.setRightComponent(detailsPanel);

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createDetailsAndActionsPanel() {
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

        // Existing Plan Remarks (read-only)
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Plan Remarks:"), gbc);
        gbc.gridy++;
        planRemarksDisplayArea = new JTextArea(3, 20);
        planRemarksDisplayArea.setEditable(false);
        planRemarksDisplayArea.setLineWrap(true);
        planRemarksDisplayArea.setWrapStyleWord(true);
        JScrollPane scrollPanePlanRemarks = new JScrollPane(planRemarksDisplayArea);
        panel.add(scrollPanePlanRemarks, gbc);

        // Remarks for Action (editable)
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        remarksForActionLabel = new JLabel("Remarks for Current Action:"); // Assign to field
        panel.add(remarksForActionLabel, gbc);
        gbc.gridy++;
        remarksForActionArea = new JTextArea(3, 20);
        remarksForActionArea.setLineWrap(true);
        remarksForActionArea.setWrapStyleWord(true);
        scrollPaneActionRemarks = new JScrollPane(remarksForActionArea); // Assign to field
        panel.add(scrollPaneActionRemarks, gbc);

        // Billing Amount (read-only, dynamically shown)
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        billingAmountLabel = new JLabel("Billing Amount:");
        panel.add(billingAmountLabel, gbc);
        gbc.gridx = 1;
        billingAmountField = new JTextField(15);
        billingAmountField.setEditable(false);
        panel.add(billingAmountField, gbc);

        // Receipt Number (editable, dynamically shown)
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        receiptNoLabel = new JLabel("Receipt No:");
        panel.add(receiptNoLabel, gbc);
        gbc.gridx = 1;
        receiptNoField = new JTextField(15);
        panel.add(receiptNoField, gbc);

        // Action Buttons
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        forwardToPlanningButton = createStyledButton("Forward to Planning", new Color(0, 123, 255));
        forwardToPlanningButton.addActionListener(e -> forwardToPlanning());
        panel.add(forwardToPlanningButton, gbc);

        gbc.gridy++;
        processPaymentButton = createStyledButton("Process Payment & Forward", new Color(255, 165, 0));
        processPaymentButton.addActionListener(e -> processPayment());
        panel.add(processPaymentButton, gbc);

        gbc.gridy++;
        forwardToStructuralButton = createStyledButton("Forward to Structural Department", new Color(40, 167, 69));
        forwardToStructuralButton.addActionListener(e -> forwardToStructural());
        panel.add(forwardToStructuralButton, gbc);

        gbc.gridy++;
        releaseToClientButton = createStyledButton("Release Plan to Client", new Color(0, 123, 255));
        releaseToClientButton.addActionListener(e -> releaseToClient());
        panel.add(releaseToClientButton, gbc);

        gbc.gridy++;
        notifyClientForResubmissionButton = createStyledButton("Notify Client for Resubmission", new Color(220, 53, 69));
        notifyClientForResubmissionButton.addActionListener(e -> notifyClientForResubmission());
        panel.add(notifyClientForResubmissionButton, gbc);

        // View Documents Button (always present when a plan is selected)
        gbc.gridy++;
        viewDocumentsButton = createStyledButton("View Documents", new Color(108, 117, 125));
        viewDocumentsButton.addActionListener(e -> viewPlanDocuments(selectedPlan));
        panel.add(viewDocumentsButton, gbc);

        // Add some vertical glue to push components to the top
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        // Initially hide all dynamic components
        hideAllDynamicComponents();

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

    private void hideAllDynamicComponents() {
        remarksForActionArea.setText("");
        if (remarksForActionLabel != null) { // Use direct reference
            remarksForActionLabel.setVisible(false);
        }
        if (scrollPaneActionRemarks != null) { // Use direct reference
            scrollPaneActionRemarks.setVisible(false);
        }

        billingAmountLabel.setVisible(false);
        billingAmountField.setVisible(false);
        billingAmountField.setText("");

        receiptNoLabel.setVisible(false);
        receiptNoField.setVisible(false);
        receiptNoField.setText("");

        forwardToPlanningButton.setVisible(false);
        processPaymentButton.setVisible(false);
        forwardToStructuralButton.setVisible(false);
        releaseToClientButton.setVisible(false);
        notifyClientForResubmissionButton.setVisible(false);
        viewDocumentsButton.setEnabled(false); // Disable view documents if no plan selected
    }

    private void loadSelectedPlanDetails() {
        int selectedRow = allPlansTablePanel.getPlansTable().getSelectedRow();
        if (selectedRow != -1) {
            // Get the actual plan ID from the hidden column (index 1)
            int planId = (int) allPlansTablePanel.getPlansTable().getValueAt(selectedRow, 1);
            selectedPlan = Database.getPlanById(planId);

            if (selectedPlan != null) {
                planIdLabel.setText(String.valueOf(selectedPlan.getId()));
                applicantNameLabel.setText(selectedPlan.getApplicantName());
                plotNoLabel.setText(selectedPlan.getPlotNo());
                statusLabel.setText(selectedPlan.getStatus());
                planRemarksDisplayArea.setText(selectedPlan.getRemarks() != null ? selectedPlan.getRemarks() : "No remarks.");
                
                hideAllDynamicComponents(); // Reset visibility for new selection

                // Always show the "Remarks for Current Action" section when a plan is selected
                if (remarksForActionLabel != null) {
                    remarksForActionLabel.setVisible(true);
                }
                if (scrollPaneActionRemarks != null) {
                    scrollPaneActionRemarks.setVisible(true);
                }
                remarksForActionArea.setText(""); // Clear for new action
                viewDocumentsButton.setEnabled(true); // Always enable view documents if a plan is selected

                switch (selectedPlan.getStatus()) {
                    case "Submitted":
                        forwardToPlanningButton.setVisible(true);
                        forwardToPlanningButton.setText("Forward to Planning");
                        break;
                    case "Awaiting Payment":
                        Billing billing = Database.getBillingByPlanId(planId);
                        if (billing != null) {
                            billingAmountLabel.setVisible(true);
                            billingAmountField.setVisible(true);
                            billingAmountField.setText(String.format("%.2f", billing.getAmount()));
                            receiptNoLabel.setVisible(true);
                            receiptNoField.setVisible(true);
                            receiptNoField.setText(billing.getReceiptNo() != null ? billing.getReceiptNo() : "");
                            processPaymentButton.setVisible(true);
                            processPaymentButton.setEnabled(billing.getReceiptNo() == null || billing.getReceiptNo().isEmpty()); // Enable only if not yet paid
                        } else {
                            // Should not happen if workflow is followed, but handle defensively
                            JOptionPane.showMessageDialog(this, "Error: No billing record found for this plan awaiting payment.", "Data Error", JOptionPane.ERROR_MESSAGE);
                        }
                        break;
                    case "Payment Received": // NEW CASE: Allow forwarding to Planning after payment
                        forwardToPlanningButton.setVisible(true);
                        forwardToPlanningButton.setText("Forward to Planning (Payment Confirmed)");
                        break;
                    case "Approved by Director (to Reception for Structural)":
                        forwardToStructuralButton.setVisible(true);
                        break;
                    case "Approved (Awaiting Client Pickup)":
                        releaseToClientButton.setVisible(true);
                        break;
                    case "Rejected (to Reception for Client)":
                        notifyClientForResubmissionButton.setVisible(true);
                        break;
                    default:
                        // For other statuses, no specific action buttons are shown, but details are visible
                        remarksForActionArea.setVisible(false);
                        if (remarksForActionLabel != null) {
                            remarksForActionLabel.setVisible(false);
                        }
                        if (scrollPaneActionRemarks != null) {
                            scrollPaneActionRemarks.setVisible(false);
                        }
                        break;
                }
            }
        } else {
            clearDetails();
        }
    }

    private void forwardToPlanning() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksForActionArea.getText().trim();
        String newStatus;
        String logAction;
        String logRemarks;

        if (selectedPlan.getStatus().equals("Submitted")) {
            newStatus = "Under Review (Planning)";
            logAction = "Forwarded for Review (No Billing)";
            logRemarks = "Forwarded to Planning by Reception. " + remarks;
        } else if (selectedPlan.getStatus().equals("Payment Received")) {
            newStatus = "Under Review (Planning)";
            logAction = "Forwarded for Review (Payment Confirmed)";
            logRemarks = "Payment confirmed, forwarded to Planning by Reception. " + remarks;
        } else {
            JOptionPane.showMessageDialog(this, "This plan cannot be forwarded to Planning from its current status: " + selectedPlan.getStatus(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), newStatus, logRemarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", logAction, remarks));

        JOptionPane.showMessageDialog(this, "Plan forwarded to Planning Department successfully. Status updated to '" + newStatus + "'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void processPayment() {
        if (selectedPlan == null || !selectedPlan.getStatus().equals("Awaiting Payment")) {
            JOptionPane.showMessageDialog(this, "Please select a plan 'Awaiting Payment' first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String receiptNo = receiptNoField.getText().trim();
        if (receiptNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a receipt number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Billing billing = Database.getBillingByPlanId(selectedPlan.getId());
        if (billing == null) {
            JOptionPane.showMessageDialog(this, "No billing record found for this plan.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean success = Database.updateBillingPayment(billing.getId(), receiptNo);

        if (success) {
            Database.updatePlanStatus(selectedPlan.getId(), "Payment Received", "Payment received with receipt: " + receiptNo);
            Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Payment Received", "Receipt No: " + receiptNo));

            String receiptDocName = "Payment Receipt";
            String receiptFilePath = "Receipt No: " + receiptNo;
            Document receiptDocument = new Document(selectedPlan.getId(), receiptDocName, receiptFilePath, "Generated");
            Database.addDocument(receiptDocument);

            JOptionPane.showMessageDialog(this, "Payment recorded and plan status updated to 'Payment Received'. You can now forward it to Planning.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearDetails();
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Error: The receipt number '" + receiptNo + "' is already taken or another database error occurred. Please use a unique receipt number.", "Payment Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void forwardToStructural() {
        if (selectedPlan == null || !selectedPlan.getStatus().equals("Approved by Director (to Reception for Structural)")) {
            JOptionPane.showMessageDialog(this, "Please select a plan 'Approved by Director (to Reception for Structural)' first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Document> documents = Database.getDocumentsByPlanId(selectedPlan.getId());
        boolean directorLetterFound = documents.stream().anyMatch(doc ->
            "Director's Approval Letter (Structural)".equals(doc.getDocName()) && "Generated".equals(doc.getDocumentType())
        );

        if (!directorLetterFound) {
            JOptionPane.showMessageDialog(this, "Director's Approval Letter (Structural) not found. Cannot forward to Structural Department.", "Missing Document", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksForActionArea.getText().trim();
        Database.updatePlanStatus(selectedPlan.getId(), "Under Review (Structural)", "Forwarded to Structural Department by Reception after Director's approval. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Structural", "Forwarded to Structural", "Director's approval letter attached. " + remarks));

        JOptionPane.showMessageDialog(this, "Plan forwarded to Structural Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void releaseToClient() {
        if (selectedPlan == null || !selectedPlan.getStatus().equals("Approved (Awaiting Client Pickup)")) {
            JOptionPane.showMessageDialog(this, "Please select a plan 'Approved (Awaiting Client Pickup)' first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksForActionArea.getText().trim();
        Database.updatePlanStatus(selectedPlan.getId(), "Completed", "Plan released to client. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Client", "Plan Released", "Approval certificate provided to client. " + remarks));

        JOptionPane.showMessageDialog(this, "Plan released to client. Status updated to 'Completed'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void notifyClientForResubmission() {
        if (selectedPlan == null || !selectedPlan.getStatus().equals("Rejected (to Reception for Client)")) {
            JOptionPane.showMessageDialog(this, "Please select a plan 'Rejected (to Reception for Client)' first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksForActionArea.getText().trim();
        Database.updatePlanStatus(selectedPlan.getId(), "Client Notified (Awaiting Resubmission)", "Client notified about rejection and requested to resubmit with revisions. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Client", "Notified of Rejection", "Client needs to resubmit. " + remarks));

        JOptionPane.showMessageDialog(this, "Client notified about the rejected plan. Status updated to 'Client Notified (Awaiting Resubmission)'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void viewPlanDocuments(Plan plan) {
        if (plan == null) {
            JOptionPane.showMessageDialog(this, "No plan selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Document> documents = Database.getDocumentsByPlanId(plan.getId());
        if (documents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No documents found for this plan.", "Documents", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder docList = new StringBuilder("Documents for Plan ID: " + plan.getId() + "\n\n");
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
        planRemarksDisplayArea.setText("");
        hideAllDynamicComponents();
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Reception
        int pending = Database.getAllPlans().stream()
                      .filter(p -> p.getStatus().equals("Submitted") ||
                                   p.getStatus().equals("Awaiting Payment") ||
                                   p.getStatus().equals("Under Review (Planning)") ||
                                   p.getStatus().equals("Under Review (Committee)") ||
                                   p.getStatus().equals("Under Review (Director)") ||
                                   p.getStatus().equals("Under Review (Structural)"))
                      .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll).size();

        int approved = Database.getAllPlans().stream()
                       .filter(p -> p.getStatus().equals("Approved") ||
                                    p.getStatus().equals("Approved (Awaiting Client Pickup)") ||
                                    p.getStatus().equals("Completed"))
                       .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll).size();

        int deferredOrRejected = Database.getAllPlans().stream()
                                 .filter(p -> p.getStatus().equals("Deferred") ||
                                              p.getStatus().equals("Rejected") ||
                                              p.getStatus().equals("Rejected (to Planning)") ||
                                              p.getStatus().equals("Deferred (to Planning)") ||
                                              p.getStatus().equals("Rejected by Structural (to Planning)") ||
                                              p.getStatus().equals("Deferred by Structural (Awaiting Clarification)") ||
                                              p.getStatus().equals("Rejected (to Reception for Client)") ||
                                              p.getStatus().equals("Client Notified (Awaiting Resubmission)"))
                                 .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll).size();

        cardsPanel.updateCard(0, "Total Pending", pending, new Color(255, 193, 7));
        cardsPanel.updateCard(1, "Total Approved", approved, new Color(40, 167, 69));
        cardsPanel.updateCard(2, "Total Deferred/Rejected", deferredOrRejected, new Color(220, 53, 69));

        // Update table with ALL plans for Reception
        List<Plan> allPlans = Database.getAllPlans();
        allPlansTablePanel.updateTable(allPlans);

        // Clear details if no plan is selected or if selected plan is no longer relevant
        if (selectedPlan != null && !allPlans.stream().anyMatch(p -> p.getId() == selectedPlan.getId())) {
            clearDetails();
        } else if (selectedPlan != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanDetails();
        }
    }
}