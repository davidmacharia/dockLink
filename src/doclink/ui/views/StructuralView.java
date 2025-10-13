package doclink.ui.views;

import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.DashboardTablePanel;

import javax.swing.*;
import java.awt.*;

public class StructuralView extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;

    private DashboardCardsPanel cardsPanel;
    private DashboardTablePanel tablePanel;

    public StructuralView(User user, Dashboard parentDashboard) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        cardsPanel = new DashboardCardsPanel();
        add(cardsPanel, BorderLayout.NORTH);

        tablePanel = new DashboardTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        JLabel placeholder = new JLabel("Structural Section Dashboard - Review design for safety compliance.");
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 18));
        placeholder.setForeground(Color.GRAY);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        add(placeholder, BorderLayout.SOUTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        // Example: Update cards with relevant counts for Structural
        // int plansForReview = Database.getPlansByStatus("Under Review (Structural)").size();
        // cardsPanel.updateCard(0, "Plans for Structural Review", plansForReview, new Color(255, 193, 7));

        // Example: Update table with plans for Structural review
        // List<Plan> structuralPlans = Database.getPlansByStatus("Under Review (Structural)");
        // tablePanel.updateTable(structuralPlans);
    }
}