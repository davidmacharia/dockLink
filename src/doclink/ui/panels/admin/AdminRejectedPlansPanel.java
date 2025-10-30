package doclink.ui.panels.admin;

import doclink.Database;
import doclink.models.Document;
import doclink.models.Log;
import doclink.models.Plan;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.DashboardTablePanel;
import doclink.ui.components.DocumentViewerDialog; // NEW: Import DocumentViewerDialog

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AdminRejectedPlansPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private DashboardTablePanel tablePanel;

    // Plan details panel components
    private JLabel planIdLabel, applicantNameLabel, plotNoLabel, statusLabel, referenceNoLabel;
    private JTextArea remarksArea;
    private JButton reRouteToPlanningButton, deletePlanButton, viewDocumentsButton;

    private Plan selectedPlan;
    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public AdminRejectedPlansPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel panelTitle = new JLabel("Rejected / Deferred Plans Management");
        panelTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panelTitle.setForeground(DARK_NAVY);
        panelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(panelTitle, BorderLayout.NORTH);

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
        panel.add(new JLabel("Reference No:"), gbc);
        gbc.gridx = 1;
        referenceNoLabel = new JLabel("N/A");
        panel.add(referenceNoLabel, gbc);

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
        remarksArea.setEditable(false);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(remarksArea);
        panel.add(scrollPane, gbc);

        // Action Buttons
        gbc.gridy++;
        reRouteToPlanningButton = createStyledButton("Re-route to Planning", new Color(0, 123, 255)); // Blue
        reRouteToPlanningButton.addActionListener(e -> reRouteToPlanning());
        reRouteToPlanningButton.setEnabled(false);
        panel.add(reRouteToPlanningButton, gbc);

        gbc.gridy++;
        viewDocumentsButton = createStyledButton("View Documents", new Color(108, 117, 125)); // Grey
        viewDocumentsButton.addActionListener(e -> viewPlanDocuments(selectedPlan));
        viewDocumentsButton.setEnabled(false);
        panel.add(viewDocumentsButton, gbc);

        gbc.gridy++;
        deletePlanButton = createStyledButton("Delete Plan", new Color(220, 53, 69)); // Red
        deletePlanButton.addActionListener(e -> deleteSelectedPlan());
        deletePlanButton.setEnabled(false);
        panel.add(deletePlanButton, gbc);

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
                referenceNoLabel.setText(selectedPlan.getReferenceNo() != null ? selectedPlan.getReferenceNo() : "N/A");
                applicantNameLabel.setText(selectedPlan.getApplicantName());
                plotNoLabel.setText(selectedPlan.getPlotNo());
                statusLabel.setText(selectedPlan.getStatus());
                remarksArea.setText(selectedPlan.getRemarks() != null ? selectedPlan.getRemarks() : "No remarks.");

                // Enable/disable buttons
                reRouteToPlanningButton.setEnabled(true);
                deletePlanButton.setEnabled(true);
                viewDocumentsButton.setEnabled(true);
            }
        }
    }

    private void clearDetails() {
        selectedPlan = null;
        planIdLabel.setText("N/A");
        referenceNoLabel.setText("N/A");
        applicantNameLabel.setText("N/A");
        plotNoLabel.setText("N/A");
        statusLabel.setText("N/A");
        remarksArea.setText("");
        reRouteToPlanningButton.setEnabled(false);
        deletePlanButton.setEnabled(false);
        viewDocumentsButton.setEnabled(false);
    }

    private void reRouteToPlanning() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to re-route this plan to Planning? Its status will be set to 'Under Review (Planning)'.", "Confirm Re-route", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String newRemarks = "Re-routed by Admin from '" + selectedPlan.getStatus() + "' to Planning. Original remarks: " + (selectedPlan.getRemarks() != null ? selectedPlan.getRemarks() : "N/A");
            Database.updatePlanStatus(selectedPlan.getId(), "Under Review (Planning)", newRemarks);
            Database.addLog(new Log(selectedPlan.getId(), currentUser.getRole(), "Planning", "Re-routed by Admin", "Plan re-routed to Planning for review."));
            JOptionPane.showMessageDialog(this, "Plan re-routed to Planning Department successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearDetails();
            refreshData();
        }
    }

    private void deleteSelectedPlan() {
        if (selectedPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently delete plan ID " + selectedPlan.getId() + " and all its associated data (documents, billing, logs)? This action cannot be undone.", "Confirm Permanent Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (Database.deletePlanAndRelatedData(selectedPlan.getId())) {
                JOptionPane.showMessageDialog(this, "Plan ID " + selectedPlan.getId() + " and all related data deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearDetails();
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete plan and related data.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewPlanDocuments(Plan plan) {
        if (plan == null) {
            JOptionPane.showMessageDialog(this, "No plan selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        DocumentViewerDialog viewer = new DocumentViewerDialog(parentDashboard, plan);
        viewer.setVisible(true);
    }

    @Override
    public void refreshData() {
        List<Plan> rejectedDeferredPlans = new ArrayList<>();
        rejectedDeferredPlans.addAll(Database.getPlansByStatus("Rejected"));
        rejectedDeferredPlans.addAll(Database.getPlansByStatus("Deferred"));
        rejectedDeferredPlans.addAll(Database.getPlansByStatus("Rejected (to Planning)"));
        rejectedDeferredPlans.addAll(Database.getPlansByStatus("Deferred (to Planning)"));
        rejectedDeferredPlans.addAll(Database.getPlansByStatus("Rejected by Structural (to Planning)"));
        rejectedDeferredPlans.addAll(Database.getPlansByStatus("Deferred by Structural (Awaiting Clarification)"));
        rejectedDeferredPlans.addAll(Database.getPlansByStatus("Rejected (to Reception for Client)"));
        rejectedDeferredPlans.addAll(Database.getPlansByStatus("Client Notified (Awaiting Resubmission)"));

        tablePanel.updateTable(rejectedDeferredPlans);
        clearDetails(); // Clear details panel when table refreshes
        // cardsPanel.refreshData(); // Removed this line
    }
}