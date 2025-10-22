package doclink.ui.panels.director;

import doclink.Database;
import doclink.models.Plan;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.DashboardTablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DirectorAllPlansPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    private DashboardTablePanel allPlansTablePanel;
    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public DirectorAllPlansPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel panelTitle = new JLabel("All Plans Overview");
        panelTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panelTitle.setForeground(DARK_NAVY);
        panelTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(panelTitle, BorderLayout.NORTH);

        allPlansTablePanel = new DashboardTablePanel();
        add(allPlansTablePanel, BorderLayout.CENTER);

        refreshData(); // Initial data load
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Director
        int plansForReview = Database.getPlansByStatus("Under Review (Director)").size();
        int approvedToReceptionStructural = Database.getPlansByStatus("Approved by Director (to Reception for Structural)").size();
        int approvedToReceptionClient = Database.getPlansByStatus("Approved (Awaiting Client Pickup)").size();
        int returnedPlans = Database.getPlansByStatus("Rejected (to Planning)").size() + Database.getPlansByStatus("Deferred (to Planning)").size();

        cardsPanel.updateCard(0, "Plans for Director Review", plansForReview, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Approved (to Reception)", approvedToReceptionStructural + approvedToReceptionClient, new Color(40, 167, 69)); // Green
        cardsPanel.updateCard(2, "Returned Plans", returnedPlans, new Color(220, 53, 69)); // Red

        // Update the table with all plans
        List<Plan> allPlans = Database.getAllPlans();
        allPlansTablePanel.updateTable(allPlans);
    }
}