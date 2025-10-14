package doclink.ui.views;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.io.File; // For opening files

public class ReceptionView extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;

    private DashboardCardsPanel cardsPanel;
    private DashboardTablePanel allPlansTablePanel; // Renamed for clarity
    private DashboardTablePanel paymentTablePanel; // New table for payment processing
    private DashboardTablePanel directorDecisionsTablePanel; // New table for Director decisions
    private DashboardTablePanel clientCommunicationTablePanel; // New table for client communication

    // Form components for New Plan Submission
    private JTextField applicantNameField;
    private JTextField contactField;
    private JTextField plotNoField;
    private JTextField locationField;
    private JCheckBox sitePlanCb, titleDeedCb, drawingsCb, otherDocsCb;
    private JTextArea remarksArea;
    private JButton submitPlanButton; // Added submitPlanButton

    // Components for Payment Processing
    private JLabel paymentPlanIdLabel, paymentApplicantNameLabel, paymentPlotNoLabel, paymentStatusLabel, paymentAmountLabel;
    private JTextField receiptNoField;
    private JButton attachReceiptButton, paymentViewDocumentsButton; // Added paymentViewDocumentsButton
    private Plan selectedPlanForPayment; // To hold the plan selected in the payment table

    // Components for Director Decisions
    private JLabel directorPlanIdLabel, directorApplicantNameLabel, directorPlotNoLabel, directorStatusLabel;
    private JButton forwardToStructuralButton, releaseToClientButton, directorDecisionViewDocumentsButton; // Added directorDecisionViewDocumentsButton
    private Plan selectedPlanForDirectorDecision;

    // Components for Client Communication
    private JLabel clientCommPlanIdLabel, clientCommApplicantNameLabel, clientCommPlotNoLabel, clientCommStatusLabel;
    private JButton notifyClientButton, clientCommViewDocumentsButton; // Added clientCommViewDocumentsButton
    private Plan selectedPlanForClientCommunication;

    private JPanel receptionSidebar; // New sidebar for ReceptionView
    private JPanel receptionContentPanel; // Panel to hold different views with CardLayout
    private CardLayout receptionCardLayout; // CardLayout for switching views

    public ReceptionView(User user, Dashboard parentDashboard) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Very light grey

        // Top section: Cards
        cardsPanel = new DashboardCardsPanel();
        add(cardsPanel, BorderLayout.NORTH);

        // Create Reception-specific sidebar
        receptionSidebar = createReceptionSidebar();
        add(receptionSidebar, BorderLayout.WEST);

        // Create content panel with CardLayout
        receptionCardLayout = new CardLayout();
        receptionContentPanel = new JPanel(receptionCardLayout);
        receptionContentPanel.setBackground(new Color(245, 247, 250));

        // Initialize existing panels and add them to receptionContentPanel
        JPanel submissionPanel = createSubmissionFormPanel();
        allPlansTablePanel = new DashboardTablePanel();
        JPanel paymentPanel = createPaymentHandlingPanel();
        JPanel directorDecisionsPanel = createDirectorDecisionsPanel();
        JPanel clientCommunicationPanel = createClientCommunicationPanel();

        receptionContentPanel.add(submissionPanel, "New Plan Submission");
        receptionContentPanel.add(allPlansTablePanel, "All Plans");
        receptionContentPanel.add(paymentPanel, "Payment Processing");
        receptionContentPanel.add(directorDecisionsPanel, "Director Decisions");
        receptionContentPanel.add(clientCommunicationPanel, "Client Communication");

        add(receptionContentPanel, BorderLayout.CENTER);

        // Show initial view (e.g., New Plan Submission)
        receptionCardLayout.show(receptionContentPanel, "New Plan Submission");

        refreshData(); // Load initial data
    }

    private JPanel createReceptionSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(200, 0)); // Adjust width as needed
        sidebar.setBackground(new Color(230, 235, 240)); // Light grey for reception sidebar
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220))); // Right border

        JLabel title = new JLabel("Reception Menu");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(26, 35, 126));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 20, 0));
        sidebar.add(title);

        addReceptionSidebarButton(sidebar, "New Plan Submission");
        addReceptionSidebarButton(sidebar, "All Plans");
        addReceptionSidebarButton(sidebar, "Payment Processing");
        addReceptionSidebarButton(sidebar, "Director Decisions");
        addReceptionSidebarButton(sidebar, "Client Communication");

        sidebar.add(Box.createVerticalGlue()); // Push buttons to top

        return sidebar;
    }

    private void addReceptionSidebarButton(JPanel sidebar, String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(new Color(26, 35, 126));
        button.setBackground(new Color(0,0,0,0)); // Transparent
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(200, 210, 220)); // Light hover effect
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0,0,0,0));
            }
        });

        button.addActionListener(e -> receptionCardLayout.show(receptionContentPanel, text));
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(5)); // Small spacing
    }

    private JPanel createSubmissionFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("New Building Plan Submission");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(26, 35, 126));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        // Applicant Details
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Applicant Name:"), gbc);
        gbc.gridx = 1;
        applicantNameField = new JTextField(25);
        panel.add(applicantNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Contact:"), gbc);
        gbc.gridx = 1;
        contactField = new JTextField(25);
        panel.add(contactField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Plot No:"), gbc);
        gbc.gridx = 1;
        plotNoField = new JTextField(25);
        panel.add(plotNoField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        locationField = new JTextField(25);
        panel.add(locationField, gbc);

        // Document Checklist
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JLabel docTitle = new JLabel("Document Checklist:");
        docTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        docTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        panel.add(docTitle, gbc);

        gbc.gridy++;
        sitePlanCb = new JCheckBox("Site Plan");
        panel.add(sitePlanCb, gbc);
        gbc.gridy++;
        titleDeedCb = new JCheckBox("Title Deed");
        panel.add(titleDeedCb, gbc);
        gbc.gridy++;
        drawingsCb = new JCheckBox("Architectural Drawings");
        panel.add(drawingsCb, gbc);
        gbc.gridy++;
        otherDocsCb = new JCheckBox("Other Supporting Documents");
        panel.add(otherDocsCb, gbc);

        // Remarks
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Remarks:"), gbc);
        gbc.gridy++;
        remarksArea = new JTextArea(5, 25);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(remarksArea);
        panel.add(scrollPane, gbc);

        // Submit Button
        gbc.gridy++;
        submitPlanButton = new JButton("Submit Plan");
        submitPlanButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        submitPlanButton.setBackground(new Color(0, 123, 255));
        submitPlanButton.setForeground(Color.WHITE);
        submitPlanButton.setFocusPainted(false);
        submitPlanButton.setBorderPainted(false);
        submitPlanButton.setOpaque(true);
        submitPlanButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        submitPlanButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitPlanButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { submitPlanButton.setBackground(new Color(0, 100, 200)); }
            public void mouseExited(java.awt.event.MouseEvent evt) { submitPlanButton.setBackground(new Color(0, 123, 255)); }
        });
        submitPlanButton.addActionListener(e -> submitNewPlan());
        panel.add(submitPlanButton, gbc);

        return panel;
    }

    private JPanel createPaymentHandlingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7); // 70% for table, 30% for details
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(new Color(245, 247, 250));

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

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
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
        title.setForeground(new Color(26, 35, 126));
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
        paymentViewDocumentsButton = createStyledButton("View Documents");
        paymentViewDocumentsButton.setBackground(new Color(108, 117, 125)); // Grey
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

    private JPanel createDirectorDecisionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7); // 70% for table, 30% for details
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(new Color(245, 247, 250));

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

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
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
        title.setForeground(new Color(26, 35, 126));
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
        directorDecisionViewDocumentsButton = createStyledButton("View Documents");
        directorDecisionViewDocumentsButton.setBackground(new Color(108, 117, 125)); // Grey
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

    private JPanel createClientCommunicationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.7); // 70% for table, 30% for details
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(new Color(245, 247, 250));

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

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
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
        title.setForeground(new Color(26, 35, 126));
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
        clientCommViewDocumentsButton = createStyledButton("View Documents");
        clientCommViewDocumentsButton.setBackground(new Color(108, 117, 125)); // Grey
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

    private void submitNewPlan() {
        String applicantName = applicantNameField.getText();
        String contact = contactField.getText();
        String plotNo = plotNoField.getText();
        String location = locationField.getText();
        String remarks = remarksArea.getText();

        if (applicantName.isEmpty() || contact.isEmpty() || plotNo.isEmpty() || location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all applicant details.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Document> documents = new ArrayList<>();
        boolean allAttached = true;
        if (sitePlanCb.isSelected()) documents.add(new Document("Site Plan", null, true)); else allAttached = false;
        if (titleDeedCb.isSelected()) documents.add(new Document("Title Deed", null, true)); else allAttached = false;
        if (drawingsCb.isSelected()) documents.add(new Document("Architectural Drawings", null, true)); else allAttached = false;
        if (otherDocsCb.isSelected()) documents.add(new Document("Other Documents", null, true)); else allAttached = false;

        if (!allAttached) {
            int confirm = JOptionPane.showConfirmDialog(this, "Some documents are not attached. Do you want to proceed and return to applicant?", "Incomplete Submission", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Plan marked as incomplete and returned to applicant.", "Submission Info", JOptionPane.INFORMATION_MESSAGE);
                clearSubmissionForm();
                refreshData();
                return;
            } else {
                return; // Stay on form
            }
        }

        // If complete, forward to Planning Department
        Plan newPlan = new Plan(applicantName, contact, plotNo, location, LocalDate.now(), "Under Review (Planning)", remarks);
        Database.addPlan(newPlan, documents);

        // Log the action
        Database.addLog(new Log(newPlan.getId(), currentUser.getRole(), "Planning", "Forwarded for Review", "Initial submission, documents verified."));

        JOptionPane.showMessageDialog(this, "Plan submitted successfully and forwarded to Planning Department.", "Submission Success", JOptionPane.INFORMATION_MESSAGE);
        clearSubmissionForm();
        refreshData();
        parentDashboard.showRoleDashboard("Reception"); // Refresh current view
    }

    private void clearSubmissionForm() {
        applicantNameField.setText("");
        contactField.setText("");
        plotNoField.setText("");
        locationField.setText("");
        remarksArea.setText("");
        sitePlanCb.setSelected(false);
        titleDeedCb.setSelected(false);
        drawingsCb.setSelected(false);
        otherDocsCb.setSelected(false);
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

        Database.updateBillingPayment(billing.getId(), receiptNo);
        Database.updatePlanStatus(selectedPlanForPayment.getId(), "Payment Received", "Payment received with receipt: " + receiptNo);
        Database.addLog(new Log(selectedPlanForPayment.getId(), currentUser.getRole(), "Planning", "Payment Received", "Receipt No: " + receiptNo));

        JOptionPane.showMessageDialog(this, "Payment recorded and plan forwarded to Planning Department.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearPaymentDetails();
        refreshData();
        parentDashboard.showRoleDashboard("Reception"); // Refresh current view
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
        parentDashboard.showRoleDashboard("Reception");
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
        parentDashboard.showRoleDashboard("Reception");
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
        parentDashboard.showRoleDashboard("Reception");
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
        // Update cards (placeholder counts)
        int pending = Database.getPlansByStatus("Under Review (Planning)").size() + Database.getPlansByStatus("Awaiting Payment").size();
        int approved = Database.getPlansByStatus("Approved").size() + Database.getPlansByStatus("Approved (Awaiting Client Pickup)").size();
        int deferred = Database.getPlansByStatus("Deferred").size() + Database.getPlansByStatus("Rejected").size() + Database.getPlansByStatus("Rejected (to Planning)").size() + Database.getPlansByStatus("Deferred (to Planning)").size();
        int directorApprovedToReception = Database.getPlansByStatus("Approved by Director (to Reception for Structural)").size() + Database.getPlansByStatus("Approved (Awaiting Client Pickup)").size();
        int rejectedForClientComm = Database.getPlansByStatus("Rejected (to Reception for Client)").size();


        cardsPanel.updateCard(0, "Pending Plans", pending, new Color(255, 193, 7));
        cardsPanel.updateCard(1, "Approved Plans", approved, new Color(40, 167, 69));
        cardsPanel.updateCard(2, "Deferred/Returned", deferred + rejectedForClientComm, new Color(220, 53, 69));

        // Update table with ALL plans for Reception
        List<Plan> allPlans = Database.getAllPlans();
        allPlansTablePanel.updateTable(allPlans);

        // Update table for Payment Processing tab
        List<Plan> awaitingPaymentPlans = Database.getPlansByStatus("Awaiting Payment");
        paymentTablePanel.updateTable(awaitingPaymentPlans);

        // Update table for Director Decisions tab
        List<Plan> directorDecisionPlans = new ArrayList<>();
        directorDecisionPlans.addAll(Database.getPlansByStatus("Approved by Director (to Reception for Structural)"));
        directorDecisionPlans.addAll(Database.getPlansByStatus("Approved (Awaiting Client Pickup)"));
        directorDecisionsTablePanel.updateTable(directorDecisionPlans);

        // Update table for Client Communication tab
        List<Plan> clientCommunicationPlans = Database.getPlansByStatus("Rejected (to Reception for Client)");
        clientCommunicationTablePanel.updateTable(clientCommunicationPlans);


        // Clear payment details if selected plan is no longer in "Awaiting Payment" list
        if (selectedPlanForPayment != null && !awaitingPaymentPlans.stream().anyMatch(p -> p.getId() == selectedPlanForPayment.getId())) {
            clearPaymentDetails();
        } else if (selectedPlanForPayment != null) {
            // Re-load details for the currently selected plan to reflect any status changes
            loadSelectedPlanForPaymentDetails();
        }

        // Clear director decision details if selected plan is no longer relevant
        if (selectedPlanForDirectorDecision != null && !directorDecisionPlans.stream().anyMatch(p -> p.getId() == selectedPlanForDirectorDecision.getId())) {
            clearDirectorDecisionDetails();
        } else if (selectedPlanForDirectorDecision != null) {
            loadSelectedPlanForDirectorDecisionDetails();
        }

        // Clear client communication details if selected plan is no longer relevant
        if (selectedPlanForClientCommunication != null && !clientCommunicationPlans.stream().anyMatch(p -> p.getId() == selectedPlanForClientCommunication.getId())) {
            clearClientCommunicationDetails();
        } else if (selectedPlanForClientCommunication != null) {
            loadSelectedPlanForClientCommunicationDetails();
        }
    }
}