package doclink.ui.views;

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

public class ReceptionView extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;

    private DashboardCardsPanel cardsPanel;
    private DashboardTablePanel submittedPlansTablePanel; // Renamed for clarity
    private DashboardTablePanel paymentPlansTablePanel; // New table for payment processing

    // Form components for New Plan Submission
    private JTextField applicantNameField;
    private JTextField contactField;
    private JTextField plotNoField;
    private JTextField locationField;
    private JCheckBox sitePlanCb, titleDeedCb, drawingsCb, otherDocsCb;
    private JTextArea remarksArea;

    // Form components for Payment Processing
    private JLabel paymentPlanIdLabel, paymentApplicantNameLabel, paymentPlotNoLabel, paymentAmountLabel;
    private JTextField receiptNoField;
    private JButton recordPaymentButton;
    private Plan selectedPlanForPayment;

    public ReceptionView(User user, Dashboard parentDashboard) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Very light grey

        // Top section: Cards
        cardsPanel = new DashboardCardsPanel();
        add(cardsPanel, BorderLayout.NORTH);

        // Center section: Tabbed Pane for Submission Form and Plans Table
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // New Plan Submission Tab
        JPanel submissionPanel = createSubmissionFormPanel();
        tabbedPane.addTab("New Plan Submission", submissionPanel);

        // Submitted Plans Table Tab
        submittedPlansTablePanel = new DashboardTablePanel();
        tabbedPane.addTab("Submitted Plans", submittedPlansTablePanel);

        // Payment Processing Tab
        JPanel paymentProcessingPanel = createPaymentProcessingPanel();
        tabbedPane.addTab("Payment Processing", paymentProcessingPanel);

        add(tabbedPane, BorderLayout.CENTER);

        refreshData(); // Load initial data
    }

    private JPanel createSubmissionFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("New Building Plan Submission");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(26, 35, 126));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        // Applicant Details
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Applicant Name:"), gbc);
        gbc.gridx = 1;
        applicantNameField = new JTextField(25);
        panel.add(applicantNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Contact:"), gbc);
        gbc.gridx = 1;
        contactField = new JTextField(25);
        panel.add(contactField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Plot No:"), gbc);
        gbc.gridx = 1;
        plotNoField = new JTextField(25);
        panel.add(plotNoField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        locationField = new JTextField(25);
        panel.add(locationField, gbc);

        // Document Checklist
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JLabel docTitle = new JLabel("Document Checklist:");
        docTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        docTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        panel.add(docTitle, gbc);

        gbc.gridy++;
        sitePlanCb = new JCheckBox("Site Plan");
        panel.add(sitePlanCb, gbc);
        gbc.gridy++;
        titleDeedCb = new JCheckBox("Title Deed");
        panel.add(titleDeedCb, gbc);
        gbc.gridy++;
        drawingsCb = new JCheckBox("Architectural Drawings");
        panel.add(drawingsCb, gbc);
        gbc.gridy++;
        otherDocsCb = new JCheckBox("Other Supporting Documents");
        panel.add(otherDocsCb, gbc);

        // Remarks
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Remarks:"), gbc);
        gbc.gridy++;
        remarksArea = new JTextArea(5, 25);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(remarksArea);
        panel.add(scrollPane, gbc);

        // Submit Button
        gbc.gridy++;
        JButton submitButton = new JButton("Submit Plan");
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        submitButton.setBackground(new Color(0, 123, 255));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setOpaque(true);
        submitButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { submitButton.setBackground(new Color(0, 100, 200)); }
            public void mouseExited(java.awt.event.MouseEvent evt) { submitButton.setBackground(new Color(0, 123, 255)); }
        });
        submitButton.addActionListener(e -> submitNewPlan());
        panel.add(submitButton, gbc);

        return panel;
    }

    private JPanel createPaymentProcessingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7); // 70% for table, 30% for details
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(Color.WHITE);

        paymentPlansTablePanel = new DashboardTablePanel();
        paymentPlansTablePanel.getPlansTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && paymentPlansTablePanel.getPlansTable().getSelectedRow() != -1) {
                    loadSelectedPlanForPayment();
                }
            }
        });
        splitPane.setLeftComponent(paymentPlansTablePanel);

        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Payment Details");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(26, 35, 126));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        detailsPanel.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        detailsPanel.add(new JLabel("Plan ID:"), gbc);
        gbc.gridx = 1;
        paymentPlanIdLabel = new JLabel("N/A");
        detailsPanel.add(paymentPlanIdLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        detailsPanel.add(new JLabel("Applicant:"), gbc);
        gbc.gridx = 1;
        paymentApplicantNameLabel = new JLabel("N/A");
        detailsPanel.add(paymentApplicantNameLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        detailsPanel.add(new JLabel("Plot No:"), gbc);
        gbc.gridx = 1;
        paymentPlotNoLabel = new JLabel("N/A");
        detailsPanel.add(paymentPlotNoLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        detailsPanel.add(new JLabel("Amount Due:"), gbc);
        gbc.gridx = 1;
        paymentAmountLabel = new JLabel("N/A");
        detailsPanel.add(paymentAmountLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        detailsPanel.add(new JLabel("Receipt No:"), gbc);
        gbc.gridx = 1;
        receiptNoField = new JTextField(15);
        detailsPanel.add(receiptNoField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        recordPaymentButton = createStyledButton("Record Payment");
        recordPaymentButton.addActionListener(e -> recordPayment());
        recordPaymentButton.setEnabled(false); // Initially disabled
        detailsPanel.add(recordPaymentButton, gbc);

        // Add some vertical glue to push components to the top
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1.0; // Make this row expand vertically
        detailsPanel.add(Box.createVerticalGlue(), gbc);

        splitPane.setRightComponent(detailsPanel);
        panel.add(splitPane, BorderLayout.CENTER);

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

    private void submitNewPlan() {
        String applicantName = applicantNameField.getText();
        String contact = contactField.getText();
        String plotNo = plotNoField.getText();
        String location = locationField.getText();
        String remarks = remarksArea.getText();

        if (applicantName.isEmpty() || contact.isEmpty() || plotNo.isEmpty() || location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all applicant details.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Document> documents = new ArrayList<>();
        boolean allAttached = true;
        if (sitePlanCb.isSelected()) documents.add(new Document("Site Plan", null, true)); else allAttached = false;
        if (titleDeedCb.isSelected()) documents.add(new Document("Title Deed", null, true)); else allAttached = false;
        if (drawingsCb.isSelected()) documents.add(new Document("Architectural Drawings", null, true)); else allAttached = false;
        if (otherDocsCb.isSelected()) documents.add(new Document("Other Documents", null, true)); else allAttached = false;

        if (!allAttached) {
            int confirm = JOptionPane.showConfirmDialog(this, "Some documents are not attached. Do you want to proceed and return to applicant?", "Incomplete Submission", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                // For now, just log and don't forward. Status remains 'Submitted' but with a remark.
                Plan newPlan = new Plan(applicantName, contact, plotNo, location, LocalDate.now(), "Submitted", "Incomplete documents, returned to applicant.");
                Database.addPlan(newPlan, documents);
                Database.addLog(new Log(newPlan.getId(), currentUser.getRole(), "Client", "Incomplete Submission", "Documents missing."));
                JOptionPane.showMessageDialog(this, "Plan marked as incomplete and returned to applicant.", "Submission Info", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                refreshData();
                return;
            } else {
                return; // Stay on form
            }
        }

        // If complete, forward to Planning Department
        Plan newPlan = new Plan(applicantName, contact, plotNo, location, LocalDate.now(), "Under Review (Planning)", remarks);
        Database.addPlan(newPlan, documents);

        // Log the action
        Database.addLog(new Log(newPlan.getId(), currentUser.getRole(), "Planning", "Forwarded for Review", "Initial submission, documents verified."));

        JOptionPane.showMessageDialog(this, "Plan submitted successfully and forwarded to Planning Department.", "Submission Success", JOptionPane.INFORMATION_MESSAGE);
        clearForm();
        refreshData();
        parentDashboard.showRoleDashboard("Reception"); // Refresh current view
    }

    private void clearForm() {
        applicantNameField.setText("");
        contactField.setText("");
        plotNoField.setText("");
        locationField.setText("");
        remarksArea.setText("");
        sitePlanCb.setSelected(false);
        titleDeedCb.setSelected(false);
        drawingsCb.setSelected(false);
        otherDocsCb.setSelected(false);
    }

    private void loadSelectedPlanForPayment() {
        int selectedRow = paymentPlansTablePanel.getPlansTable().getSelectedRow();
        if (selectedRow != -1) {
            int planId = (int) paymentPlansTablePanel.getPlansTable().getValueAt(selectedRow, 0);
            selectedPlanForPayment = Database.getPlanById(planId);

            if (selectedPlanForPayment != null) {
                paymentPlanIdLabel.setText(String.valueOf(selectedPlanForPayment.getId()));
                paymentApplicantNameLabel.setText(selectedPlanForPayment.getApplicantName());
                paymentPlotNoLabel.setText(selectedPlanForPayment.getPlotNo());

                Billing billing = Database.getBillingByPlanId(planId);
                if (billing != null) {
                    paymentAmountLabel.setText(String.format("%.2f", billing.getAmount()));
                    receiptNoField.setText(billing.getReceiptNo() != null ? billing.getReceiptNo() : "");
                    recordPaymentButton.setEnabled(billing.getReceiptNo() == null); // Enable only if not already paid
                } else {
                    paymentAmountLabel.setText("N/A");
                    receiptNoField.setText("");
                    recordPaymentButton.setEnabled(false);
                }
            }
        }
    }

    private void recordPayment() {
        if (selectedPlanForPayment == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlanForPayment.getStatus().equals("Awaiting Payment")) {
            JOptionPane.showMessageDialog(this, "This plan is not awaiting payment.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String receiptNo = receiptNoField.getText().trim();
        if (receiptNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a receipt number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Billing billing = Database.getBillingByPlanId(selectedPlanForPayment.getId());
        if (billing != null) {
            Database.updateBillingPayment(billing.getId(), receiptNo);
            Database.updatePlanStatus(selectedPlanForPayment.getId(), "Payment Received", "Payment recorded by Reception. Receipt No: " + receiptNo);
            Database.addLog(new Log(selectedPlanForPayment.getId(), currentUser.getRole(), "Planning", "Payment Recorded", "Receipt No: " + receiptNo));

            JOptionPane.showMessageDialog(this, "Payment recorded successfully. Plan status updated to 'Payment Received'.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearPaymentDetails();
            refreshData();
            parentDashboard.showRoleDashboard("Reception"); // Refresh current view
        } else {
            JOptionPane.showMessageDialog(this, "No billing record found for this plan.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearPaymentDetails() {
        selectedPlanForPayment = null;
        paymentPlanIdLabel.setText("N/A");
        paymentApplicantNameLabel.setText("N/A");
        paymentPlotNoLabel.setText("N/A");
        paymentAmountLabel.setText("N/A");
        receiptNoField.setText("");
        recordPaymentButton.setEnabled(false);
    }

    @Override
    public void refreshData() {
        // Update cards
        int pendingReception = Database.getPlansByStatus("Submitted").size(); // Plans just submitted, potentially incomplete
        int awaitingPayment = Database.getPlansByStatus("Awaiting Payment").size();
        int forwardedToPlanning = Database.getPlansByStatus("Under Review (Planning)").size();

        cardsPanel.updateCard(0, "New Submissions", pendingReception, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Awaiting Payment", awaitingPayment, new Color(255, 165, 0)); // Orange
        cardsPanel.updateCard(2, "Forwarded to Planning", forwardedToPlanning, new Color(40, 167, 69)); // Green

        // Update 'Submitted Plans' table with plans relevant to Reception
        List<Plan> receptionSubmittedPlans = new ArrayList<>();
        receptionSubmittedPlans.addAll(Database.getPlansByStatus("Submitted"));
        receptionSubmittedPlans.addAll(Database.getPlansByStatus("Under Review (Planning)")); // To see what's been forwarded
        receptionSubmittedPlans.addAll(Database.getPlansByStatus("Awaiting Payment")); // Also show here for overview
        receptionSubmittedPlans.addAll(Database.getPlansByStatus("Payment Received")); // And after payment
        submittedPlansTablePanel.updateTable(receptionSubmittedPlans);

        // Update 'Payment Processing' table with plans specifically awaiting payment
        List<Plan> plansAwaitingPayment = Database.getPlansByStatus("Awaiting Payment");
        paymentPlansTablePanel.updateTable(plansAwaitingPayment);

        // Clear details if no plan is selected or if selected plan is no longer relevant
        if (selectedPlanForPayment != null && !plansAwaitingPayment.stream().anyMatch(p -> p.getId() == selectedPlanForPayment.getId())) {
            clearPaymentDetails();
        } else if (selectedPlanForPayment != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanForPayment();
        }
    }
}