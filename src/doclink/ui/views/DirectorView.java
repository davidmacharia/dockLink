package doclink.ui.views;

import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.DashboardTablePanel;

import javax.swing.*;
import java.awt.*;

public class DirectorView extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;

    private DashboardCardsPanel cardsPanel;
    private DashboardTablePanel tablePanel;

    public DirectorView(User user, Dashboard parentDashboard) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        cardsPanel = new DashboardCardsPanel();
        add(cardsPanel, BorderLayout.NORTH);

        tablePanel = new DashboardTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        JLabel placeholder = new JLabel("Director Dashboard - View committee decisions, Approve/Defer/Reject.");
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 18));
        placeholder.setForeground(Color.GRAY);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        add(placeholder, BorderLayout.SOUTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        // Example: Update cards with relevant counts for Director
        // int plansForApproval = Database.getPlansByStatus("Under Review (Director)").size();
        // cardsPanel.updateCard(0, "Plans for Approval", plansForApproval, new Color(255, 193, 7));

        // Example: Update table with plans for Director review
        // List<Plan> directorPlans = Database.getPlansByStatus("Under Review (Director)");
        // tablePanel.updateTable(directorPlans);
    }
}