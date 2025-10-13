package doclink.ui.views;

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
import java.util.List;

public class CommitteeView extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;

    private DashboardCardsPanel cardsPanel;
    private DashboardTablePanel tablePanel;

    // Plan details panel components
    private JLabel planIdLabel, applicantNameLabel, plotNoLabel, locationLabel, referenceNoLabel, statusLabel, remarksLabel;
    private JList<String> documentList;
    private DefaultListModel<String> documentListModel;
    private JButton approveButton, rejectButton, deferButton, returnToPlanningButton;

    private Plan selectedPlan;

    public CommitteeView(User user, Dashboard parentDashboard) {
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
        panel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        locationLabel = new JLabel("N/A");
        panel.add(locationLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Reference No:"), gbc);
        gbc.gridx = 1;
        referenceNoLabel = new JLabel("N/A");
        panel.add(referenceNoLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        statusLabel = new JLabel("N/A");
        panel.add(statusLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Remarks:"), gbc);
        gbc.gridx = 1;
        remarksLabel = new JLabel("N/A");
        panel.add(remarksLabel, gbc);

        // Document List
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JLabel docTitle = new JLabel("Attached Documents:");
        docTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        docTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        panel.add(docTitle, gbc);

        gbc.gridy++;
        documentListModel = new DefaultListModel<>();
        documentList = new JList<>(documentListModel);
        documentList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane docScrollPane = new JScrollPane(documentList);
        docScrollPane.setPreferredSize(new Dimension(200, 100));
        panel.add(docScrollPane, gbc);

        // Action Buttons
        gbc.gridy++;
        approveButton = createStyledButton("Approve Plan");
        approveButton.addActionListener(e -> approvePlan());
        panel.add(approveButton, gbc);

        gbc.gridy++;
        rejectButton = createStyledButton("Reject Plan");
        rejectButton.addActionListener(e -> rejectPlan());
        panel.add(rejectButton, gbc);

        gbc.gridy++;
        deferButton = createStyledButton("Defer Plan");
        deferButton.addActionListener(e -> deferPlan());
        panel.add(deferButton, gbc);

        gbc.gridy++;
        returnToPlanningButton = createStyledButton("Return to Planning");
        returnToPlanningButton.addActionListener(e -> returnToPlanning());
        panel.add(returnToPlanningButton, gbc);

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
                locationLabel.setText(selectedPlan.getLocation());
                referenceNoLabel.setText(selectedPlan.getReferenceNo() != null ? selectedPlan.getReferenceNo() : "N/A");
                statusLabel.setText(selectedPlan.getStatus());
                remarksLabel.setText(selectedPlan.getRemarks() != null ? selectedPlan.getRemarks() : "N/A");

                // Load documents
                documentListModel.clear();
                List<Document> documents = Database.getDocumentsByPlanId(planId);
                if (documents.isEmpty()) {
                    documentListModel.addElement("No documents attached.");
                } else {
                    for (Document doc : documents) {
                        documentListModel.addElement(doc.getDocName() + (doc.isAttached() ? " (Attached)" : " (Missing)"));
                    }
                }

                // Enable/disable buttons based on status
                boolean isUnderReviewCommittee = selectedPlan.getStatus().equals("Under Review (Committee)");
                approveButton.setEnabled(isUnderReviewCommittee);
                rejectButton.setEnabled(isUnderReviewCommittee);
                deferButton.setEnabled(isUnderReviewCommittee);
                returnToPlanningButton.setEnabled(isUnderReviewCommittee);
            }
        }
    }

    private void approvePlan() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Committee)")) {
            JOptionPane.showMessageDialog(this, "Only plans 'Under Review (Committee)' can be approved.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = JOptionPane.showInputDialog(this, "Enter remarks for approval (optional):", "Approve Plan", JOptionPane.QUESTION_MESSAGE);
        if (remarks == null) return; // User cancelled

        Database.updatePlanStatus(selectedPlan.getId(), "Under Review (Director)", remarks.isEmpty() ? "Approved by Committee, forwarded to Director." : remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Director", "Approved by Committee", remarks));

        JOptionPane.showMessageDialog(this, "Plan approved by Committee and forwarded to Director.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
        parentDashboard.showRoleDashboard("Committee");
    }

    private void rejectPlan() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Committee)")) {
            JOptionPane.showMessageDialog(this, "Only plans 'Under Review (Committee)' can be rejected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = JOptionPane.showInputDialog(this, "Enter reasons for rejection:", "Reject Plan", JOptionPane.QUESTION_MESSAGE);
        if (remarks == null || remarks.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Remarks are required for rejection.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Rejected", remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Client", "Rejected by Committee", remarks));

        JOptionPane.showMessageDialog(this, "Plan rejected by Committee.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
        parentDashboard.showRoleDashboard("Committee");
    }

    private void deferPlan() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Committee)")) {
            JOptionPane.showMessageDialog(this, "Only plans 'Under Review (Committee)' can be deferred.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = JOptionPane.showInputDialog(this, "Enter reasons for deferral:", "Defer Plan", JOptionPane.QUESTION_MESSAGE);
        if (remarks == null || remarks.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Remarks are required for deferral.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Deferred", remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Client", "Deferred by Committee", remarks));

        JOptionPane.showMessageDialog(this, "Plan deferred by Committee.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
        parentDashboard.showRoleDashboard("Committee");
    }

    private void returnToPlanning() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Committee)")) {
            JOptionPane.showMessageDialog(this, "Only plans 'Under Review (Committee)' can be returned to Planning.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = JOptionPane.showInputDialog(this, "Enter reasons for returning to Planning:", "Return to Planning", JOptionPane.QUESTION_MESSAGE);
        if (remarks == null || remarks.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Remarks are required for returning to Planning.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Under Review (Planning)", remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Returned for Clarification", remarks));

        JOptionPane.showMessageDialog(this, "Plan returned to Planning Department for clarification.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
        parentDashboard.showRoleDashboard("Committee");
    }

    private void clearDetails() {
        selectedPlan = null;
        planIdLabel.setText("N/A");
        applicantNameLabel.setText("N/A");
        plotNoLabel.setText("N/A");
        locationLabel.setText("N/A");
        referenceNoLabel.setText("N/A");
        statusLabel.setText("N/A");
        remarksLabel.setText("N/A");
        documentListModel.clear();
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
        deferButton.setEnabled(false);
        returnToPlanningButton.setEnabled(false);
    }

    @Override
    public void refreshData() {
        // Update cards
        int plansForReview = Database.getPlansByStatus("Under Review (Committee)").size();
        int approvedByCommittee = Database.getPlansByStatus("Under Review (Director)").size(); // Plans approved by committee, waiting for director
        int deferredOrRejected = Database.getPlansByStatus("Deferred").size() + Database.getPlansByStatus("Rejected").size();

        cardsPanel.updateCard(0, "Plans for Review", plansForReview, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Forwarded to Director", approvedByCommittee, new Color(40, 167, 69)); // Green
        cardsPanel.updateCard(2, "Deferred/Rejected", deferredOrRejected, new Color(220, 53, 69)); // Red

        // Update table with plans for Committee review
        List<Plan> committeePlans = Database.getPlansByStatus("Under Review (Committee)");
        tablePanel.updateTable(committeePlans);

        // Clear details if no plan is selected or if selected plan is no longer relevant
        if (selectedPlan != null && !committeePlans.stream().anyMatch(p -> p.getId() == selectedPlan.getId())) {
            clearDetails();
        } else if (selectedPlan != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanDetails();
        }
    }
}