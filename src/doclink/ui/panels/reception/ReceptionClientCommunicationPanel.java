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

public class ReceptionClientCommunicationPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    private DashboardTablePanel clientCommunicationTablePanel;

    // Components for Client Communication
    private JLabel clientCommPlanIdLabel, clientCommApplicantNameLabel, clientCommPlotNoLabel, clientCommStatusLabel;
    private JButton notifyClientButton, clientCommViewDocumentsButton;
    private Plan selectedPlanForClientCommunication;
    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public ReceptionClientCommunicationPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Reverted to light grey

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7); // 70% for table, 30% for details
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(new Color(245, 247, 250)); // Reverted to light grey

        clientCommunicationTablePanel = new DashboardTablePanel();
        clientCommunicationTablePanel.getPlansTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && clientCommunicationTablePanel.getPlansTable().getSelectedRow() != -1) {
                    loadSelectedPlanForClientCommunicationDetails();
                }
            }
        });
        splitPane.setLeftComponent(clientCommunicationTablePanel);

        JPanel clientCommDetailsPanel = createClientCommunicationDetailsPanel();
        splitPane.setRightComponent(clientCommDetailsPanel);

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createClientCommunicationDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Client Communication & Actions");
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
        clientCommPlanIdLabel = new JLabel("N/A");
        panel.add(clientCommPlanIdLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Applicant:"), gbc);
        gbc.gridx = 1;
        clientCommApplicantNameLabel = new JLabel("N/A");
        panel.add(clientCommApplicantNameLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Plot No:"), gbc);
        gbc.gridx = 1;
        clientCommPlotNoLabel = new JLabel("N/A");
        panel.add(clientCommPlotNoLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        clientCommStatusLabel = new JLabel("N/A");
        panel.add(clientCommStatusLabel, gbc);

        // Action Button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        notifyClientButton = createStyledButton("Notify Client & Mark for Resubmission");
        notifyClientButton.setBackground(new Color(255, 165, 0)); // Orange for notification
        notifyClientButton.addActionListener(e -> notifyClientAndMarkForResubmission());
        panel.add(notifyClientButton, gbc);

        // View Documents Button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        clientCommViewDocumentsButton = createStyledButton("View Documents", new Color(108, 117, 125)); // Grey
        clientCommViewDocumentsButton.addActionListener(e -> viewPlanDocuments(selectedPlanForClientCommunication));
        clientCommViewDocumentsButton.setEnabled(false); // Initially disabled
        panel.add(clientCommViewDocumentsButton, gbc);

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

    private void loadSelectedPlanForClientCommunicationDetails() {
        int selectedRow = clientCommunicationTablePanel.getPlansTable().getSelectedRow();
        if (selectedRow != -1) {
            int planId = (int) clientCommunicationTablePanel.getPlansTable().getValueAt(selectedRow, 0);
            selectedPlanForClientCommunication = Database.getPlanById(planId);

            if (selectedPlanForClientCommunication != null) {
                clientCommPlanIdLabel.setText(String.valueOf(selectedPlanForClientCommunication.getId()));
                clientCommApplicantNameLabel.setText(selectedPlanForClientCommunication.getApplicantName());
                clientCommPlotNoLabel.setText(selectedPlanForClientCommunication.getPlotNo());
                clientCommStatusLabel.setText(selectedPlanForClientCommunication.getStatus());

                boolean isRejectedForClient = selectedPlanForClientCommunication.getStatus().equals("Rejected (to Reception for Client)");
                notifyClientButton.setEnabled(isRejectedForClient);
                clientCommViewDocumentsButton.setEnabled(true); // Always enable view documents if a plan is selected
            }
        }
    }

    private void notifyClientAndMarkForResubmission() {
        if (selectedPlanForClientCommunication == null) {
            JOptionPane.showMessageDialog(this, "Please select a plan first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedPlanForClientCommunication.getStatus().equals("Rejected (to Reception for Client)")) {
            JOptionPane.showMessageDialog(this, "This plan is not awaiting client notification.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database.updatePlanStatus(selectedPlanForClientCommunication.getId(), "Client Notified (Awaiting Resubmission)", "Client notified about rejection and requested to resubmit with revisions.");
        Database.addLog(new Log(selectedPlanForClientCommunication.getId(), currentUser.getRole(), "Client", "Notified of Rejection", "Client needs to resubmit."));

        JOptionPane.showMessageDialog(this, "Client notified about the rejected plan. Status updated to 'Client Notified (Awaiting Resubmission)'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearClientCommunicationDetails();
        refreshData();
    }

    private void clearClientCommunicationDetails() {
        selectedPlanForClientCommunication = null;
        clientCommPlanIdLabel.setText("N/A");
        clientCommApplicantNameLabel.setText("N/A");
        clientCommPlotNoLabel.setText("N/A");
        clientCommStatusLabel.setText("N/A");
        notifyClientButton.setEnabled(false);
        clientCommViewDocumentsButton.setEnabled(false);
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

        // Update table for Client Communication tab
        List<Plan> clientCommunicationPlans = Database.getPlansByStatus("Rejected (to Reception for Client)");
        clientCommunicationTablePanel.updateTable(clientCommunicationPlans);

        // Clear client communication details if selected plan is no longer relevant
        if (selectedPlanForClientCommunication != null && !clientCommunicationPlans.stream().anyMatch(p -> p.getId() == selectedPlanForClientCommunication.getId())) {
            clearClientCommunicationDetails();
        } else if (selectedPlanForClientCommunication != null) {
            loadSelectedPlanForClientCommunicationDetails();
        }
    }
}