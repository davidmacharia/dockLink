package doclink.ui.panels.reception;

import doclink.Database;
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

public class ReceptionDirectorDecisionsPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    private DashboardTablePanel directorDecisionsTablePanel;

    // Components for Director Decisions
    private JLabel directorPlanIdLabel, directorApplicantNameLabel, directorPlotNoLabel, directorStatusLabel;
    private JButton forwardToStructuralButton, releaseToClientButton, directorDecisionViewDocumentsButton;
    private Plan selectedPlanForDirectorDecision;
    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public ReceptionDirectorDecisionsPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Reverted to light grey

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7); // 70% for table, 30% for details
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(new Color(245, 247, 250)); // Reverted to light grey

        directorDecisionsTablePanel = new DashboardTablePanel();
        directorDecisionsTablePanel.getPlansTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && directorDecisionsTablePanel.getPlansTable().getSelectedRow() != -1) {
                    loadSelectedPlanForDirectorDecisionDetails();
                }
            }
        });
        splitPane.setLeftComponent(directorDecisionsTablePanel);

        JPanel decisionDetailsPanel = createDirectorDecisionDetailsPanel();
        splitPane.setRightComponent(decisionDetailsPanel);

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createDirectorDecisionDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Director's Decision Details & Actions");
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
        directorPlanIdLabel = new JLabel("N/A");
        panel.add(directorPlanIdLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Applicant:"), gbc);
        gbc.gridx = 1;
        directorApplicantNameLabel = new JLabel("N/A");
        panel.add(directorApplicantNameLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Plot No:"), gbc);
        gbc.gridx = 1;
        directorPlotNoLabel = new JLabel("N/A");
        panel.add(directorPlotNoLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        directorStatusLabel = new JLabel("N/A");
        panel.add(directorStatusLabel, gbc);

        // Action Buttons
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        forwardToStructuralButton = createStyledButton("Forward to Structural Department");
        forwardToStructuralButton.addActionListener(e -> forwardToStructural());
        panel.add(forwardToStructuralButton, gbc);

        gbc.gridy++;
        releaseToClientButton = createStyledButton("Release Plan to Client");
        releaseToClientButton.addActionListener(e -> releaseToClient());
        panel.add(releaseToClientButton, gbc);

        // View Documents Button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        directorDecisionViewDocumentsButton = createStyledButton("View Documents", new Color(108, 117, 125)); // Grey
        directorDecisionViewDocumentsButton.addActionListener(e -> viewPlanDocuments(selectedPlanForDirectorDecision));
        directorDecisionViewDocumentsButton.setEnabled(false); // Initially disabled
        panel.add(directorDecisionViewDocumentsButton, gbc);

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

    private void loadSelectedPlanForDirectorDecisionDetails() {
        int selectedRow = directorDecisionsTablePanel.getPlansTable().getSelectedRow();
        if (selectedRow != -1) {
            int planId = (int) directorDecisionsTablePanel.getPlansTable().getValueAt(selectedRow, 0);
            selectedPlanForDirectorDecision = Database.getPlanById(planId);

            if (selectedPlanForDirectorDecision != null) {
                directorPlanIdLabel.setText(String.valueOf(selectedPlanForDirectorDecision.getId()));
                directorApplicantNameLabel.setText(selectedPlanForDirectorDecision.getApplicantName());
                directorPlotNoLabel.setText(selectedPlanForDirectorDecision.getPlotNo());
                directorStatusLabel.setText(selectedPlanForDirectorDecision.getStatus());

                boolean isApprovedForStructural = selectedPlanForDirectorDecision.getStatus().equals("Approved by Director (to Reception for Structural)");
                boolean isApprovedAwaitingClient = selectedPlanForDirectorDecision.getStatus().equals("Approved (Awaiting Client Pickup)");

                forwardToStructuralButton.setEnabled(isApprovedForStructural);
                releaseToClientButton.setEnabled(isApprovedAwaitingClient);
                directorDecisionViewDocumentsButton.setEnabled(true); // Always enable view documents if a plan is selected
            }
        }
    }

    private void forwardToStructural() {
        if (selectedPlanForDirectorDecision == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlanForDirectorDecision.getStatus().equals("Approved by Director (to Reception for Structural)")) {
            JOptionPane.showMessageDialog(this, "This plan is not approved for structural review.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- NEW VALIDATION: Check for Director's Approval Letter ---
        List<Document> documents = Database.getDocumentsByPlanId(selectedPlanForDirectorDecision.getId());
        boolean directorLetterFound = documents.stream().anyMatch(doc ->
            "Director's Approval Letter (Structural)".equals(doc.getDocName()) && "Generated".equals(doc.getDocumentType())
        );

        if (!directorLetterFound) {
            JOptionPane.showMessageDialog(this, "Director's Approval Letter (Structural) not found. Cannot forward to Structural Department.", "Missing Document", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // --- END NEW VALIDATION ---

        Database.updatePlanStatus(selectedPlanForDirectorDecision.getId(), "Under Review (Structural)", "Forwarded to Structural Department by Reception after Director's approval.");
        Database.addLog(new Log(selectedPlanForDirectorDecision.getId(), currentUser.getRole(), "Structural", "Forwarded to Structural", "Director's approval letter attached."));

        JOptionPane.showMessageDialog(this, "Plan forwarded to Structural Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDirectorDecisionDetails();
        refreshData();
    }

    private void releaseToClient() {
        if (selectedPlanForDirectorDecision == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlanForDirectorDecision.getStatus().equals("Approved (Awaiting Client Pickup)")) {
            JOptionPane.showMessageDialog(this, "This plan is not awaiting client pickup.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlanForDirectorDecision.getId(), "Completed", "Plan released to client.");
        Database.addLog(new Log(selectedPlanForDirectorDecision.getId(), currentUser.getRole(), "Client", "Plan Released", "Approval certificate provided to client."));

        JOptionPane.showMessageDialog(this, "Plan released to client. Status updated to 'Completed'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearDirectorDecisionDetails();
        refreshData();
    }

    private void clearDirectorDecisionDetails() {
        selectedPlanForDirectorDecision = null;
        directorPlanIdLabel.setText("N/A");
        directorApplicantNameLabel.setText("N/A");
        directorPlotNoLabel.setText("N/A");
        directorStatusLabel.setText("N/A");
        forwardToStructuralButton.setEnabled(false);
        releaseToClientButton.setEnabled(false);
        directorDecisionViewDocumentsButton.setEnabled(false);
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

        // Update table for Director Decisions tab
        List<Plan> directorDecisionPlans = new ArrayList<>();
        directorDecisionPlans.addAll(Database.getPlansByStatus("Approved by Director (to Reception for Structural)"));
        directorDecisionPlans.addAll(Database.getPlansByStatus("Approved (Awaiting Client Pickup)"));
        directorDecisionsTablePanel.updateTable(directorDecisionPlans);

        // Clear director decision details if selected plan is no longer relevant
        if (selectedPlanForDirectorDecision != null && !directorDecisionPlans.stream().anyMatch(p -> p.getId() == selectedPlanForDirectorDecision.getId())) {
            clearDirectorDecisionDetails();
        } else if (selectedPlanForDirectorDecision != null) {
            loadSelectedPlanForDirectorDecisionDetails();
        }
    }
}