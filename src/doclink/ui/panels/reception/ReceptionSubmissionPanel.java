package doclink.ui.panels.reception;

import doclink.Database;
import doclink.models.Document;
import doclink.models.Log;
import doclink.models.Plan;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReceptionSubmissionPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    // Form components for New Plan Submission
    private JTextField applicantNameField;
    private JTextField contactField;
    private JTextField plotNoField;
    private JTextField locationField;
    private JCheckBox sitePlanCb, titleDeedCb, drawingsCb, otherDocsCb;
    private JTextArea remarksArea;
    private JButton submitPlanButton;
    private static final Color DARK_NAVY = new Color(26, 35, 126); // Define DARK_NAVY

    public ReceptionSubmissionPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel; // Keep a reference to update cards if needed
        setLayout(new GridBagLayout());
        setBackground(new Color(245, 247, 250)); // Reverted to light grey
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        createFormComponents();
        refreshData(); // Initial data load for cards
    }

    private void createFormComponents() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("New Building Plan Submission");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(DARK_NAVY);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        // Applicant Details - Arranged side-by-side
        gbc.gridwidth = 1; // Reset gridwidth for side-by-side
        int row = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Applicant Name:"), gbc);
        gbc.gridx = 1;
        applicantNameField = new JTextField(25);
        panel.add(applicantNameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Contact:"), gbc);
        gbc.gridx = 1;
        contactField = new JTextField(25);
        panel.add(contactField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Plot No:"), gbc);
        gbc.gridx = 1;
        plotNoField = new JTextField(25);
        panel.add(plotNoField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        locationField = new JTextField(25);
        panel.add(locationField, gbc);

        // Document Checklist
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2; // Span two columns for the title
        JLabel docTitle = new JLabel("Document Checklist:");
        docTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        docTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        panel.add(docTitle, gbc);

        row++;
        gbc.gridy = row;
        JPanel checklistPanel = new JPanel(new GridLayout(0, 2, 10, 5)); // 2 columns, horizontal and vertical gap
        checklistPanel.setOpaque(false); // Inherit background
        sitePlanCb = new JCheckBox("Site Plan");
        titleDeedCb = new JCheckBox("Title Deed");
        drawingsCb = new JCheckBox("Architectural Drawings");
        otherDocsCb = new JCheckBox("Other Supporting Documents");
        
        checklistPanel.add(sitePlanCb);
        checklistPanel.add(titleDeedCb);
        checklistPanel.add(drawingsCb);
        checklistPanel.add(otherDocsCb);
        panel.add(checklistPanel, gbc); // Add the checklist panel

        // Remarks
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Remarks:"), gbc);
        row++;
        gbc.gridy = row;
        remarksArea = new JTextArea(5, 25);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(remarksArea);
        panel.add(scrollPane, gbc);

        // Submit Button
        row++;
        gbc.gridy = row;
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

        // Add the form panel to this panel, centered
        add(panel, new GridBagConstraints());
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
        // No need to call parentDashboard.showRoleDashboard("Reception") here, as we are already in Reception's context
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
    }
}