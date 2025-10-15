package doclink.ui.panels.client;

import doclink.Database;
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

public class ClientDashboardPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    private DashboardTablePanel tablePanel;

    // Plan details panel components
    private JLabel planIdLabel, applicantNameLabel, plotNoLabel, statusLabel;
    private JTextArea resubmitRemarksArea; // New text area for resubmission remarks
    private JButton viewDocumentsButton, resubmitButton; // New resubmit button

    private Plan selectedPlan;
    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public ClientDashboardPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
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

        JLabel title = new JLabel("Plan Details");
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

        // Remarks for resubmission
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Resubmission Remarks:"), gbc);
        gbc.gridy++;
        resubmitRemarksArea = new JTextArea(3, 20);
        resubmitRemarksArea.setLineWrap(true);
        resubmitRemarksArea.setWrapStyleWord(true);
        JScrollPane scrollPaneRemarks = new JScrollPane(resubmitRemarksArea);
        panel.add(scrollPaneRemarks, gbc);

        // Resubmit Button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        resubmitButton = createStyledButton("Resubmit Plan", new Color(0, 123, 255)); // Blue
        resubmitButton.addActionListener(e -> resubmitPlan());
        resubmitButton.setEnabled(false); // Initially disabled
        panel.add(resubmitButton, gbc);

        // View Documents Button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        viewDocumentsButton = createStyledButton("View Documents", new Color(108, 117, 125)); // Grey
        viewDocumentsButton.addActionListener(e -> viewPlanDocuments(selectedPlan));
        viewDocumentsButton.setEnabled(false); // Initially disabled
        panel.add(viewDocumentsButton, gbc);

        // Add some vertical glue to push components to the top
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1.0; // Make this row expand vertically
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
                resubmitRemarksArea.setText(""); // Clear remarks for new action
                viewDocumentsButton.setEnabled(true); // Enable view documents if a plan is selected

                // Enable resubmit button for rejected/deferred statuses
                String currentStatus = selectedPlan.getStatus();
                boolean canResubmit = currentStatus.equals("Rejected") ||
                                      currentStatus.equals("Deferred") ||
                                      currentStatus.equals("Rejected (to Planning)") ||
                                      currentStatus.equals("Deferred (to Planning)") ||
                                      currentStatus.equals("Rejected by Structural (to Planning)") ||
                                      currentStatus.equals("Deferred by Structural (Awaiting Clarification)") ||
                                      currentStatus.equals("Rejected (to Reception for Client)") ||
                                      currentStatus.equals("Client Notified (Awaiting Resubmission)");
                resubmitButton.setEnabled(canResubmit);
            }
        }
    }

    private void resubmitPlan() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String currentStatus = selectedPlan.getStatus();
        boolean canResubmit = currentStatus.equals("Rejected") ||
                              currentStatus.equals("Deferred") ||
                              currentStatus.equals("Rejected (to Planning)") ||
                              currentStatus.equals("Deferred (to Planning)") ||
                              currentStatus.equals("Rejected by Structural (to Planning)") ||
                              currentStatus.equals("Deferred by Structural (Awaiting Clarification)") ||
                              currentStatus.equals("Rejected (to Reception for Client)") ||
                              currentStatus.equals("Client Notified (Awaiting Resubmission)");

        if (!canResubmit) {
            JOptionPane.showMessageDialog(this, "This plan cannot be resubmitted from its current status: " + currentStatus, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to resubmit this plan? It will be sent back to Planning for review.", "Confirm Resubmission", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.NO_OPTION) {
            return;
        }

        String remarks = resubmitRemarksArea.getText().trim();
        String newRemarks = "Client resubmitted plan. Original remarks: " + (selectedPlan.getRemarks() != null ? selectedPlan.getRemarks() : "N/A") + ". Client's resubmission remarks: " + (remarks.isEmpty() ? "No additional remarks." : remarks);

        Database.updatePlanStatus(selectedPlan.getId(), "Under Review (Planning)", newRemarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Plan Resubmitted by Client", remarks));

        JOptionPane.showMessageDialog(this, "Plan resubmitted successfully and sent to Planning for review.", "Resubmission Success", JOptionPane.INFORMATION_MESSAGE);
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
        resubmitRemarksArea.setText("");
        viewDocumentsButton.setEnabled(false);
        resubmitButton.setEnabled(false);
    }

    @Override
    public void refreshData() {
        String clientEmail = currentUser.getEmail();

        // Update cards with relevant counts for Client
        int myPendingPlans = Database.getPlansByApplicantEmailAndStatus(clientEmail, "Submitted").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Awaiting Payment").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Under Review (Planning)").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Under Review (Committee)").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Under Review (Director)").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Under Review (Structural)").size();
        int myApprovedPlans = Database.getPlansByApplicantEmailAndStatus(clientEmail, "Approved").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Approved (Awaiting Client Pickup)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Completed").size();
        int myRejectedPlans = Database.getPlansByApplicantEmailAndStatus(clientEmail, "Rejected").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Deferred").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Rejected (to Planning)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Deferred (to Planning)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Rejected by Structural (to Planning)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Deferred by Structural (Awaiting Clarification)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Rejected (to Reception for Client)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Client Notified (Awaiting Resubmission)").size();


        cardsPanel.updateCard(0, "My Pending Plans", myPendingPlans, new Color(255, 193, 7));
        cardsPanel.updateCard(1, "My Approved Plans", myApprovedPlans, new Color(40, 167, 69));
        cardsPanel.updateCard(2, "My Rejected/Deferred Plans", myRejectedPlans, new Color(220, 53, 69));

        // Update table with client's plans
        List<Plan> clientPlans = Database.getPlansByApplicantEmail(clientEmail);
        tablePanel.updateTable(clientPlans);

        // Clear details if no plan is selected or if selected plan is no longer relevant
        if (selectedPlan != null && !clientPlans.stream().anyMatch(p -> p.getId() == selectedPlan.getId())) {
            clearDetails();
        } else if (selectedPlan != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanDetails();
        }
    }
}