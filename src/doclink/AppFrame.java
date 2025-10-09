package doclink;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AppFrame {
    protected final JFrame frame;
    protected final JPanel companyLogo;
    protected final JPanel form;

    public AppFrame() {
        frame = new JFrame();
        companyLogo = new JPanel();
        form = new JPanel();
    }

    // --- Main frame setup ---
    public void showFrame(String title) {
        frame.setTitle(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.getContentPane().setBackground(new Color(230, 235, 245));
        frame.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        // Left: Logo panel
        gbc.gridx = 0;
        gbc.weightx = 0.4;
        companyLogo.setPreferredSize(new Dimension(400, 400));
        frame.add(companyLogo, gbc);

        // Right: Form panel
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        form.setPreferredSize(new Dimension(500, 400));
        frame.add(form, gbc);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // --- Show login form ---
    public void showLogin() {
        form.removeAll();
        form.setLayout(new GridBagLayout());
        form.setBackground(new Color(240,240,240)); 

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 20, 15, 20);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        // Title
        JLabel loginLabel = new JLabel("LOGIN", SwingConstants.CENTER);
        loginLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        loginLabel.setForeground(Color.BLUE); 
        form.add(loginLabel, gbc);

        // --- Email Field ---
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel emailLabel = label("Email:");
        emailLabel.setForeground(Color.WHITE);
        form.add(emailLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        JTextField emailField = inputField();
        form.add(emailField, gbc);

        // --- Password Field ---
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel passLabel = label("Password:");
        passLabel.setForeground(Color.WHITE);
        form.add(passLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        JPasswordField passwordField = passwordField();
        form.add(passwordField, gbc);

        // --- Login Button ---
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton loginButton = button("Login");
        loginButton.setBackground(Color.WHITE);
        loginButton.setForeground(new Color(0, 120, 215)); // Blue text on white button

        loginButton.addActionListener(new LoginHandler(emailField, passwordField));
        form.add(loginButton, gbc);

        form.revalidate();
        form.repaint();
    }

    // --- Logo setup ---
    public void showLogo() {
        companyLogo.removeAll();
        companyLogo.setBackground(new Color(0, 120, 215));
        companyLogo.setLayout(new GridBagLayout());

        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(new Color(0, 120, 215));

        // ✅ Use absolute path (your case)
        String iconPath = "C:/Users/Macharia/dockLink/src/doclink/icon.jpg";
        File iconFile = new File(iconPath);

        JLabel iconLabel;
        if (iconFile.exists()) {
            ImageIcon icon = new ImageIcon(iconPath);
            Image scaled = icon.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
            iconLabel = new JLabel(new ImageIcon(scaled), JLabel.CENTER);
        } else {
            iconLabel = new JLabel("[icon.jpg not found]", JLabel.CENTER);
            iconLabel.setForeground(Color.WHITE);
            iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        }

        logoPanel.add(iconLabel, BorderLayout.NORTH);

        JLabel text = new JLabel(
                "<html><center><b>DocLink</b><br><span style='font-size:13px;'>Connect to documents, our priority</span></center></html>",
                JLabel.CENTER
        );
        text.setForeground(Color.WHITE);
        text.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logoPanel.add(text, BorderLayout.CENTER);

        companyLogo.add(logoPanel);
        companyLogo.revalidate();
        companyLogo.repaint();
    }

    // --- Helper methods ---
    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        return label;
    }

    private JTextField inputField() {
        JTextField field = new JTextField(15);
        styleField(field);
        return field;
    }

    private JPasswordField passwordField() {
        JPasswordField field = new JPasswordField(15);
        styleField(field);
        return field;
    }

    private JButton button(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(0, 120, 215));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        field.setBackground(new Color(250, 250, 250));
        field.setForeground(new Color(0, 50, 100));
        field.setPreferredSize(new Dimension(220, 35));
    }
}
