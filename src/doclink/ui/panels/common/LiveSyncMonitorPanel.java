package doclink.ui.panels.common;

import doclink.sync.SyncConfigManager;
import doclink.ui.Dashboard;
import doclink.AppConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class LiveSyncMonitorPanel extends JPanel implements Dashboard.Refreshable {
    private SyncConfigManager syncConfigManager;
    private Consumer<String> logConsumer; // For logging messages to the UI
    private JTextArea logOutputArea;
    private JLabel lastSyncTimeLabel;
    private JButton forceSyncButton; // Existing button for all sync
    private JButton forceCentralToLocalSyncButton; // New button
    private JButton clearLogButton; // New button
    private JButton startAllSyncButton; // New button

    private static final Color DARK_NAVY = new Color(26, 35, 126);
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LiveSyncMonitorPanel(SyncConfigManager syncConfigManager, Consumer<String> logConsumer) {
        this.syncConfigManager = syncConfigManager;
        this.logConsumer = logConsumer; // This consumer will append to the DeveloperPanel's log area

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Live Synchronization Monitor");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(DARK_NAVY);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0; // Allow components to expand horizontally

        gbc.gridx = 0;
        gbc.gridy = 0;
        contentPanel.add(new JLabel("Last Central Pull:"), gbc);
        gbc.gridx = 1;
        lastSyncTimeLabel = new JLabel("N/A");
        lastSyncTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(lastSyncTimeLabel, gbc);

        // Panel for action buttons
        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actionButtonsPanel.setOpaque(false);

        // Existing Force Sync All button
        forceSyncButton = createStyledButton("Force All Sync Now", new Color(0, 123, 255));
        forceSyncButton.addActionListener(e -> forceSync());
        actionButtonsPanel.add(forceSyncButton);

        // New: Force Pull from Central DB button
        forceCentralToLocalSyncButton = createStyledButton("Force Pull from Central DB", new Color(0, 150, 200));
        forceCentralToLocalSyncButton.addActionListener(e -> forceCentralToLocalSync());
        actionButtonsPanel.add(forceCentralToLocalSyncButton);

        // New: Clear Activity Log button
        clearLogButton = createStyledButton("Clear Activity Log", new Color(108, 117, 125));
        clearLogButton.addActionListener(e -> clearActivityLog());
        actionButtonsPanel.add(clearLogButton);

        // New: Start All Sync Services button
        startAllSyncButton = createStyledButton("Start All Sync Services", new Color(40, 167, 69));
        startAllSyncButton.addActionListener(e -> startAllSyncServices());
        actionButtonsPanel.add(startAllSyncButton);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2; // Span across two columns
        contentPanel.add(actionButtonsPanel, gbc);

        gbc.gridy++;
        contentPanel.add(new JSeparator(), gbc);

        gbc.gridy++;
        JLabel logTitle = new JLabel("Real-time Sync Log:");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logTitle.setForeground(DARK_NAVY);
        contentPanel.add(logTitle, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0; // Allow vertical expansion
        gbc.fill = GridBagConstraints.BOTH; // Fill both horizontally and vertically
        logOutputArea = new JTextArea();
        logOutputArea.setEditable(false);
        logOutputArea.setLineWrap(true);
        logOutputArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(logOutputArea);
        contentPanel.add(scrollPane, gbc);

        add(contentPanel, BorderLayout.CENTER);

        // Initial refresh
        refreshData();
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

    private void appendToLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logOutputArea.append(LocalDateTime.now().format(DISPLAY_DATE_FORMATTER) + " - " + message + "\n");
            logOutputArea.setCaretPosition(logOutputArea.getDocument().getLength());
        });
        // Also pass to the parent log consumer (DeveloperPanel's log area)
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
    }

    private void forceSync() {
        appendToLog("User initiated force sync (all services).");
        syncConfigManager.forceSyncNow();
        refreshData(); // Update display after sync attempt
    }

    private void forceCentralToLocalSync() {
        appendToLog("User initiated force pull from Central DB.");
        syncConfigManager.performCentralToLocalDbSync();
        refreshData(); // Update display after sync attempt
    }

    private void clearActivityLog() {
        logOutputArea.setText("");
        appendToLog("Activity log cleared by user.");
    }

    private void startAllSyncServices() {
        appendToLog("User initiated start of all sync services.");
        syncConfigManager.startSyncServices();
        refreshData(); // Update display after starting services
    }

    @Override
    public void refreshData() {
        // Update last sync time from AppConfig
        String lastPullTimestamp = AppConfig.getProperty(AppConfig.LAST_CENTRAL_PULL_TIMESTAMP_KEY);
        if (lastPullTimestamp != null && !lastPullTimestamp.isEmpty()) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(lastPullTimestamp);
                lastSyncTimeLabel.setText(dateTime.format(DISPLAY_DATE_FORMATTER));
            } catch (Exception e) {
                lastSyncTimeLabel.setText("Invalid Timestamp");
                appendToLog("Error parsing last central pull timestamp: " + e.getMessage());
            }
        } else {
            lastSyncTimeLabel.setText("Never");
        }
        appendToLog("Live Sync Monitor data refreshed.");
    }
}