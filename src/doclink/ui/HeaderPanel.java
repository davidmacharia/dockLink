package doclink.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HeaderPanel extends JPanel {
    private JButton menuButton;

    // Define colors consistent with the sidebar
    private static final Color DARK_NAVY = new Color(26, 35, 126);
    private static final Color ROYAL_BLUE = new Color(65, 105, 225);

    public HeaderPanel() {
        setLayout(new BorderLayout(10, 0));
        // setBackground(new Color(245, 247, 250)); // Removed as paintComponent will handle background
        setOpaque(false); // Set to false so paintComponent can draw the background
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        setPreferredSize(new Dimension(0, 60)); // Reduced preferred height for the header

        // Center: Application Title
        JLabel appTitle = new JLabel(""); // Removed "Application" text
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        appTitle.setForeground(Color.WHITE); // Changed to white for better contrast on dark gradient
        appTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(appTitle, BorderLayout.CENTER);

        // Right: Hamburger Menu
        menuButton = new JButton(new ImageIcon(createScaledIcon("src/doclink/resources/menu_icon.png", 24, 24))); // Placeholder icon
        menuButton.setBorderPainted(false);
        menuButton.setContentAreaFilled(false);
        menuButton.setFocusPainted(false);
        menuButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        menuButton.setPreferredSize(new Dimension(40, 40));
        add(menuButton, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        GradientPaint gp = new GradientPaint(0, 0, DARK_NAVY, getWidth(), 0, ROYAL_BLUE); // Horizontal gradient
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    // Helper to scale icons
    private Image createScaledIcon(String path, int width, int height) {
        ImageIcon originalIcon = new ImageIcon(path);
        Image image = originalIcon.getImage();
        return image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
}