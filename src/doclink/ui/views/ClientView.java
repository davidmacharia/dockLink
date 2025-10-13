package doclink.ui.views;

import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.DashboardTablePanel;

import javax.swing.*;
import java.awt.*;

public class ClientView extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;

    private DashboardCardsPanel cardsPanel;
    private DashboardTablePanel tablePanel;

    public ClientView(User user, Dashboard parentDashboard) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        cardsPanel = new DashboardCardsPanel();
        add(cardsPanel, BorderLayout.NORTH);

        tablePanel = new DashboardTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        JLabel placeholder = new JLabel("Client Dashboard - View plan status, make payments, download certificates.");
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 18));
        placeholder.setForeground(Color.GRAY);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        add(placeholder, BorderLayout.SOUTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        // Example: Update cards with relevant counts for Client
        // int myPendingPlans = Database.getPlansByApplicant(currentUser.getName(), "Pending").size();
        // cardsPanel.updateCard(0, "My Pending Plans", myPendingPlans, new Color(255, 193, 7));

        // Example: Update table with client's plans
        // List<Plan> clientPlans = Database.getPlansByApplicant(currentUser.getName());
        // tablePanel.updateTable(clientPlans);
    }
}