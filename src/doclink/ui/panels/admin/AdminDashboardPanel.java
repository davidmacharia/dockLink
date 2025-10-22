package doclink.ui.panels.admin;

import doclink.Database;
import doclink.models.Log;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminDashboardPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private JTextArea recentActivitiesArea; // New component for recent activities

    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public AdminDashboardPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel panelTitle = new JLabel("Admin Dashboard Overview");
        panelTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panelTitle.setForeground(DARK_NAVY);
        panelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(panelTitle, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getName() + " (" + currentUser.getRole() + ")");
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        content.add(welcomeLabel, gbc);

        // New: Recent Activities Section
        gbc.gridy++;
        JLabel activitiesTitle = new JLabel("Recent System Activities:");
        activitiesTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        activitiesTitle.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        content.add(activitiesTitle, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0; // Allow this area to expand vertically
        gbc.fill = GridBagConstraints.BOTH; // Fill both horizontally and vertically
        recentActivitiesArea = new JTextArea(10, 50); // Set initial size
        recentActivitiesArea.setEditable(false);
        recentActivitiesArea.setLineWrap(true);
        recentActivitiesArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(recentActivitiesArea);
        content.add(scrollPane, gbc);

        add(content, BorderLayout.CENTER);

        refreshData();
    }

    private void loadRecentActivities() {
        List<Log> recentLogs = Database.getRecentLogs(10); // Get the last 10 activities
        StringBuilder activitiesText = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"); // Added time for more detail

        if (recentLogs.isEmpty()) {
            activitiesText.append("No recent activities found.");
        } else {
            for (Log log : recentLogs) {
                activitiesText.append(String.format("[%s] Plan ID %d: %s from %s to %s. Remarks: %s\n",
                    log.getDate().atStartOfDay().format(formatter), // Assuming date is LocalDate, convert to LocalDateTime for formatting
                    log.getPlanId(),
                    log.getAction(),
                    log.getFromRole(),
                    log.getToRole(),
                    log.getRemarks() != null && !log.getRemarks().isEmpty() ? log.getRemarks() : "N/A"
                ));
            }
        }
        recentActivitiesArea.setText(activitiesText.toString());
        recentActivitiesArea.setCaretPosition(0); // Scroll to top
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Admin
        List<User> allUsers = Database.getAllUsers();
        int totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(u -> !u.getRole().equals("Blocked")).count(); // Assuming 'Blocked' role for inactive
        long adminUsers = allUsers.stream().filter(u -> u.getRole().equals("Admin")).count();

        cardsPanel.updateCard(0, "Total Users", totalUsers, new Color(0, 123, 255)); // Blue
        cardsPanel.updateCard(1, "Active Users", (int) activeUsers, new Color(40, 167, 69)); // Green
        cardsPanel.updateCard(2, "Admin Accounts", (int) adminUsers, new Color(255, 193, 7)); // Yellow

        loadRecentActivities(); // Load and display recent activities
    }
}