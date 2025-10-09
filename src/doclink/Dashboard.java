package doclink;


import javax.swing.*;
import java.awt.*;

public class Dashboard extends AppFrame {
    private final String email;
    private final String password;

    public Dashboard(String email, String password) {
        super();
        this.email = email;
        this.password = password;
    }

    public void showDashboard() {
        // Clear frame and prepare layout
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(240, 245, 255));

        // === TOP BAR ===
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(70, 100, 180));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel appTitle = new JLabel("DocLink Dashboard");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        appTitle.setForeground(Color.WHITE);

        JButton logout = new JButton("Logout");
        logout.setFocusPainted(false);
        logout.setBackground(new Color(230, 70, 70));
        logout.setForeground(Color.WHITE);
        logout.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logout.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        logout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "Are you sure you want to logout?",
                    "Logout Confirmation",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                frame.dispose();
                AppFrame app = new AppFrame();
                app.showFrame("DocLink Login");
                app.showLogo();
                app.showLogin();
            }
        });

        topBar.add(appTitle, BorderLayout.WEST);
        topBar.add(logout, BorderLayout.EAST);

        // === CENTER CONTENT ===
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(240, 245, 255));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));

        JLabel welcome = new JLabel("Welcome, " + email + "!");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel info = new JLabel("You are now logged into DocLink.");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        info.setForeground(Color.DARK_GRAY);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(welcome);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(info);
        centerPanel.add(Box.createVerticalStrut(30));

        // === ACTION BUTTONS (placeholders for future features) ===
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(240, 245, 255));
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));

        JButton profileBtn = new JButton("Profile");
        JButton documentsBtn = new JButton("Documents");
        JButton settingsBtn = new JButton("Settings");

        for (JButton btn : new JButton[]{profileBtn, documentsBtn, settingsBtn}) {
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setBackground(new Color(220, 230, 250));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            buttonPanel.add(btn);
        }

        centerPanel.add(buttonPanel);

        // === ADD ALL TO FRAME ===
        frame.add(topBar, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);

        // Refresh frame
        frame.revalidate();
        frame.repaint();
    }
}
