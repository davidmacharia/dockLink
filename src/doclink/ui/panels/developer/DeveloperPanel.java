package doclink.ui.panels.developer;

import doclink.Database;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.LocalDate; // Added import for LocalDate
import java.time.format.DateTimeFormatter;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;

public class DeveloperPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel; // Retain for interface compatibility

    private JTextArea syncLogArea;
    private JButton exportDataButton;
    private JButton importDataButton;
    private JButton viewInstancesButton; // Placeholder for viewing app instances

    private static final Color DARK_NAVY = new Color(26, 35, 126);
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DeveloperPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel panelTitle = new JLabel("Developer - Database Sync Management");
        panelTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panelTitle.setForeground(DARK_NAVY);
        panelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(panelTitle, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel sectionTitle = new JLabel("Sync Operations:");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sectionTitle.setForeground(DARK_NAVY);
        contentPanel.add(sectionTitle, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setOpaque(false);

        exportDataButton = createStyledButton("Export All Data", new Color(40, 167, 69)); // Green
        exportDataButton.addActionListener(e -> exportAllData());
        buttonPanel.add(exportDataButton);

        importDataButton = createStyledButton("Import All Data", new Color(0, 123, 255)); // Blue
        importDataButton.addActionListener(e -> importAllData());
        buttonPanel.add(importDataButton);

        viewInstancesButton = createStyledButton("View App Instances", new Color(108, 117, 125)); // Grey
        viewInstancesButton.addActionListener(e -> viewAppInstances());
        buttonPanel.add(viewInstancesButton);

        gbc.gridy++;
        contentPanel.add(buttonPanel, gbc);

        // Sync Log Area
        gbc.gridy++;
        JLabel logTitle = new JLabel("Sync Log:");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logTitle.setForeground(DARK_NAVY);
        contentPanel.add(logTitle, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0; // Allow log area to expand vertically
        gbc.fill = GridBagConstraints.BOTH;
        syncLogArea = new JTextArea(15, 60);
        syncLogArea.setEditable(false);
        syncLogArea.setLineWrap(true);
        syncLogArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(syncLogArea);
        contentPanel.add(scrollPane, gbc);

        add(contentPanel, BorderLayout.CENTER);

        logMessage("Developer Panel initialized.");
        refreshData(); // Initial data load for cards
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private void logMessage(String message) {
        syncLogArea.append(LocalDateTime.now().format(LOG_FORMATTER) + " - " + message + "\n");
        syncLogArea.setCaretPosition(syncLogArea.getDocument().getLength()); // Scroll to bottom
    }

    private void exportAllData() {
        logMessage("Initiating data export...");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Exported Data");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));
        fileChooser.setSelectedFile(new File("doclink_export_" + LocalDate.now() + ".json"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".json")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".json");
            }
            
            // --- Placeholder for actual export logic ---
            // In a real implementation, you would read data from all tables (users, plans, documents, billing, logs, meetings, document_checklist_items)
            // and write it to the selected file in a structured format (e.g., JSON).
            // This would involve iterating through ResultSet objects for each table and building a data structure.
            // For now, we'll just simulate success.
            logMessage("Simulating export to: " + fileToSave.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Data export simulated successfully to " + fileToSave.getName() + "!", "Export Success", JOptionPane.INFORMATION_MESSAGE);
            logMessage("Data export completed.");
        } else {
            logMessage("Data export cancelled.");
        }
    }

    private void importAllData() {
        logMessage("Initiating data import...");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Data File to Import");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();
            
            // --- Placeholder for actual import logic ---
            // In a real implementation, you would read data from the selected file,
            // parse it (e.g., from JSON), and then insert/update records in your database.
            // This would require careful handling of primary keys, foreign key constraints,
            // and conflict resolution strategies (e.g., overwrite, skip, merge).
            // For now, we'll just simulate success.
            logMessage("Simulating import from: " + fileToLoad.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Data import simulated successfully from " + fileToLoad.getName() + "!", "Import Success", JOptionPane.INFORMATION_MESSAGE);
            logMessage("Data import completed. (Note: Actual data changes would require a rebuild/restart to reflect in UI if not dynamically loaded)");
            // After a real import, you might want to refresh all panels or restart the app.
            parentDashboard.revalidate();
            parentDashboard.repaint();
            // For a full refresh, a restart might be needed depending on how data is cached.
        } else {
            logMessage("Data import cancelled.");
        }
    }

    private void viewAppInstances() {
        logMessage("Attempting to view app instances...");
        // --- Placeholder for actual instance discovery logic ---
        // This is highly dependent on how "app instances" are defined and how they communicate.
        // For a local desktop app, this might involve:
        // 1. Scanning for other running instances on the local machine (complex).
        // 2. Connecting to a central registry/server that tracks active instances.
        // 3. Simply listing known database files if each instance uses a separate file.
        // For now, we'll provide a conceptual message.
        String instancesInfo = "Currently, DocLink operates with local SQLite databases.\n" +
                               "To 'sync' between instances, you would typically export data from one instance\n" +
                               "and import it into another. Future enhancements could involve:\n" +
                               "- A central server for shared data.\n" +
                               "- Peer-to-peer data exchange mechanisms.\n" +
                               "- Automated backup/restore points.";
        JOptionPane.showMessageDialog(this, instancesInfo, "App Instances Overview", JOptionPane.INFORMATION_MESSAGE);
        logMessage("App instances overview displayed.");
    }

    @Override
    public void refreshData() {
        // The Developer panel should not update the main dashboard cards with business metrics.
        // Its refreshData method can be used for internal logging or state updates relevant to developer tools.
        logMessage("Developer Panel data refreshed (no business metrics updated).");
    }
}