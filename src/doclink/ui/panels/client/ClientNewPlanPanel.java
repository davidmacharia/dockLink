package doclink.ui.panels.client;

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

public class ClientNewPlanPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    // Form components
    private JLabel applicantNameLabel; // Will display currentUser.getName()
    private JLabel contactLabel; // Will display currentUser.getEmail()
    private JTextField plotNoField;
    private JTextField locationField;
    private JCheckBox sitePlanCb, titleDeedCb, drawingsCb, otherDocsCb;
    private JTextArea remarksArea;
    private JButton submitPlanButton;
    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public ClientNewPlanPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new GridBagLayout());
        setBackground(new Color(245, 247, 250));
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

        JLabel title = new JLabel("Submit New Building Plan");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(DARK_NAVY);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        // Applicant Details (pre-filled from current user)
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Applicant Name:"), gbc);
        gbc.gridx = 1;
        applicantNameLabel = new JLabel(currentUser.getName()); // Display user's name
        applicantNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(applicantNameLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Contact Email:"), gbc);
        gbc.gridx = 1;
        contactLabel = new JLabel(currentUser.getEmail()); // Display user's email
        contactLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(contactLabel, gbc);

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
        submitPlanButton.setOpaque(true); // Corrected: changed 'button' to 'submitPlanButton'
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
        String applicantName = currentUser.getName(); // From current user
        String contact = currentUser.getEmail(); // From current user
        String plotNo = plotNoField.getText();
        String location = locationField.getText();
        String remarks = remarksArea.getText();

        if (plotNo.isEmpty() || location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in Plot No and Location.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Document> documents = new ArrayList<>();
        boolean allAttached = true;
        if (sitePlanCb.isSelected()) documents.add(new Document("Site Plan", null, true)); else allAttached = false;
        if (titleDeedCb.isSelected()) documents.add(new Document("Title Deed", null, true)); else allAttached = false;
        if (drawingsCb.isSelected()) documents.add(new Document("Architectural Drawings", null, true)); else allAttached = false;
        if (otherDocsCb.isSelected()) documents.add(new Document("Other Documents", null, true)); else allAttached = false;

        if (!allAttached) {
            int confirm = JOptionPane.showConfirmDialog(this, "Some documents are not attached. Do you want to proceed? (Note: In a real system, this might require all documents or a specific process for incomplete submissions.)", "Incomplete Submission", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.NO_OPTION) {
                return; // Stay on form
            }
        }

        // If complete, forward to Planning Department
        Plan newPlan = new Plan(applicantName, contact, plotNo, location, LocalDate.now(), "Submitted", remarks); // Initial status "Submitted"
        Database.addPlan(newPlan, documents);

        // Log the action
        Database.addLog(new Log(newPlan.getId(), currentUser.getRole(), "Reception", "New Plan Submitted by Client", "Initial submission, documents attached."));

        JOptionPane.showMessageDialog(this, "Plan submitted successfully! It will now be reviewed by the Reception.", "Submission Success", JOptionPane.INFORMATION_MESSAGE);
        clearSubmissionForm();
        refreshData();
        // After submission, navigate back to the client's main dashboard view
        parentDashboard.showRoleDashboard("Client"); // This will show the default client panel (My Plans)
    }

    private void clearSubmissionForm() {
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
        String clientEmail = currentUser.getEmail();

        // Update cards with relevant counts for Client
        int myPendingPlans = Database.getPlansByApplicantEmailAndStatus(clientEmail, "Submitted").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Awaiting Payment").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Under Review (Planning)").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Under Review (Committee)").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Under Review (Director)").size() +
                             Database.getPlansByApplicantEmailAndStatus(clientEmail, "Under Review (Structural)").size();
        int myApprovedPlans = Database.getPlansByApplicantEmailAndStatus(clientEmail, "Approved").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Approved (Awaiting Client Pickup)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Completed").size();
        int myRejectedPlans = Database.getPlansByApplicantEmailAndStatus(clientEmail, "Rejected").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Deferred").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Rejected (to Planning)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Deferred (to Planning)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Rejected by Structural (to Planning)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Deferred by Structural (Awaiting Clarification)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Rejected (to Reception for Client)").size() +
                              Database.getPlansByApplicantEmailAndStatus(clientEmail, "Client Notified (Awaiting Resubmission)").size();


        cardsPanel.updateCard(0, "My Pending Plans", myPendingPlans, new Color(255, 193, 7));
        cardsPanel.updateCard(1, "My Approved Plans", myApprovedPlans, new Color(40, 167, 69));
        cardsPanel.updateCard(2, "My Rejected/Deferred Plans", myRejectedPlans, new Color(220, 53, 69));
    }
}