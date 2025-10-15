package doclink.ui.panels.structural;

import doclink.Database;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StructuralReviewPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    private DashboardTablePanel tablePanel;

    // Plan details panel components
    private JLabel planIdLabel, applicantNameLabel, plotNoLabel, statusLabel;
    private JTextArea remarksArea;
    private JButton approveToCommitteeButton, rejectToPlanningButton, deferInStructuralButton;

    private Plan selectedPlan;
    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public StructuralReviewPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
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

        JLabel title = new JLabel("Structural Review & Actions");
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
        approveToCommitteeButton = createStyledButton("Approve (to Committee)", new Color(40, 167, 69)); // Green
        approveToCommitteeButton.addActionListener(e -> approveToCommittee());
        panel.add(approveToCommitteeButton, gbc);

        gbc.gridy++;
        rejectToPlanningButton = createStyledButton("Reject (to Planning)", new Color(220, 53, 69)); // Red
        rejectToPlanningButton.addActionListener(e -> rejectToPlanning());
        panel.add(rejectToPlanningButton, gbc);

        gbc.gridy++;
        deferInStructuralButton = createStyledButton("Defer (Awaiting Clarification)", new Color(255, 193, 7)); // Yellow
        deferInStructuralButton.addActionListener(e -> deferInStructural());
        panel.add(deferInStructuralButton, gbc);

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
                boolean isUnderStructuralReview = selectedPlan.getStatus().equals("Under Review (Structural)");
                boolean isDeferredInStructural = selectedPlan.getStatus().equals("Deferred by Structural (Awaiting Clarification)");

                approveToCommitteeButton.setEnabled(isUnderStructuralReview || isDeferredInStructural);
                rejectToPlanningButton.setEnabled(isUnderStructuralReview || isDeferredInStructural);
                deferInStructuralButton.setEnabled(isUnderStructuralReview); // Can only defer if currently under review, not if already deferred
            }
        }
    }

    private void approveToCommittee() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Structural)") && !selectedPlan.getStatus().equals("Deferred by Structural (Awaiting Clarification)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Structural review or deferred for clarification.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        Database.updatePlanStatus(selectedPlan.getId(), "Approved by Structural (to Committee)", "Approved by Structural Section. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Committee", "Approved by Structural", remarks));

        JOptionPane.showMessageDialog(this, "Plan approved and forwarded to Technical Committee.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void rejectToPlanning() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Structural)") && !selectedPlan.getStatus().equals("Deferred by Structural (Awaiting Clarification)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Structural review or deferred for clarification.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add remarks for rejecting the plan.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Rejected by Structural (to Planning)", "Rejected by Structural Section. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Rejected by Structural", remarks));

        JOptionPane.showMessageDialog(this, "Plan rejected and returned to Planning Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void deferInStructural() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Structural)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under Structural review to be deferred.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add remarks for deferral.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Deferred by Structural (Awaiting Clarification)", "Deferred by Structural Section, awaiting clarification. " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), currentUser.getRole(), "Deferred by Structural", remarks));

        JOptionPane.showMessageDialog(this, "Plan deferred, awaiting clarification within Structural Section.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void clearDetails() {
        selectedPlan = null;
        planIdLabel.setText("N/A");
        applicantNameLabel.setText("N/A");
        plotNoLabel.setText("N/A");
        statusLabel.setText("N/A");
        remarksArea.setText("");
        approveToCommitteeButton.setEnabled(false);
        rejectToPlanningButton.setEnabled(false);
        deferInStructuralButton.setEnabled(false);
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Structural
        int plansForReview = Database.getPlansByStatus("Under Review (Structural)").size();
        int deferredForClarification = Database.getPlansByStatus("Deferred by Structural (Awaiting Clarification)").size();
        int approvedToCommittee = Database.getPlansByStatus("Approved by Structural (to Committee)").size();

        cardsPanel.updateCard(0, "Plans for Structural Review", plansForReview, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Deferred (Awaiting Clarification)", deferredForClarification, new Color(255, 165, 0)); // Orange
        cardsPanel.updateCard(2, "Approved (to Committee)", approvedToCommittee, new Color(40, 167, 69)); // Green

        // Update table with plans for Structural review and deferred plans
        List<Plan> structuralPlans = new ArrayList<>();
        structuralPlans.addAll(Database.getPlansByStatus("Under Review (Structural)"));
        structuralPlans.addAll(Database.getPlansByStatus("Deferred by Structural (Awaiting Clarification)"));
        tablePanel.updateTable(structuralPlans);

        // Clear details if no plan is selected or if selected plan is no longer relevant
        if (selectedPlan != null && !structuralPlans.stream().anyMatch(p -> p.getId() == selectedPlan.getId())) {
            clearDetails();
        } else if (selectedPlan != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanDetails();
        }
    }
}