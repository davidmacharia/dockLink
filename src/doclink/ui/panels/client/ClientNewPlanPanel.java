package doclink.ui.panels.client;

import doclink.Database;
import doclink.models.Document;
import doclink.models.DocumentChecklistItem;
import doclink.models.Log;
import doclink.models.Plan;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.components.ChecklistItemUploadPanel; // NEW: Import the new component

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClientNewPlanPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    // Form components
    private JLabel applicantNameLabel;
    private JTextField contactField;
    private JTextField plotNoField;
    private JTextField locationField;
    private JPanel dynamicChecklistPanel;
    private List<ChecklistItemUploadPanel> checklistItemUploadPanels; // Changed to list of custom panels
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
        refreshData();
    }

    private void createFormComponents() {
        JPanel mainFormPanel = new JPanel(new GridBagLayout()); // Main panel for the form content
        mainFormPanel.setBackground(Color.WHITE);
        mainFormPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Title
        JLabel title = new JLabel("Submit New Building Plan");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(DARK_NAVY);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Span across two conceptual columns
        mainFormPanel.add(title, gbc);

        // Container for side-by-side layout (Applicant Details and Checklist)
        JPanel sideBySideContainer = new JPanel(new GridLayout(1, 2, 20, 0)); // 1 row, 2 columns, 20px horizontal gap
        sideBySideContainer.setOpaque(false); // Inherit background

        // Left Panel: Applicant Details
        JPanel applicantDetailsPanel = new JPanel(new GridBagLayout());
        applicantDetailsPanel.setOpaque(false); // Inherit background
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.insets = new Insets(5, 5, 5, 5);
        gbcLeft.fill = GridBagConstraints.HORIZONTAL;
        gbcLeft.anchor = GridBagConstraints.WEST;
        gbcLeft.gridx = 0;
        gbcLeft.gridwidth = 1;

        int row = 0;
        gbcLeft.gridy = row++;
        applicantDetailsPanel.add(new JLabel("Applicant Name:"), gbcLeft);
        gbcLeft.gridx = 1;
        applicantNameLabel = new JLabel(currentUser.getName());
        applicantNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        applicantDetailsPanel.add(applicantNameLabel, gbcLeft);

        gbcLeft.gridx = 0;
        gbcLeft.gridy = row++;
        applicantDetailsPanel.add(new JLabel("Contact:"), gbcLeft);
        gbcLeft.gridx = 1;
        contactField = new JTextField(20); // Adjusted width
        contactField.setText(currentUser.getEmail());
        applicantDetailsPanel.add(contactField, gbcLeft);

        gbcLeft.gridx = 0;
        gbcLeft.gridy = row++;
        applicantDetailsPanel.add(new JLabel("Plot No:"), gbcLeft);
        gbcLeft.gridx = 1;
        plotNoField = new JTextField(20); // Adjusted width
        applicantDetailsPanel.add(plotNoField, gbcLeft);

        gbcLeft.gridx = 0;
        gbcLeft.gridy = row++;
        applicantDetailsPanel.add(new JLabel("Location:"), gbcLeft);
        gbcLeft.gridx = 1;
        locationField = new JTextField(20); // Adjusted width
        applicantDetailsPanel.add(locationField, gbcLeft);

        // Add vertical glue to push content to top in its panel
        gbcLeft.gridx = 0;
        gbcLeft.gridy = row++;
        gbcLeft.gridwidth = 2;
        gbcLeft.weighty = 1.0;
        applicantDetailsPanel.add(Box.createVerticalGlue(), gbcLeft);

        // Right Panel: Document Checklist
        JPanel checklistPanel = new JPanel(new GridBagLayout());
        checklistPanel.setOpaque(false); // Inherit background
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.insets = new Insets(5, 5, 5, 5);
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        gbcRight.anchor = GridBagConstraints.NORTHWEST; // Align checklist items to top-left
        gbcRight.gridx = 0;
        gbcRight.gridwidth = 2; // Checklist title spans 2 columns within its panel

        row = 0;
        gbcRight.gridy = row++;
        JLabel docTitle = new JLabel("Document Checklist:");
        docTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        docTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        checklistPanel.add(docTitle, gbcRight);

        gbcRight.gridy = row++;
        gbcRight.weightx = 1.0;
        gbcRight.weighty = 1.0; // Allow dynamic checklist panel to expand
        gbcRight.fill = GridBagConstraints.BOTH;
        dynamicChecklistPanel = new JPanel(); // Changed to BoxLayout
        dynamicChecklistPanel.setLayout(new BoxLayout(dynamicChecklistPanel, BoxLayout.Y_AXIS)); // Set BoxLayout
        dynamicChecklistPanel.setOpaque(false);
        
        JScrollPane checklistScrollPane = new JScrollPane(dynamicChecklistPanel); // Wrap in JScrollPane
        checklistScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        checklistScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        checklistScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove border from scroll pane
        checklistScrollPane.getViewport().setBackground(Color.WHITE); // Set viewport background to white
        checklistPanel.add(checklistScrollPane, gbcRight); // Add scroll pane to checklistPanel

        // Add left and right panels to the sideBySideContainer
        sideBySideContainer.add(applicantDetailsPanel);
        sideBySideContainer.add(checklistPanel);

        // Add the sideBySideContainer to the main form panel
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0; // Allow this section to expand
        gbc.fill = GridBagConstraints.BOTH;
        mainFormPanel.add(sideBySideContainer, gbc);

        // Remarks
        gbc.gridx = 0;
        gbc.gridy = 2; // Next row after side-by-side container
        gbc.gridwidth = 2;
        gbc.weighty = 0; // Don't expand
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainFormPanel.add(new JLabel("Remarks:"), gbc);
        gbc.gridy = 3;
        remarksArea = new JTextArea(5, 25);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(remarksArea);
        mainFormPanel.add(scrollPane, gbc);

        // Submit Button
        gbc.gridy = 4;
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
        mainFormPanel.add(submitPlanButton, gbc);

        // Add the main form panel to this panel, centered
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.fill = GridBagConstraints.BOTH;
        outerGbc.weightx = 1.0;
        outerGbc.weighty = 1.0;
        add(mainFormPanel, outerGbc);
    }

    private void loadDynamicChecklist() {
        dynamicChecklistPanel.removeAll();
        checklistItemUploadPanels = new ArrayList<>();

        List<DocumentChecklistItem> items = Database.getAllChecklistItems();
        for (DocumentChecklistItem item : items) {
            ChecklistItemUploadPanel itemPanel = new ChecklistItemUploadPanel(item);
            checklistItemUploadPanels.add(itemPanel);
            dynamicChecklistPanel.add(itemPanel);
            dynamicChecklistPanel.add(Box.createVerticalStrut(5)); // Add spacing between items
        }
        dynamicChecklistPanel.revalidate();
        dynamicChecklistPanel.repaint();
    }

    private void submitNewPlan() {
        String applicantName = currentUser.getName();
        String contact = contactField.getText();
        String plotNo = plotNoField.getText();
        String location = locationField.getText();
        String remarks = remarksArea.getText();

        if (plotNo.isEmpty() || location.isEmpty() || contact.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in Plot No, Location, and Contact.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Document> documents = new ArrayList<>();
        boolean allRequiredAttached = true;

        // First, create the plan to get its ID, which is needed for document storage
        Plan newPlan = new Plan(applicantName, contact, plotNo, location, LocalDate.now(), "Submitted", remarks);
        Database.addPlan(newPlan, new ArrayList<>()); // Add plan first without documents

        if (newPlan.getId() == 0) { // Check if plan was successfully added and ID assigned
            JOptionPane.showMessageDialog(this, "Failed to create new plan in the database.", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create a directory for this plan's documents
        String documentsDir = System.getProperty("user.home") + "/DocLink_Documents/Plan_" + newPlan.getId() + "/";
        File dir = new File(documentsDir);
        if (!dir.exists()) {
            dir.mkdirs(); // Create the directory if it doesn't exist
        }

        for (ChecklistItemUploadPanel itemPanel : checklistItemUploadPanels) {
            DocumentChecklistItem item = itemPanel.getChecklistItem();
            boolean isAttached = itemPanel.isAttached();
            String originalFilePath = itemPanel.getOriginalFilePath(); // Get the original path

            if (item.isRequired() && !isAttached) {
                allRequiredAttached = false;
            }
            
            // Only process if attached (either by file upload or manual check for optional)
            if (isAttached) {
                String savedFilePath = null;
                if (item.requiresFileUpload() && originalFilePath != null) {
                    // If file upload is required and a file was selected, copy it
                    File originalFile = new File(originalFilePath);
                    if (originalFile.exists()) {
                        Path targetPath = Paths.get(documentsDir, originalFile.getName());
                        try {
                            Files.copy(originalFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                            savedFilePath = targetPath.toString();
                        } catch (IOException e) {
                            System.err.println("Error copying file: " + e.getMessage());
                            JOptionPane.showMessageDialog(this, "Error saving document '" + originalFile.getName() + "'.", "File Save Error", JOptionPane.ERROR_MESSAGE);
                            // Decide whether to abort or continue with a null path
                            savedFilePath = null; 
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Selected file '" + originalFile.getName() + "' not found.", "File Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else if (!item.requiresFileUpload()) {
                    // If no file upload is required, but it's marked as attached, store a placeholder path
                    savedFilePath = "N/A (No file upload required)";
                }

                if (savedFilePath != null) {
                    documents.add(new Document(newPlan.getId(), item.getItemName(), savedFilePath, "Submitted"));
                }
            }
        }

        // Now add all collected documents to the database for the new plan
        for (Document doc : documents) {
            Database.addDocument(doc);
        }

        if (!allRequiredAttached) {
            int confirm = JOptionPane.showConfirmDialog(this, "Some required documents are not attached. Do you want to proceed? (Note: In a real system, this might require all documents or a specific process for incomplete submissions.)", "Incomplete Submission", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.NO_OPTION) {
                // If user chooses not to proceed, delete the partially created plan and documents
                Database.deletePlanAndRelatedData(newPlan.getId());
                JOptionPane.showMessageDialog(this, "Submission cancelled. Plan and associated data deleted.", "Submission Cancelled", JOptionPane.INFORMATION_MESSAGE);
                clearSubmissionForm();
                refreshData();
                return;
            }
            // If user chooses to proceed despite missing required documents, the plan will be submitted as "Submitted"
            // and the Receptionist will need to handle it.
        }

        Database.addLog(new Log(newPlan.getId(), currentUser.getRole(), "Reception", "New Plan Submitted by Client", "Initial submission, documents attached."));

        JOptionPane.showMessageDialog(this, "Plan submitted successfully! It will now be reviewed by the Reception.", "Submission Success", JOptionPane.INFORMATION_MESSAGE);
        clearSubmissionForm();
        refreshData();
        parentDashboard.showRoleDashboard("Client");
    }

    private void clearSubmissionForm() {
        contactField.setText(currentUser.getEmail());
        plotNoField.setText("");
        locationField.setText("");
        remarksArea.setText("");
        for (ChecklistItemUploadPanel itemPanel : checklistItemUploadPanels) {
            itemPanel.clearButton.doClick(); // Simulate clicking clear button on each item
        }
    }

    @Override
    public void refreshData() {
        String clientEmail = currentUser.getEmail();

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

        loadDynamicChecklist();
    }
}