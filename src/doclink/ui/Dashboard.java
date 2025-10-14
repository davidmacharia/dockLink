package doclink.ui;

import doclink.models.User;
import doclink.ui.views.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage; // Import BufferedImage
import java.util.HashMap;
import java.util.Map;

public class Dashboard extends JFrame {
    private User currentUser;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private Map<String, JPanel> roleViews;

    // Colors
    private static final Color DARK_NAVY = new Color(26, 35, 126);
    private static final Color ROYAL_BLUE = new Color(65, 105, 225);
    private static final Color LIGHT_GREY_BG = new Color(245, 247, 250);
    private static final Color PRIMARY_BLUE = new Color(0, 123, 255);
    private static final Color HOVER_BLUE = new Color(0, 100, 200);
    private static final Color SIDEBAR_TEXT_COLOR = new Color(230, 230, 230); // Very light grey for better contrast

    public Dashboard(User user) {
        this.currentUser = user;
        setTitle("DocLink - " + user.getRole() + " Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximize window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(LIGHT_GREY_BG);

        // --- Sidebar ---
        JPanel sidebar = createSidebar();
        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- Main Content Area (Top Bar + Role Views) ---
        JPanel mainContentArea = new JPanel(new BorderLayout());
        mainContentArea.setBackground(LIGHT_GREY_BG);

        // Top Bar
        JPanel topBar = createTopBar();
        mainContentArea.add(topBar, BorderLayout.NORTH);

        // Role-specific content panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(LIGHT_GREY_BG);

        roleViews = new HashMap<>();
        initializeRoleViews();

        mainContentArea.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(mainContentArea, BorderLayout.CENTER);
        add(mainPanel);

        // Show initial dashboard based on role
        showRoleDashboard(currentUser.getRole());
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
        appLogo.setForeground(SIDEBAR_TEXT_COLOR); // Changed to SIDEBAR_TEXT_COLOR
        appLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        appLogo.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        sidebar.add(appLogo);

        // Menu Items
        addSidebarButton(sidebar, "Dashboard", "Dashboard");
        addSidebarButton(sidebar, "Files", "Files");
        addSidebarButton(sidebar, "Reports", "Reports");
        addSidebarButton(sidebar, "Users", "Users");
        addSidebarButton(sidebar, "Settings", "Settings");
        addSidebarButton(sidebar, "Logout", "Logout");

        // Add some space at the bottom
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private void addSidebarButton(JPanel sidebar, String text, String actionCommand) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(SIDEBAR_TEXT_COLOR); // Changed to SIDEBAR_TEXT_COLOR
        button.setBackground(new Color(0,0,0,0)); // Transparent
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // Make button fill width
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(255, 255, 255, 30)); // Light transparent white
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0,0,0,0));
            }
        });

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSidebarAction(actionCommand);
            }
        });
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(10)); // Spacing between buttons
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setPreferredSize(new Dimension(0, 60));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220))); // Bottom border

        // Search bar
        JTextField searchField = new JTextField("Search...");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setForeground(Color.GRAY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchField.setPreferredSize(new Dimension(300, 35));
        searchField.setMaximumSize(new Dimension(300, 35));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.add(searchField);
        topBar.add(searchPanel, BorderLayout.WEST);

        // User/Menu icons
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        iconPanel.setBackground(Color.WHITE);

        // Corrected usage: createScaledIcon now returns Image, which is then used to create a new ImageIcon
        JButton notificationButton = new JButton(new ImageIcon(createScaledIcon("icons/bell.png", 20, 20)));
        JButton userButton = new JButton(new ImageIcon(createScaledIcon("icons/user.png", 20, 20)));

        styleIconButton(notificationButton);
        styleIconButton(userButton);

        JLabel userNameLabel = new JLabel(currentUser.getName() + " (" + currentUser.getRole() + ")");
        userNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userNameLabel.setForeground(DARK_NAVY);

        iconPanel.add(userNameLabel);
        iconPanel.add(notificationButton);
        iconPanel.add(userButton);
        topBar.add(iconPanel, BorderLayout.EAST);

        return topBar;
    }

    private void styleIconButton(JButton button) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // Modified to return Image instead of ImageIcon
    private Image createScaledIcon(String path, int width, int height) {
        // This is a placeholder. In a real app, you'd load actual icons.
        // For now, it creates a blank icon.
        // You would need to place actual image files (e.g., bell.png, user.png)
        // in a 'icons' folder within your project's resources.
        // Example: Image img = new ImageIcon(getClass().getResource(path)).getImage();
        // return img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private void initializeRoleViews() {
        // Initialize all role-specific views
        ReceptionView receptionView = new ReceptionView(currentUser, this);
        PlanningView planningView = new PlanningView(currentUser, this);
        CommitteeView committeeView = new CommitteeView(currentUser, this);
        DirectorView directorView = new DirectorView(currentUser, this);
        StructuralView structuralView = new StructuralView(currentUser, this);
        ClientView clientView = new ClientView(currentUser, this);

        roleViews.put("Reception", receptionView);
        roleViews.put("Planning", planningView);
        roleViews.put("Committee", committeeView);
        roleViews.put("Director", directorView);
        roleViews.put("Structural", structuralView);
        roleViews.put("Client", clientView);

        // Add all views to the card layout
        contentPanel.add(receptionView, "Reception");
        contentPanel.add(planningView, "Planning");
        contentPanel.add(committeeView, "Committee");
        contentPanel.add(directorView, "Director");
        contentPanel.add(structuralView, "Structural");
        contentPanel.add(clientView, "Client");
    }

    public void showRoleDashboard(String role) {
        JPanel view = roleViews.get(role);
        if (view instanceof Refreshable) {
            ((Refreshable) view).refreshData(); // Refresh data when switching views
        }
        cardLayout.show(contentPanel, role);
        setTitle("DocLink - " + currentUser.getRole() + " Dashboard");
    }

    private void handleSidebarAction(String actionCommand) {
        switch (actionCommand) {
            case "Dashboard":
                showRoleDashboard(currentUser.getRole());
                break;
            case "Files":
                // Implement a generic file view or redirect to role-specific file view
                JOptionPane.showMessageDialog(this, "Files view for " + currentUser.getRole(), "Info", JOptionPane.INFORMATION_MESSAGE);
                break;
            case "Reports":
                JOptionPane.showMessageDialog(this, "Reports view for " + currentUser.getRole(), "Info", JOptionPane.INFORMATION_MESSAGE);
                break;
            case "Users":
                if (currentUser.getRole().equals("Director")) { // Example of role-based access for menu items
                    JOptionPane.showMessageDialog(this, "Manage Users", "Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Access Denied: Only Director can manage users.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                break;
            case "Settings":
                JOptionPane.showMessageDialog(this, "Settings for " + currentUser.getRole(), "Info", JOptionPane.INFORMATION_MESSAGE);
                break;
            case "Logout":
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    dispose();
                    LoginFrame loginFrame = new LoginFrame();
                    loginFrame.setVisible(true);
                }
                break;
            default:
                break;
        }
    }

    // Interface for views that need to refresh their data
    public interface Refreshable {
        void refreshData();
    }
}