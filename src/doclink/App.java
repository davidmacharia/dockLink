
            package doclink;
            import java.awt.*;
            import javax.swing.*;

            class AppFrame {
                JFrame frame;
                JPanel companyLogo, form;
                boolean showingLogin = true;

                public AppFrame() {
                    frame = new JFrame();
                    companyLogo = new JPanel();
                    form = new JPanel();
                }

                public void showFrame(String title) {
                    frame.getContentPane().setBackground(new Color(230, 235, 245));
                    frame.setLayout(new GridBagLayout());
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setTitle(title);
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    gbc.weightx = 0.5;
                    gbc.weighty = 1.0;
                    gbc.fill = GridBagConstraints.BOTH;
                    gbc.anchor = GridBagConstraints.CENTER;
                    companyLogo.setPreferredSize(new Dimension(400, 400));
                    frame.add(companyLogo, gbc);
                    gbc.gridx = 1;
                    form.setPreferredSize(new Dimension(500, 400));
                    frame.add(form, gbc);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                }

                public void showLogin() {
                    form.removeAll();
                    form.setLayout(new GridBagLayout());
                    form.setBackground(new Color(245, 245, 250));
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.insets = new Insets(20, 20, 20, 20);
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    gbc.gridwidth = 2;
                    gbc.anchor = GridBagConstraints.CENTER;
                    JPanel top = new JPanel();
                    top.setBackground(new Color(220, 240, 255));
                    top.setPreferredSize(new Dimension(400, 60));
                    JLabel loginLabel = new JLabel("LOGIN");
                    loginLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
                    top.add(loginLabel);
                    form.add(top, gbc);

                    gbc.gridy++;
                    gbc.gridwidth = 1;
                    gbc.anchor = GridBagConstraints.LINE_END;
                    JLabel emailLabel = new JLabel("Email:");
                    emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    form.add(emailLabel, gbc);

                    gbc.gridx = 1;
                    gbc.anchor = GridBagConstraints.LINE_START;
                    JTextField email = new JTextField(15);
                    email.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    email.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(180, 180, 200), 1),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
                    email.setPreferredSize(new Dimension(200, 35));
                    form.add(email, gbc);

                    gbc.gridx = 0;
                    gbc.gridy++;
                    gbc.anchor = GridBagConstraints.LINE_END;
                    JLabel passwordLabel = new JLabel("Password:");
                    passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    form.add(passwordLabel, gbc);

                    gbc.gridx = 1;
                    gbc.anchor = GridBagConstraints.LINE_START;
                    JPasswordField password = new JPasswordField(15);
                    password.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    password.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(180, 180, 200), 1),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
                    password.setPreferredSize(new Dimension(200, 35));
                    form.add(password, gbc);

                    gbc.gridx = 0;
                    gbc.gridy++;
                    gbc.gridwidth = 2;
                    gbc.anchor = GridBagConstraints.CENTER;
                    JButton button = new JButton("Login");
                    button.setBackground(new Color(0, 120, 215));
                    button.setForeground(Color.WHITE);
                    button.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    button.setFocusPainted(false);
                    button.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
                    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    button.setPreferredSize(new Dimension(150, 40));
                    form.add(button, gbc);

                    gbc.gridy++;
                    JLabel registerLabel = new JLabel("No account? ");
                    JButton registerBtn = new JButton("Register");
                    registerBtn.setBorderPainted(false);
                    registerBtn.setForeground(new Color(0, 120, 215));
                    registerBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    registerBtn.setContentAreaFilled(false);
                    JPanel linkPanel = new JPanel();
                    linkPanel.setBackground(new Color(245, 245, 250));
                    linkPanel.add(registerLabel);
                    linkPanel.add(registerBtn);
                    gbc.anchor = GridBagConstraints.CENTER;
                    form.add(linkPanel, gbc);

                    registerBtn.addActionListener(e -> {
                        showingLogin = false;
                        showRegister();
                        form.revalidate();
                        form.repaint();
                    });
                }

                public void showRegister() {
                    form.removeAll();
                    form.setLayout(new GridBagLayout());
                    form.setBackground(new Color(245, 245, 250));
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.insets = new Insets(20, 20, 20, 20);
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    gbc.gridwidth = 2;
                    gbc.anchor = GridBagConstraints.CENTER;
                    JPanel top = new JPanel();
                    top.setBackground(new Color(220, 240, 255));
                    top.setPreferredSize(new Dimension(400, 60));
                    JLabel regLabel = new JLabel("REGISTER");
                    regLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
                    top.add(regLabel);
                    form.add(top, gbc);

                    gbc.gridy++;
                    gbc.gridwidth = 1;
                    gbc.anchor = GridBagConstraints.LINE_END;
                    JLabel nameLabel = new JLabel("Name:");
                    nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    form.add(nameLabel, gbc);

                    gbc.gridx = 1;
                    gbc.anchor = GridBagConstraints.LINE_START;
                    JTextField nameField = new JTextField(15);
                    nameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    nameField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(180, 180, 200), 1),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
                    nameField.setPreferredSize(new Dimension(200, 35));
                    form.add(nameField, gbc);

                    gbc.gridx = 0;
                    gbc.gridy++;
                    gbc.anchor = GridBagConstraints.LINE_END;
                    JLabel emailLabel = new JLabel("Email:");
                    emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    form.add(emailLabel, gbc);

                    gbc.gridx = 1;
                    gbc.anchor = GridBagConstraints.LINE_START;
                    JTextField emailField = new JTextField(15);
                    emailField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    emailField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(180, 180, 200), 1),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
                    emailField.setPreferredSize(new Dimension(200, 35));
                    form.add(emailField, gbc);

                    gbc.gridx = 0;
                    gbc.gridy++;
                    gbc.anchor = GridBagConstraints.LINE_END;
                    JLabel passwordLabel = new JLabel("Password:");
                    passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    form.add(passwordLabel, gbc);

                    gbc.gridx = 1;
                    gbc.anchor = GridBagConstraints.LINE_START;
                    JPasswordField passwordField = new JPasswordField(15);
                    passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    passwordField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(180, 180, 200), 1),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
                    passwordField.setPreferredSize(new Dimension(200, 35));
                    form.add(passwordField, gbc);

                    gbc.gridx = 0;
                    gbc.gridy++;
                    gbc.gridwidth = 2;
                    gbc.anchor = GridBagConstraints.CENTER;
                    JButton regButton = new JButton("Register");
                    regButton.setBackground(new Color(0, 120, 215));
                    regButton.setForeground(Color.WHITE);
                    regButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    regButton.setFocusPainted(false);
                    regButton.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
                    regButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    regButton.setPreferredSize(new Dimension(150, 40));
                    form.add(regButton, gbc);

                    gbc.gridy++;
                    JLabel loginLabel = new JLabel("Already have an account? ");
                    JButton loginBtn = new JButton("Login");
                    loginBtn.setBorderPainted(false);
                    loginBtn.setForeground(new Color(0, 120, 215));
                    loginBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    loginBtn.setContentAreaFilled(false);
                    JPanel linkPanel = new JPanel();
                    linkPanel.setBackground(new Color(245, 245, 250));
                    linkPanel.add(loginLabel);
                    linkPanel.add(loginBtn);
                    gbc.anchor = GridBagConstraints.CENTER;
                    form.add(linkPanel, gbc);

                    loginBtn.addActionListener(e -> {
                        showingLogin = true;
                        showLogin();
                        form.revalidate();
                        form.repaint();
                    });
                }

                public void showLogo() {
                    companyLogo.setBackground(new Color(0, 120, 215));
                    companyLogo.setLayout(new GridBagLayout());
                    JPanel logoPanel = new JPanel();
                    logoPanel.setBackground(new Color(0, 120, 215));
                    logoPanel.setLayout(new BorderLayout());

                    // Load icon.jpg from the provided absolute path
                    String iconPath = "C:\\Users\\Macharia\\Desktop\\docLink\\src\\doclink\\icon.jpg";
                    java.io.File iconFile = new java.io.File(iconPath);
                    if (iconFile.exists()) {
                        ImageIcon icon = new ImageIcon(iconPath);
                        Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                        JLabel imgLabel = new JLabel(new ImageIcon(img));
                        imgLabel.setHorizontalAlignment(JLabel.CENTER);
                        logoPanel.add(imgLabel, BorderLayout.NORTH);
                    } else {
                        JLabel errorLabel = new JLabel("[icon.jpg not found]");
                        errorLabel.setForeground(Color.WHITE);
                        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
                        errorLabel.setHorizontalAlignment(JLabel.CENTER);
                        logoPanel.add(errorLabel, BorderLayout.NORTH);
                    }

                    JLabel logo = new JLabel();
                    logo.setText("<html><center><b>DocLink</b><br><span style='font-size:12px;'>Connect to documents, Our Priority</span></center></html>");
                    logo.setForeground(Color.WHITE);
                    logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
                    logo.setPreferredSize(new Dimension(350, 120));
                    logo.setHorizontalAlignment(JLabel.CENTER);
                    logoPanel.add(logo, BorderLayout.CENTER);
                    companyLogo.add(logoPanel);
                }
            }

            public class App {
                public static void main(String[] args) {
                    AppFrame app = new AppFrame();
                    app.showLogo();
                    app.showLogin();
                    app.showFrame("DocLink");
                }
            }