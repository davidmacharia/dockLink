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
import javax.swing.event.ListSelectionEvent; // Added import
import javax.swing.event.ListSelectionListener; // Added import
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ReceptionPaymentPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    private DashboardTablePanel paymentTablePanel;

    // Components for Payment Processing
    private JLabel paymentPlanIdLabel, paymentApplicantNameLabel, paymentPlotNoLabel, paymentStatusLabel, paymentAmountLabel;
    private JTextField receiptNoField;
    private JButton attachReceiptButton, paymentViewDocumentsButton;
    private Plan selectedPlanForPayment;
    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public ReceptionPaymentPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Reverted to light grey

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7); // 70% for table, 30% for details
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(new Color(245, 247, 250)); // Reverted to light grey

        paymentTablePanel = new DashboardTablePanel();
        paymentTablePanel.getPlansTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && paymentTablePanel.getPlansTable().getSelectedRow() != -1) {
                    loadSelectedPlanForPaymentDetails();
                }
            }
        });
        splitPane.setLeftComponent(paymentTablePanel);

        JPanel paymentDetailsPanel = createPaymentDetailsPanel();
        splitPane.setRightComponent(paymentDetailsPanel);

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createPaymentDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Payment Details & Actions");
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
        paymentPlanIdLabel = new JLabel("N/A");
        panel.add(paymentPlanIdLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Applicant:"), gbc);
        gbc.gridx = 1;
        paymentApplicantNameLabel = new JLabel("N/A");
        panel.add(paymentApplicantNameLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Plot No:"), gbc);
        gbc.gridx = 1;
        paymentPlotNoLabel = new JLabel("N/A");
        panel.add(paymentPlotNoLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        paymentStatusLabel = new JLabel("N/A");
        panel.add(paymentStatusLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Billing Amount:"), gbc);
        gbc.gridx = 1;
        paymentAmountLabel = new JLabel("N/A");
        panel.add(paymentAmountLabel, gbc);

        // Receipt Number
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Receipt No:"), gbc);
        gbc.gridx = 1;
        receiptNoField = new JTextField(15);
        panel.add(receiptNoField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        attachReceiptButton = createStyledButton("Attach Receipt & Forward to Planning");
        attachReceiptButton.addActionListener(e -> attachReceiptAndForward());
        panel.add(attachReceiptButton, gbc);

        // View Documents Button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        paymentViewDocumentsButton = createStyledButton("View Documents", new Color(108, 117, 125)); // Grey
        paymentViewDocumentsButton.addActionListener(e -> viewPlanDocuments(selectedPlanForPayment));
        paymentViewDocumentsButton.setEnabled(false); // Initially disabled
        panel.add(paymentViewDocumentsButton, gbc);

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

    private void loadSelectedPlanForPaymentDetails() {
        int selectedRow = paymentTablePanel.getPlansTable().getSelectedRow();
        if (selectedRow != -1) {
            int planId = (int) paymentTablePanel.getPlansTable().getValueAt(selectedRow, 0);
            selectedPlanForPayment = Database.getPlanById(planId);

            if (selectedPlanForPayment != null) {
                paymentPlanIdLabel.setText(String.valueOf(selectedPlanForPayment.getId()));
                paymentApplicantNameLabel.setText(selectedPlanForPayment.getApplicantName());
                paymentPlotNoLabel.setText(selectedPlanForPayment.getPlotNo());
                paymentStatusLabel.setText(selectedPlanForPayment.getStatus());

                Billing billing = Database.getBillingByPlanId(planId);
                if (billing != null) {
                    paymentAmountLabel.setText(String.format("%.2f", billing.getAmount()));
                    receiptNoField.setText(billing.getReceiptNo() != null ? billing.getReceiptNo() : "");
                } else {
                    paymentAmountLabel.setText("N/A");
                    receiptNoField.setText("");
                }

                // Enable button only if status is "Awaiting Payment" and no receipt is attached yet
                boolean isAwaitingPayment = selectedPlanForPayment.getStatus().equals("Awaiting Payment");
                attachReceiptButton.setEnabled(isAwaitingPayment && (billing == null || billing.getReceiptNo() == null));
                paymentViewDocumentsButton.setEnabled(true); // Always enable view documents if a plan is selected
            }
        }
    }

    private void attachReceiptAndForward() {
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
        if (billing == null) {
            JOptionPane.showMessageDialog(this, "No billing record found for this plan.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Attempt to update billing payment and check for success/failure
        boolean success = Database.updateBillingPayment(billing.getId(), receiptNo);

        if (success) {
            Database.updatePlanStatus(selectedPlanForPayment.getId(), "Payment Received", "Payment received with receipt: " + receiptNo);
            Database.addLog(new Log(selectedPlanForPayment.getId(), currentUser.getRole(), "Planning", "Payment Received", "Receipt No: " + receiptNo));

            // Add the receipt as a document
            String receiptDocName = "Payment Receipt";
            String receiptFilePath = "Receipt No: " + receiptNo; // Store receipt number as path for display
            Document receiptDocument = new Document(selectedPlanForPayment.getId(), receiptDocName, receiptFilePath, "Generated");
            Database.addDocument(receiptDocument);

            JOptionPane.showMessageDialog(this, "Payment recorded and plan forwarded to Planning Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearPaymentDetails();
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Error: The receipt number '" + receiptNo + "' is already taken or another database error occurred. Please use a unique receipt number.", "Payment Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearPaymentDetails() {
        selectedPlanForPayment = null;
        paymentPlanIdLabel.setText("N/A");
        paymentApplicantNameLabel.setText("N/A");
        paymentPlotNoLabel.setText("N/A");
        paymentStatusLabel.setText("N/A");
        paymentAmountLabel.setText("N/A");
        receiptNoField.setText("");
        attachReceiptButton.setEnabled(false);
        paymentViewDocumentsButton.setEnabled(false);
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

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Reception
        int pending = Database.getPlansByStatus("Submitted").size() +
                      Database.getPlansByStatus("Awaiting Payment").size() +
                      Database.getPlansByStatus("Under Review (Planning)").size() +
                      Database.getPlansByStatus("Under Review (Committee)").size() +
                      Database.getPlansByStatus("Under Review (Director)").size() +
                      Database.getPlansByStatus("Under Review (Structural)").size();

        int approved = Database.getPlansByStatus("Approved").size() +
                       Database.getPlansByStatus("Approved (Awaiting Client Pickup)").size() +
                       Database.getPlansByStatus("Completed").size();

        int deferredOrRejected = Database.getPlansByStatus("Deferred").size() +
                                 Database.getPlansByStatus("Rejected").size() +
                                 Database.getPlansByStatus("Rejected (to Planning)").size() +
                                 Database.getPlansByStatus("Deferred (to Planning)").size() +
                                 Database.getPlansByStatus("Rejected by Structural (to Planning)").size() +
                                 Database.getPlansByStatus("Deferred by Structural (Awaiting Clarification)").size() +
                                 Database.getPlansByStatus("Rejected (to Reception for Client)").size() +
                                 Database.getPlansByStatus("Client Notified (Awaiting Resubmission)").size();

        cardsPanel.updateCard(0, "Total Pending", pending, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Total Approved", approved, new Color(40, 167, 69)); // Green
        cardsPanel.updateCard(2, "Total Deferred/Rejected", deferredOrRejected, new Color(220, 53, 69)); // Red

        // Update table for Payment Processing tab
        List<Plan> awaitingPaymentPlans = Database.getPlansByStatus("Awaiting Payment");
        paymentTablePanel.updateTable(awaitingPaymentPlans);

        // Clear payment details if selected plan is no longer in "Awaiting Payment" list
        if (selectedPlanForPayment != null && !awaitingPaymentPlans.stream().anyMatch(p -> p.getId() == selectedPlanForPayment.getId())) {
            clearPaymentDetails();
        } else if (selectedPlanForPayment != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanForPaymentDetails();
        }
    }
}