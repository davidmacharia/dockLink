package doclink.ui.views;

import doclink.Database;
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

public class CommitteeView extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;

    private DashboardCardsPanel cardsPanel;
    private DashboardTablePanel tablePanel;

    // Plan details panel components
    private JLabel planIdLabel, applicantNameLabel, plotNoLabel, statusLabel;
    private JTextArea remarksArea;
    private JButton approveToDirectorButton, rejectToPlanningButton, deferToPlanningButton;

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

        JLabel title = new JLabel("Plan Review & Actions");
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
        approveToDirectorButton = createStyledButton("Approve (to Director)", new Color(40, 167, 69)); // Green
        approveToDirectorButton.addActionListener(e -> approveToDirector());
        panel.add(approveToDirectorButton, gbc);

        gbc.gridy++;
        rejectToPlanningButton = createStyledButton("Reject (to Planning)", new Color(220, 53, 69)); // Red
        rejectToPlanningButton.addActionListener(e -> rejectToPlanning());
        panel.add(rejectToPlanningButton, gbc);

        gbc.gridy++;
        deferToPlanningButton = createStyledButton("Defer (to Planning)", new Color(255, 193, 7)); // Yellow
        deferToPlanningButton.addActionListener(e -> deferToPlanning());
        panel.add(deferToPlanningButton, gbc);

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
                boolean isUnderReviewCommittee = selectedPlan.getStatus().equals("Under Review (Committee)");
                boolean isApprovedByStructural = selectedPlan.getStatus().equals("Approved by Structural (to Committee)");

                approveToDirectorButton.setEnabled(isUnderReviewCommittee || isApprovedByStructural);
                rejectToPlanningButton.setEnabled(isUnderReviewCommittee || isApprovedByStructural);
                deferToPlanningButton.setEnabled(isUnderReviewCommittee || isApprovedByStructural);
            }
        }
    }

    private void approveToDirector() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Committee)") && !selectedPlan.getStatus().equals("Approved by Structural (to Committee)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Committee review or approved by Structural for Committee review.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        Database.updatePlanStatus(selectedPlan.getId(), "Under Review (Director)", "Approved by Committee for Director's Review. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Director", "Approved by Committee", remarks));

        JOptionPane.showMessageDialog(this, "Plan approved and forwarded to Director.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
        parentDashboard.showRoleDashboard("Committee"); // Refresh current view
    }

    private void rejectToPlanning() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Committee)") && !selectedPlan.getStatus().equals("Approved by Structural (to Committee)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Committee review or approved by Structural for Committee review.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add remarks for rejecting the plan.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Rejected", "Rejected by Committee. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Rejected by Committee", remarks));

        JOptionPane.showMessageDialog(this, "Plan rejected and returned to Planning Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
        parentDashboard.showRoleDashboard("Committee"); // Refresh current view
    }

    private void deferToPlanning() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Committee)") && !selectedPlan.getStatus().equals("Approved by Structural (to Committee)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Committee review or approved by Structural for Committee review.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add remarks for deferring the plan.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Deferred", "Deferred by Committee. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Deferred by Committee", remarks));

        JOptionPane.showMessageDialog(this, "Plan deferred and returned to Planning Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
        parentDashboard.showRoleDashboard("Committee"); // Refresh current view
    }

    private void clearDetails() {
        selectedPlan = null;
        planIdLabel.setText("N/A");
        applicantNameLabel.setText("N/A");
        plotNoLabel.setText("N/A");
        statusLabel.setText("N/A");
        remarksArea.setText("");
        approveToDirectorButton.setEnabled(false);
        rejectToPlanningButton.setEnabled(false);
        deferToPlanningButton.setEnabled(false);
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Committee
        List<Plan> plansForCommitteeReview = new ArrayList<>();
        plansForCommitteeReview.addAll(Database.getPlansByStatus("Under Review (Committee)"));
        plansForCommitteeReview.addAll(Database.getPlansByStatus("Approved by Structural (to Committee)"));

        int plansForReviewCount = plansForCommitteeReview.size();
        int approvedToDirector = Database.getPlansByStatus("Under Review (Director)").size(); // Plans forwarded to Director
        int returnedPlans = Database.getPlansByStatus("Rejected").size() + Database.getPlansByStatus("Deferred").size(); // Plans rejected/deferred by committee

        cardsPanel.updateCard(0, "Plans for Committee Review", plansForReviewCount, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Approved (to Director)", approvedToDirector, new Color(40, 167, 69)); // Green
        cardsPanel.updateCard(2, "Rejected/Deferred", returnedPlans, new Color(220, 53, 69)); // Red

        // Update table with plans for Committee review
        tablePanel.updateTable(plansForCommitteeReview);

        // Clear details if no plan is selected or if selected plan is no longer relevant
        if (selectedPlan != null && !plansForCommitteeReview.stream().anyMatch(p -> p.getId() == selectedPlan.getId())) {
            clearDetails();
        } else if (selectedPlan != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanDetails();
        }
    }
}