package doclink.ui;

import doclink.models.User;
import doclink.ui.panels.client.ClientDashboardPanel;
import doclink.ui.panels.client.ClientNewPlanPanel; // New import
import doclink.ui.panels.committee.CommitteeReviewPanel;
import doclink.ui.panels.director.DirectorReviewPanel;
import doclink.ui.panels.planning.PlanningReviewPanel;
import doclink.ui.panels.reception.ReceptionAllPlansPanel;
import doclink.ui.panels.reception.ReceptionClientCommunicationPanel;
import doclink.ui.panels.reception.ReceptionDirectorDecisionsPanel;
import doclink.ui.panels.reception.ReceptionPaymentPanel;
import doclink.ui.panels.reception.ReceptionSubmissionPanel;
import doclink.ui.panels.structural.StructuralReviewPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Dashboard extends JFrame {
    private User currentUser;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private DashboardCardsPanel cardsPanel; // Centralized cards panel
    private HeaderPanel headerPanel; // New Header Panel

    // Colors
    private static final Color DARK_NAVY = new Color(26, 35, 126);
    private static final Color ROYAL_BLUE = new Color(65, 105, 225);
    private static final Color LIGHT_GREY_BG = new Color(245, 247, 250); 
    private static final Color PRIMARY_BLUE = new Color(0, 123, 255);
    private static final Color HOVER_BLUE = new Color(0, 100, 200);

    // Map to hold all functional panels
    private Map<String, JPanel> functionalPanels;
    // Map to hold refreshable panels for easy refresh
    private Map<String, Refreshable> refreshablePanels;

    public Dashboard(User user) {
        this.currentUser = user;
        setTitle("DocLink - " + user.getRole() + " Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(LIGHT_GREY_BG); // Reverted to light grey

        // Add the HeaderPanel to the NORTH of the mainPanel
        headerPanel = new HeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // This panel will now hold the sidebar (WEST) and the rest of the dashboard content (CENTER)
        JPanel dashboardMainContent = new JPanel(new BorderLayout());
        dashboardMainContent.setBackground(LIGHT_GREY_BG); // Reverted to light grey

        // Sidebar
        JPanel sidebar = createSidebar();
        dashboardMainContent.add(sidebar, BorderLayout.WEST);

        // This panel will hold the cards (NORTH) and the functional panels (CENTER)
        JPanel rightHandContentPanel = new JPanel(new BorderLayout());
        rightHandContentPanel.setBackground(LIGHT_GREY_BG); // Reverted to light grey

        // Centralized Cards Panel at the top of the right-hand content
        cardsPanel = new DashboardCardsPanel();
        rightHandContentPanel.add(cardsPanel, BorderLayout.NORTH);

        // Functional content panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout); // This is the panel that holds the actual views
        contentPanel.setBackground(LIGHT_GREY_BG); // Reverted to light grey

        functionalPanels = new LinkedHashMap<>(); // Use LinkedHashMap to maintain insertion order for sidebar buttons
        refreshablePanels = new HashMap<>();

        initializeAllPanels(); // Initialize all panels and add them to contentPanel

        rightHandContentPanel.add(contentPanel, BorderLayout.CENTER); // Functional panels are in the center of rightHandContentPanel
        dashboardMainContent.add(rightHandContentPanel, BorderLayout.CENTER); // rightHandContentPanel is in the center of dashboardMainContent
        mainPanel.add(dashboardMainContent, BorderLayout.CENTER); // dashboardMainContent is in the center of mainPanel

        add(mainPanel);

        // Show the default panel for the current user's role
        showDefaultRolePanel(currentUser.getRole());
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, DARK_NAVY, 0, getHeight(), ROYAL_BLUE);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        // App Logo
        JLabel appLogo = new JLabel("DocLink");
        appLogo.setFont(new Font("Segoe UI", Font.BOLD, 32));
        appLogo.setForeground(Color.WHITE);
        appLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        appLogo.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        sidebar.add(appLogo);

        // Add role-specific menu items
        switch (currentUser.getRole()) {
            case "Reception":
                addSidebarButton(sidebar, "New Plan Submission", "ReceptionSubmissionPanel");
                addSidebarButton(sidebar, "All Plans", "ReceptionAllPlansPanel");
                addSidebarButton(sidebar, "Payment Processing", "ReceptionPaymentPanel");
                addSidebarButton(sidebar, "Director Decisions", "ReceptionDirectorDecisionsPanel");
                addSidebarButton(sidebar, "Client Communication", "ReceptionClientCommunicationPanel");
                break;
            case "Planning":
                addSidebarButton(sidebar, "Plan Review", "PlanningReviewPanel");
                addSidebarButton(sidebar, "Files", "PlanningFilesPanel"); // Placeholder
                addSidebarButton(sidebar, "Reports", "PlanningReportsPanel"); // Placeholder
                break;
            case "Committee":
                addSidebarButton(sidebar, "Plan Review", "CommitteeReviewPanel");
                addSidebarButton(sidebar, "Meetings", "CommitteeMeetingsPanel"); // Placeholder
                break;
            case "Director":
                addSidebarButton(sidebar, "Plan Review", "DirectorReviewPanel");
                addSidebarButton(sidebar, "User Management", "DirectorUserManagementPanel"); // Placeholder
                addSidebarButton(sidebar, "Reports", "DirectorReportsPanel"); // Placeholder
                break;
            case "Structural":
                addSidebarButton(sidebar, "Structural Review", "StructuralReviewPanel");
                addSidebarButton(sidebar, "Calculations", "StructuralCalculationsPanel"); // Placeholder
                break;
            case "Client":
                addSidebarButton(sidebar, "My Plans", "ClientDashboardPanel");
                addSidebarButton(sidebar, "Submit New Plan", "ClientNewPlanPanel"); // Placeholder
                break;
        }

        // Common items
        addSidebarButton(sidebar, "Settings", "SettingsPanel"); // Placeholder
        addSidebarButton(sidebar, "Logout", "Logout");

        return sidebar;
    }

    private void addSidebarButton(JPanel sidebar, String text, String panelName) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE); // Default text color
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false); // <--- Set to false initially for transparency
        button.setOpaque(false); // Keep opaque false so gradient shows through by default
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // Make button fill width
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); // Initial border is empty

        // Store default background color (transparent)
        Color defaultBg = new Color(0,0,0,0); // Fully transparent

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setForeground(new Color(200, 200, 255)); // Lighter text on hover
                button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1)); // Add white border on hover
                button.setBackground(ROYAL_BLUE); // Set hover background color to opaque ROYAL_BLUE
                button.setOpaque(true); // Make it opaque to show the background color
                button.setContentAreaFilled(true); // <--- Set to true on hover
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setForeground(Color.WHITE); // Revert text color
                button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); // Remove border on exit
                button.setBackground(defaultBg); // Revert background to transparent
                button.setOpaque(false); // Make it non-opaque again
                button.setContentAreaFilled(false); // <--- Set to false on exit
            }
        });

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSidebarAction(panelName);
            }
        });
        sidebar.add(button);
    }

    private void initializeAllPanels() {
        // Reception Panels
        ReceptionSubmissionPanel receptionSubmissionPanel = new ReceptionSubmissionPanel(currentUser, this, cardsPanel);
        functionalPanels.put("ReceptionSubmissionPanel", receptionSubmissionPanel);
        refreshablePanels.put("ReceptionSubmissionPanel", receptionSubmissionPanel);

        ReceptionAllPlansPanel receptionAllPlansPanel = new ReceptionAllPlansPanel(currentUser, this, cardsPanel);
        functionalPanels.put("ReceptionAllPlansPanel", receptionAllPlansPanel);
        refreshablePanels.put("ReceptionAllPlansPanel", receptionAllPlansPanel);

        ReceptionPaymentPanel receptionPaymentPanel = new ReceptionPaymentPanel(currentUser, this, cardsPanel);
        functionalPanels.put("ReceptionPaymentPanel", receptionPaymentPanel);
        refreshablePanels.put("ReceptionPaymentPanel", receptionPaymentPanel);

        ReceptionDirectorDecisionsPanel receptionDirectorDecisionsPanel = new ReceptionDirectorDecisionsPanel(currentUser, this, cardsPanel);
        functionalPanels.put("ReceptionDirectorDecisionsPanel", receptionDirectorDecisionsPanel);
        refreshablePanels.put("ReceptionDirectorDecisionsPanel", receptionDirectorDecisionsPanel);

        ReceptionClientCommunicationPanel receptionClientCommunicationPanel = new ReceptionClientCommunicationPanel(currentUser, this, cardsPanel);
        functionalPanels.put("ReceptionClientCommunicationPanel", receptionClientCommunicationPanel);
        refreshablePanels.put("ReceptionClientCommunicationPanel", receptionClientCommunicationPanel);

        // Planning Panels
        PlanningReviewPanel planningReviewPanel = new PlanningReviewPanel(currentUser, this, cardsPanel);
        functionalPanels.put("PlanningReviewPanel", planningReviewPanel);
        refreshablePanels.put("PlanningReviewPanel", planningReviewPanel);
        // Add placeholders for other Planning panels
        functionalPanels.put("PlanningFilesPanel", createPlaceholderPanel("Planning Files View"));
        functionalPanels.put("PlanningReportsPanel", createPlaceholderPanel("Planning Reports View"));

        // Committee Panels
        CommitteeReviewPanel committeeReviewPanel = new CommitteeReviewPanel(currentUser, this, cardsPanel);
        functionalPanels.put("CommitteeReviewPanel", committeeReviewPanel);
        refreshablePanels.put("CommitteeReviewPanel", committeeReviewPanel);
        // Add placeholders for other Committee panels
        functionalPanels.put("CommitteeMeetingsPanel", createPlaceholderPanel("Committee Meetings View"));

        // Director Panels
        DirectorReviewPanel directorReviewPanel = new DirectorReviewPanel(currentUser, this, cardsPanel);
        functionalPanels.put("DirectorReviewPanel", directorReviewPanel);
        refreshablePanels.put("DirectorReviewPanel", directorReviewPanel);
        // Add placeholders for other Director panels
        functionalPanels.put("DirectorUserManagementPanel", createPlaceholderPanel("Director User Management View"));
        functionalPanels.put("DirectorReportsPanel", createPlaceholderPanel("Director Reports View"));

        // Structural Panels
        StructuralReviewPanel structuralReviewPanel = new StructuralReviewPanel(currentUser, this, cardsPanel);
        functionalPanels.put("StructuralReviewPanel", structuralReviewPanel);
        refreshablePanels.put("StructuralReviewPanel", structuralReviewPanel);
        // Add placeholders for other Structural panels
        functionalPanels.put("StructuralCalculationsPanel", createPlaceholderPanel("Structural Calculations View"));

        // Client Panels
        ClientDashboardPanel clientDashboardPanel = new ClientDashboardPanel(currentUser, this, cardsPanel);
        functionalPanels.put("ClientDashboardPanel", clientDashboardPanel);
        refreshablePanels.put("ClientDashboardPanel", clientDashboardPanel);
        
        ClientNewPlanPanel clientNewPlanPanel = new ClientNewPlanPanel(currentUser, this, cardsPanel); // Instantiate new panel
        functionalPanels.put("ClientNewPlanPanel", clientNewPlanPanel); // Add to map
        refreshablePanels.put("ClientNewPlanPanel", clientNewPlanPanel); // Add to refreshable map

        // Common Panels
        functionalPanels.put("SettingsPanel", createPlaceholderPanel("Settings View"));

        // Add all panels to the CardLayout
        for (Map.Entry<String, JPanel> entry : functionalPanels.entrySet()) {
            contentPanel.add(entry.getValue(), entry.getKey());
        }
    }

    private JPanel createPlaceholderPanel(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(LIGHT_GREY_BG); // Reverted to light grey
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        label.setForeground(DARK_NAVY); // Kept DARK_NAVY for title contrast
        panel.add(label);
        return panel;
    }

    private void showDefaultRolePanel(String role) {
        String defaultPanelName = "";
        switch (role) {
            case "Reception":
                defaultPanelName = "ReceptionAllPlansPanel";
                break;
            case "Planning":
                defaultPanelName = "PlanningReviewPanel";
                break;
            case "Committee":
                defaultPanelName = "CommitteeReviewPanel";
                break;
            case "Director":
                defaultPanelName = "DirectorReviewPanel";
                break;
            case "Structural":
                defaultPanelName = "StructuralReviewPanel";
                break;
            case "Client":
                defaultPanelName = "ClientDashboardPanel";
                break;
            default:
                defaultPanelName = "SettingsPanel"; // Fallback
                break;
        }
        handleSidebarAction(defaultPanelName);
    }

    private void handleSidebarAction(String panelName) {
        if (panelName.equals("Logout")) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            }
        } else {
            // Refresh the data for the target panel before showing it
            Refreshable refreshablePanel = refreshablePanels.get(panelName);
            if (refreshablePanel != null) {
                refreshablePanel.refreshData();
            }
            cardLayout.show(contentPanel, panelName);
        }
    }

    // New method to allow panels to trigger a dashboard view change and refresh
    public void showRoleDashboard(String role) {
        showDefaultRolePanel(role);
    }

    // Interface for panels that need to refresh their data
    public interface Refreshable {
        void refreshData();
    }
}