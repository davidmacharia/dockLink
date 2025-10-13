package doclink.ui;

import javax.swing.*;
import java.awt.*;

public class DashboardCardsPanel extends JPanel {

    public DashboardCardsPanel() {
        setLayout(new GridLayout(1, 3, 20, 0)); // 1 row, 3 columns, 20px horizontal gap
        setBackground(new Color(245, 247, 250)); // Very light grey
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding

        // Placeholder cards
        add(createCard("Pending Plans", 5, new Color(255, 193, 7))); // Yellow
        add(createCard("Approved Plans", 12, new Color(40, 167, 69))); // Green
        add(createCard("Deferred/Returned", 3, new Color(220, 53, 69))); // Red
    }

    private JPanel createCard(String title, int count, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setPreferredSize(new Dimension(200, 120));
        card.setOpaque(true);
        card.putClientProperty("JButton.buttonType", "roundRect"); // Hint for rounded corners

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(titleLabel, BorderLayout.NORTH);

        JLabel countLabel = new JLabel(String.valueOf(count));
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        countLabel.setForeground(Color.WHITE);
        countLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(countLabel, BorderLayout.CENTER);

        return card;
    }

    public void updateCard(int index, String title, int count, Color bgColor) {
        // This method would update the content of a specific card
        // For now, it's a placeholder. You'd need to store references to the cards.
        // Example:
        // JPanel card = (JPanel) getComponent(index);
        // ((JLabel) card.getComponent(0)).setText(title);
        // ((JLabel) card.getComponent(1)).setText(String.valueOf(count));
        // card.setBackground(bgColor);
    }
}