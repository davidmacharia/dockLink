package doclink.ui.panels.director;

import doclink.ApprovalCertificateGenerator;
import doclink.Database;
import doclink.DirectorDecisionLetterGenerator;
import doclink.models.Log;
import doclink.models.Plan;
import doclink.models.User;
import doclink.models.Document;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.DashboardTablePanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent; // Added import
import javax.swing.event.ListSelectionListener; // Added import
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class DirectorReviewPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    private DashboardTablePanel tablePanel;

    // Plan details panel components
    private JLabel planIdLabel, applicantNameLabel, plotNoLabel, statusLabel;
    private JTextArea remarksArea;
    private JButton approveStructuralButton, approveNoStructuralButton, notRecommendButton, deferButton, viewDocumentsButton;

    private Plan selectedPlan;
    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public DirectorReviewPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
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

        JLabel title = new JLabel("Plan Review & Actions");
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

        // Remarks for action
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Remarks for Action:"), gbc);
        gbc.gridy++;
        remarksArea = new JTextArea(3, 20);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(remarksArea);
        panel.add(scrollPane, gbc);

        // Action Buttons
        gbc.gridy++;
        approveStructuralButton = createStyledButton("Approve (to Structural)", new Color(40, 167, 69)); // Green
        approveStructuralButton.addActionListener(e -> approveForStructural());
        panel.add(approveStructuralButton, gbc);

        gbc.gridy++;
        approveNoStructuralButton = createStyledButton("Approve (No Structural Review)", new Color(0, 123, 255)); // Blue
        approveNoStructuralButton.addActionListener(e -> approveNoStructuralReview());
        panel.add(approveNoStructuralButton, gbc);

        gbc.gridy++;
        notRecommendButton = createStyledButton("Not Recommend (to Planning)", new Color(220, 53, 69)); // Red
        notRecommendButton.addActionListener(e -> notRecommendPlan());
        panel.add(notRecommendButton, gbc);

        gbc.gridy++;
        deferButton = createStyledButton("Defer (to Planning)", new Color(255, 193, 7)); // Yellow
        deferButton.addActionListener(e -> deferPlan());
        panel.add(deferButton, gbc);

        // View Documents Button
        gbc.gridy++;
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
                remarksArea.setText(""); // Clear remarks for new action

                // Enable/disable buttons based on status
                boolean isUnderReviewDirector = selectedPlan.getStatus().equals("Under Review (Director)");
                approveStructuralButton.setEnabled(isUnderReviewDirector);
                approveNoStructuralButton.setEnabled(isUnderReviewDirector);
                notRecommendButton.setEnabled(isUnderReviewDirector);
                deferButton.setEnabled(isUnderReviewDirector);
                viewDocumentsButton.setEnabled(true); // Always enable view documents if a plan is selected
            }
        }
    }

    private void approveForStructural() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Director)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Director's review.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        
        // Generate Director's Decision Letter
        String filePath = DirectorDecisionLetterGenerator.generateLetter(selectedPlan, "Approved for Structural Review", remarks);
        if (filePath != null) {
            Database.addDocument(new Document(selectedPlan.getId(), "Director's Approval Letter (Structural)", filePath, "Generated"));
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Approved by Director (to Reception for Structural)", "Approved by Director for Structural Review. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Reception", "Approved by Director for Structural", remarks));

        JOptionPane.showMessageDialog(this, "Plan approved and forwarded to Reception for Structural Section routing.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void approveNoStructuralReview() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Director)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Director's review.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();

        // Generate Approval Certificate
        String filePath = ApprovalCertificateGenerator.generateCertificate(selectedPlan, System.getProperty("user.home") + "/DocLink_Documents/" + (selectedPlan.getReferenceNo() != null ? selectedPlan.getReferenceNo() : "Plan_" + selectedPlan.getId()) + "_ApprovalCertificate.pdf");
        if (filePath != null) {
            Database.addDocument(new Document(selectedPlan.getId(), "Approval Certificate", filePath, "Generated"));
        }
        
        Database.updatePlanStatus(selectedPlan.getId(), "Approved (Awaiting Client Pickup)", "Approved by Director, no structural review needed. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Reception", "Approved (No Structural Review)", remarks));

        JOptionPane.showMessageDialog(this, "Plan approved (no structural review) and forwarded to Reception for client pickup.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void notRecommendPlan() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Director)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Director's review.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add remarks for not recommending the plan.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Generate Director's Decision Letter (Rejection)
        String filePath = DirectorDecisionLetterGenerator.generateLetter(selectedPlan, "Not Recommended (Rejected)", remarks);
        if (filePath != null) {
            Database.addDocument(new Document(selectedPlan.getId(), "Director's Rejection Letter", filePath, "Generated"));
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Rejected (to Planning)", "Not recommended by Director. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Not Recommended by Director", remarks));

        JOptionPane.showMessageDialog(this, "Plan not recommended and returned to Planning Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void deferPlan() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Director)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Director's review.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add remarks for deferral.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Generate Director's Decision Letter (Deferral)
        String filePath = DirectorDecisionLetterGenerator.generateLetter(selectedPlan, "Deferred", remarks);
        if (filePath != null) {
            Database.addDocument(new Document(selectedPlan.getId(), "Director's Deferral Letter", filePath, "Generated"));
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Deferred (to Planning)", "Deferred by Director. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Deferred by Director", remarks));

        JOptionPane.showMessageDialog(this, "Plan deferred and returned to Planning Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
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
        remarksArea.setText("");
        approveStructuralButton.setEnabled(false);
        approveNoStructuralButton.setEnabled(false);
        notRecommendButton.setEnabled(false);
        deferButton.setEnabled(false);
        viewDocumentsButton.setEnabled(false);
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Director
        int plansForReview = Database.getPlansByStatus("Under Review (Director)").size();
        int approvedToReceptionStructural = Database.getPlansByStatus("Approved by Director (to Reception for Structural)").size();
        int approvedToReceptionClient = Database.getPlansByStatus("Approved (Awaiting Client Pickup)").size();
        int returnedPlans = Database.getPlansByStatus("Rejected (to Planning)").size() + Database.getPlansByStatus("Deferred (to Planning)").size();

        cardsPanel.updateCard(0, "Plans for Director Review", plansForReview, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Approved (to Reception)", approvedToReceptionStructural + approvedToReceptionClient, new Color(40, 167, 69)); // Green
        cardsPanel.updateCard(2, "Returned Plans", returnedPlans, new Color(220, 53, 69)); // Red

        // Update table with plans for Director review
        List<Plan> directorPlans = Database.getPlansByStatus("Under Review (Director)");
        tablePanel.updateTable(directorPlans);

        // Clear details if no plan is selected or if selected plan is no longer relevant
        if (selectedPlan != null && !directorPlans.stream().anyMatch(p -> p.getId() == selectedPlan.getId())) {
            clearDetails();
        } else if (selectedPlan != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanDetails();
        }
    }
}