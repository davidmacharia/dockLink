package doclink.ui.panels.planning;

import doclink.Database;
import doclink.models.Plan;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;
import doclink.ui.DashboardTablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PlanningFilesPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Reference to the main dashboard cards

    private DashboardTablePanel allPlansTablePanel;
    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public PlanningFilesPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
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
        // Update cards with relevant counts for Planning
        int pendingPlanning = Database.getPlansByStatus("Under Review (Planning)").size();
        int awaitingPayment = Database.getPlansByStatus("Awaiting Payment").size();
        int paymentReceived = Database.getPlansByStatus("Payment Received").size();
        int returnedFromDirectorOrCommitteeOrStructural = Database.getPlansByStatus("Rejected (to Planning)").size() +
                                             Database.getPlansByStatus("Deferred (to Planning)").size() +
                                             Database.getPlansByStatus("Rejected").size() +
                                             Database.getPlansByStatus("Deferred").size() +
                                             Database.getPlansByStatus("Rejected by Structural (to Planning)").size();

        cardsPanel.updateCard(0, "New Plans for Review", pendingPlanning, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Awaiting Payment", awaitingPayment, new Color(255, 165, 0)); // Orange
        cardsPanel.updateCard(2, "Payment Received / Returned", paymentReceived + returnedFromDirectorOrCommitteeOrStructural, new Color(40, 167, 69)); // Green

        // Update the table with all plans
        List<Plan> allPlans = Database.getAllPlans();
        allPlansTablePanel.updateTable(allPlans);
    }
}