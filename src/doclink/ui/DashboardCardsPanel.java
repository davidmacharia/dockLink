package doclink.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardCardsPanel extends JPanel {
    private List<JPanel> cards; // Store references to the cards

    public DashboardCardsPanel() {
        setLayout(new GridLayout(1, 3, 20, 0)); // 1 row, 3 columns, 20px horizontal gap
        setBackground(new Color(245, 247, 250)); // Very light grey
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding

        cards = new ArrayList<>();

        // Initialize and add placeholder cards
        cards.add(createCard("Card 1 Title", 0, new Color(255, 193, 7)));
        cards.add(createCard("Card 2 Title", 0, new Color(40, 167, 69)));
        cards.add(createCard("Card 3 Title", 0, new Color(220, 53, 69)));

        for (JPanel card : cards) {
            add(card);
        }
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
        if (index >= 0 && index < cards.size()) {
            JPanel card = cards.get(index);
            // Update title
            ((JLabel) card.getComponent(0)).setText(title);
            // Update count
            ((JLabel) card.getComponent(1)).setText(String.valueOf(count));
            // Update background color
            card.setBackground(bgColor);
            card.revalidate();
            card.repaint();
        } else {
            System.err.println("Error: Card index out of bounds: " + index);
        }
    }
}