package doclink.ui.panels.structural;

import doclink.Database; // Added import
import doclink.models.Log;
import doclink.models.Plan;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.DashboardTablePanel;
import doclink.ui.components.DocumentViewerDialog; // NEW: Import DocumentViewerDialog

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
    private JButton approveToCommitteeButton, rejectToPlanningButton, deferInStructuralButton, viewDocumentsButton; // Added viewDocumentsButton

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

        // Remarks
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Remarks:"), gbc);
        gbc.gridy++;
        remarksArea = new JTextArea(3, 20);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(remarksArea);
        panel.add(scrollPane, gbc);

        // Action Buttons
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        approveToCommitteeButton = createStyledButton("Approve & Forward to Committee", new Color(40, 167, 69)); // Green
        approveToCommitteeButton.addActionListener(e -> approveAndForwardToCommittee());
        panel.add(approveToCommitteeButton, gbc);

        gbc.gridy++;
        rejectToPlanningButton = createStyledButton("Reject & Return to Planning", new Color(220, 53, 69)); // Red
        rejectToPlanningButton.addActionListener(e -> rejectAndReturnToPlanning());
        panel.add(rejectToPlanningButton, gbc);

        gbc.gridy++;
        deferInStructuralButton = createStyledButton("Defer (Awaiting Clarification)", new Color(255, 165, 0)); // Orange
        deferInStructuralButton.addActionListener(e -> deferInStructural());
        panel.add(deferInStructuralButton, gbc);

        // View Documents Button
        gbc.gridy++;
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
            int planId = (int) tablePanel.getPlansTable().getValueAt(selectedRow, 1); // ID is at index 1
            selectedPlan = Database.getPlanById(planId);

            if (selectedPlan != null) {
                planIdLabel.setText(String.valueOf(selectedPlan.getId()));
                applicantNameLabel.setText(selectedPlan.getApplicantName());
                plotNoLabel.setText(selectedPlan.getPlotNo());
                statusLabel.setText(selectedPlan.getStatus());
                remarksArea.setText(selectedPlan.getRemarks() != null ? selectedPlan.getRemarks() : "No remarks.");

                // Enable/disable buttons based on status
                boolean isUnderReviewStructural = selectedPlan.getStatus().equals("Under Review (Structural)");
                boolean isDeferredStructural = selectedPlan.getStatus().equals("Deferred by Structural (Awaiting Clarification)");

                approveToCommitteeButton.setEnabled(isUnderReviewStructural || isDeferredStructural);
                rejectToPlanningButton.setEnabled(isUnderReviewStructural || isDeferredStructural);
                deferInStructuralButton.setEnabled(isUnderReviewStructural); // Can only defer if currently under review
                viewDocumentsButton.setEnabled(true); // Always enable view documents if a plan is selected
            }
        } else {
            clearDetails();
        }
    }

    private void approveAndForwardToCommittee() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Structural)") && !selectedPlan.getStatus().equals("Deferred by Structural (Awaiting Clarification)")) {
            JOptionPane.showMessageDialog(this, "This plan is not in a state to be approved by Structural.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter remarks for approval.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Under Review (Committee)", "Approved by Structural. Forwarded to Technical Committee. Remarks: " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Committee", "Approved by Structural", remarks));

        JOptionPane.showMessageDialog(this, "Plan approved by Structural and forwarded to Technical Committee.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void rejectAndReturnToPlanning() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Structural)") && !selectedPlan.getStatus().equals("Deferred by Structural (Awaiting Clarification)")) {
            JOptionPane.showMessageDialog(this, "This plan is not in a state to be rejected by Structural.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter remarks for rejection.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Rejected by Structural (to Planning)", "Rejected by Structural. Returned to Planning for re-evaluation. Remarks: " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Rejected by Structural", remarks));

        JOptionPane.showMessageDialog(this, "Plan rejected by Structural and returned to Planning Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void deferInStructural() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlan.getStatus().equals("Under Review (Structural)")) {
            JOptionPane.showMessageDialog(this, "This plan is not currently under structural review to be deferred.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remarks = remarksArea.getText().trim();
        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter remarks for deferral (e.g., what clarification is needed).", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlan.getId(), "Deferred by Structural (Awaiting Clarification)", "Deferred by Structural. Awaiting further clarification. Remarks: " + remarks);
        Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), currentUser.getRole(), "Deferred by Structural", remarks));

        JOptionPane.showMessageDialog(this, "Plan deferred by Structural. Awaiting clarification.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDetails();
        refreshData();
    }

    private void viewPlanDocuments(Plan plan) {
        if (plan == null) {
            JOptionPane.showMessageDialog(this, "No plan selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        DocumentViewerDialog viewer = new DocumentViewerDialog(parentDashboard, plan);
        viewer.setVisible(true);
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
        viewDocumentsButton.setEnabled(false);
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Structural
        int pendingStructural = Database.getPlansByStatus("Under Review (Structural)").size();
        int deferredStructural = Database.getPlansByStatus("Deferred by Structural (Awaiting Clarification)").size();
        int approvedByStructural = (int) Database.getAllPlans().stream() // Cast to int here
                                    .filter(p -> p.getRemarks() != null && p.getRemarks().contains("Approved by Structural"))
                                    .count(); 

        cardsPanel.updateCard(0, "Pending Structural Review", pendingStructural, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Deferred by Structural", deferredStructural, new Color(255, 165, 0)); // Orange
        cardsPanel.updateCard(2, "Approved by Structural (Total)", approvedByStructural, new Color(40, 167, 69)); // Green

        // Update table with plans relevant to Structural Engineer
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