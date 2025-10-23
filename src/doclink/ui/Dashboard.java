package doclink.ui;

import doclink.models.User;
import doclink.ui.panels.admin.AdminDashboardPanel;
import doclink.ui.panels.admin.AdminRejectedPlansPanel;
import doclink.ui.panels.admin.AdminUserManagementPanel;
import doclink.ui.panels.client.ClientDashboardPanel;
import doclink.ui.panels.client.ClientNewPlanPanel;
import doclink.ui.panels.committee.CommitteeMeetingsPanel; // NEW: Import CommitteeMeetingsPanel
import doclink.ui.panels.committee.CommitteeReviewPanel;
import doclink.ui.panels.director.DirectorAllPlansPanel;
import doclink.ui.panels.director.DirectorReviewPanel;
import doclink.ui.panels.director.DirectorReportsPanel;
import doclink.ui.panels.planning.PlanningFilesPanel;
import doclink.ui.panels.planning.PlanningReviewPanel;
import doclink.ui.panels.planning.PlanningReportsPanel;
import doclink.ui.panels.reception.ReceptionAllPlansPanel;
import doclink.ui.panels.reception.ReceptionSubmissionPanel;
import doclink.ui.panels.structural.StructuralReviewPanel;
import doclink.ui.panels.SettingsPanel;

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
        mainPanel.setBackground(LIGHT_GREY_BG);

        headerPanel = new HeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel dashboardMainContent = new JPanel(new BorderLayout());
        dashboardMainContent.setBackground(LIGHT_GREY_BG);

        JPanel sidebar = createSidebar();
        dashboardMainContent.add(sidebar, BorderLayout.WEST);

        JPanel rightHandContentPanel = new JPanel(new BorderLayout());
        rightHandContentPanel.setBackground(LIGHT_GREY_BG);

        cardsPanel = new DashboardCardsPanel();
        rightHandContentPanel.add(cardsPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(LIGHT_GREY_BG);

        functionalPanels = new LinkedHashMap<>();
        refreshablePanels = new HashMap<>();

        initializeAllPanels();

        // Add all initialized panels to the contentPanel
        for (Map.Entry<String, JPanel> entry : functionalPanels.entrySet()) {
            contentPanel.add(entry.getValue(), entry.getKey());
        }

        rightHandContentPanel.add(contentPanel, BorderLayout.CENTER);
        dashboardMainContent.add(rightHandContentPanel, BorderLayout.CENTER);
        mainPanel.add(dashboardMainContent, BorderLayout.CENTER);

        add(mainPanel);

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

        JLabel appLogo = new JLabel("DocLink");
        appLogo.setFont(new Font("Segoe UI", Font.BOLD, 32));
        appLogo.setForeground(Color.WHITE);
        appLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        appLogo.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        sidebar.add(appLogo);

        switch (currentUser.getRole()) {
            case "Reception":
                addSidebarButton(sidebar, "New Plan Submission", "ReceptionSubmissionPanel");
                addSidebarButton(sidebar, "All Plans", "ReceptionAllPlansPanel");
                break;
            case "Planning":
                addSidebarButton(sidebar, "Plan Review", "PlanningReviewPanel");
                addSidebarButton(sidebar, "Files", "PlanningFilesPanel");
                addSidebarButton(sidebar, "Reports", "PlanningReportsPanel");
                break;
            case "Committee":
                addSidebarButton(sidebar, "Plan Review", "CommitteeReviewPanel");
                addSidebarButton(sidebar, "Meetings", "CommitteeMeetingsPanel"); // NEW: Added Meetings button
                break;
            case "Director":
                addSidebarButton(sidebar, "Plan Review", "DirectorReviewPanel");
                addSidebarButton(sidebar, "All Plans Overview", "DirectorAllPlansPanel");
                addSidebarButton(sidebar, "Reports", "DirectorReportsPanel");
                break;
            case "Structural":
                addSidebarButton(sidebar, "Structural Review", "StructuralReviewPanel");
                addSidebarButton(sidebar, "Calculations", "StructuralCalculationsPanel");
                break;
            case "Client":
                addSidebarButton(sidebar, "My Plans", "ClientDashboardPanel");
                addSidebarButton(sidebar, "Submit New Plan", "ClientNewPlanPanel");
                break;
            case "Admin": // New Admin Role
                addSidebarButton(sidebar, "Dashboard", "AdminDashboardPanel");
                addSidebarButton(sidebar, "User Management", "AdminUserManagementPanel");
                addSidebarButton(sidebar, "Rejected Plans", "AdminRejectedPlansPanel");
                break;
        }

        addSidebarButton(sidebar, "Settings", "SettingsPanel");
        addSidebarButton(sidebar, "Logout", "Logout");

        return sidebar;
    }

    private void addSidebarButton(JPanel sidebar, String text, String panelName) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT); // Align the button component to the left
        button.setHorizontalAlignment(SwingConstants.LEFT); // Align text within the button to the left
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        Color defaultBg = new Color(0,0,0,0);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setForeground(new Color(200, 200, 255));
                button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
                button.setBackground(ROYAL_BLUE);
                button.setOpaque(true);
                button.setContentAreaFilled(true);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setForeground(Color.WHITE);
                button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                button.setBackground(defaultBg);
                button.setOpaque(false);
                button.setContentAreaFilled(false);
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

        // Planning Panels
        PlanningReviewPanel planningReviewPanel = new PlanningReviewPanel(currentUser, this, cardsPanel);
        functionalPanels.put("PlanningReviewPanel", planningReviewPanel);
        refreshablePanels.put("PlanningReviewPanel", planningReviewPanel);
        
        PlanningFilesPanel planningFilesPanel = new PlanningFilesPanel(currentUser, this, cardsPanel);
        functionalPanels.put("PlanningFilesPanel", planningFilesPanel);
        refreshablePanels.put("PlanningFilesPanel", planningFilesPanel);

        PlanningReportsPanel planningReportsPanel = new PlanningReportsPanel(currentUser, this, cardsPanel);
        functionalPanels.put("PlanningReportsPanel", planningReportsPanel);
        refreshablePanels.put("PlanningReportsPanel", planningReportsPanel);

        // Committee Panels
        CommitteeReviewPanel committeeReviewPanel = new CommitteeReviewPanel(currentUser, this, cardsPanel);
        functionalPanels.put("CommitteeReviewPanel", committeeReviewPanel);
        refreshablePanels.put("CommitteeReviewPanel", committeeReviewPanel);
        
        CommitteeMeetingsPanel committeeMeetingsPanel = new CommitteeMeetingsPanel(currentUser, this, cardsPanel); // NEW: Instantiate
        functionalPanels.put("CommitteeMeetingsPanel", committeeMeetingsPanel); // NEW: Add to map
        refreshablePanels.put("CommitteeMeetingsPanel", committeeMeetingsPanel); // NEW: Add to refreshable map

        // Director Panels
        DirectorReviewPanel directorReviewPanel = new DirectorReviewPanel(currentUser, this, cardsPanel);
        functionalPanels.put("DirectorReviewPanel", directorReviewPanel);
        refreshablePanels.put("DirectorReviewPanel", directorReviewPanel);
        
        DirectorAllPlansPanel directorAllPlansPanel = new DirectorAllPlansPanel(currentUser, this, cardsPanel);
        functionalPanels.put("DirectorAllPlansPanel", directorAllPlansPanel);
        refreshablePanels.put("DirectorAllPlansPanel", directorAllPlansPanel);

        DirectorReportsPanel directorReportsPanel = new DirectorReportsPanel(currentUser, this, cardsPanel);
        functionalPanels.put("DirectorReportsPanel", directorReportsPanel);
        refreshablePanels.put("DirectorReportsPanel", directorReportsPanel);

        // Structural Panels
        StructuralReviewPanel structuralReviewPanel = new StructuralReviewPanel(currentUser, this, cardsPanel);
        functionalPanels.put("StructuralReviewPanel", structuralReviewPanel);
        refreshablePanels.put("StructuralReviewPanel", structuralReviewPanel);
        functionalPanels.put("StructuralCalculationsPanel", createPlaceholderPanel("Structural Calculations View"));

        // Client Panels
        ClientDashboardPanel clientDashboardPanel = new ClientDashboardPanel(currentUser, this, cardsPanel);
        functionalPanels.put("ClientDashboardPanel", clientDashboardPanel);
        refreshablePanels.put("ClientDashboardPanel", clientDashboardPanel);
        
        ClientNewPlanPanel clientNewPlanPanel = new ClientNewPlanPanel(currentUser, this, cardsPanel);
        functionalPanels.put("ClientNewPlanPanel", clientNewPlanPanel);
        refreshablePanels.put("ClientNewPlanPanel", clientNewPlanPanel);

        // Admin Panels (New)
        AdminDashboardPanel adminDashboardPanel = new AdminDashboardPanel(currentUser, this, cardsPanel);
        functionalPanels.put("AdminDashboardPanel", adminDashboardPanel);
        refreshablePanels.put("AdminDashboardPanel", adminDashboardPanel);

        AdminUserManagementPanel adminUserManagementPanel = new AdminUserManagementPanel(currentUser, this, cardsPanel);
        functionalPanels.put("AdminUserManagementPanel", adminUserManagementPanel);
        refreshablePanels.put("AdminUserManagementPanel", adminUserManagementPanel);

        AdminRejectedPlansPanel adminRejectedPlansPanel = new AdminRejectedPlansPanel(currentUser, this, cardsPanel);
        functionalPanels.put("AdminRejectedPlansPanel", adminRejectedPlansPanel);
        refreshablePanels.put("AdminRejectedPlansPanel", adminRejectedPlansPanel);

        // Common Panels
        SettingsPanel settingsPanel = new SettingsPanel(currentUser, this, cardsPanel);
        functionalPanels.put("SettingsPanel", settingsPanel);
        refreshablePanels.put("SettingsPanel", settingsPanel);
    }

    private JPanel createPlaceholderPanel(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(LIGHT_GREY_BG);
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        label.setForeground(DARK_NAVY);
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
                defaultPanelName = "CommitteeReviewPanel"; // Default to review panel
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
            case "Admin":
                defaultPanelName = "AdminDashboardPanel";
                break;
            default:
                defaultPanelName = "SettingsPanel";
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
            Refreshable refreshablePanel = refreshablePanels.get(panelName);
            if (refreshablePanel != null) {
                refreshablePanel.refreshData();
            }
            cardLayout.show(contentPanel, panelName);
        }
    }

    public void showRoleDashboard(String role) {
        showDefaultRolePanel(role);
    }

    public interface Refreshable {
        void refreshData();
    }
}