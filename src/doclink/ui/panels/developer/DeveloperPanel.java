package doclink.ui.panels.developer;

import doclink.AppConfig;
import doclink.Database;
import doclink.communication.CommunicationManager;
import doclink.models.MessageLog;
import doclink.models.MessageTemplate;
import doclink.models.Peer;
import doclink.models.SystemUpdate;
import doclink.models.User;
import doclink.sync.SyncConfigManager;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.filechooser.FileNameExtensionFilter;

public class DeveloperPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private JTextArea syncLogArea;
    private JButton exportDataButton;
    private JButton importDataButton;
    private JButton viewInstancesButton;

    private SyncConfigManager syncConfigManager;
    private CommunicationManager communicationManager;

    private JComboBox<String> syncRoleComboBox;
    private JComboBox<String> hybridSyncModeComboBox;
    private JButton saveSyncConfigButton;

    private JTable peerTable;
    private DefaultTableModel peerTableModel;
    private JTextField peerIpField, peerPortField;
    private JCheckBox peerTrustedCheckBox;
    private JButton addPeerButton, removePeerButton, discoverPeersButton;
    private Peer selectedPeer;

    private JSpinner syncIntervalSpinner;
    private JComboBox<String> conflictStrategyComboBox;
    private JSpinner changelogRetentionSpinner;
    private JCheckBox autoSyncEnabledCheckBox;
    private JCheckBox compressionEnabledCheckBox;
    private JCheckBox encryptionEnabledCheckBox;
    private JCheckBox deleteLocalOnCentralPullCheckBox; // NEW: Checkbox for deleteLocalOnCentralPull
    
    // NEW: Separate field for Central API URL
    private JTextField centralApiUrlInputField; 
    private JTextField centralApiAuthTokenField;
    private JButton testCentralApiConnectionButton;
    private JButton saveSyncSettingsButton;

    private JTextArea liveSyncMonitorArea;
    private JButton forceSyncNowButton;
    private JButton forceLocalToCentralDbSyncButton; // NEW: Button for local-to-central DB sync

    // System Update Broadcast components
    private JTextField updateVersionField, updateTitleField;
    private JTextArea updateMessageArea;
    private JComboBox<String> updateTargetRoleComboBox; 
    private JButton addNewBroadcastButton; 
    private JButton updateBroadcastButton; 
    private JButton deleteBroadcastButton; 
    private JButton clearBroadcastFormButton; 
    private JTable systemUpdateLogTable;
    private DefaultTableModel systemUpdateLogTableModel;
    private SystemUpdate selectedSystemUpdate; 

    private JTextField smtpHostField, smtpPortField, smtpUsernameField, senderEmailField;
    private JPasswordField smtpPasswordField; 
    private JTextField smsApiKeyField, smsSenderIdField;
    private JCheckBox autoNotificationsEnabledCheckBox;
    private JButton saveCommSettingsButton;
    private JTable messageTemplatesTable;
    private DefaultTableModel messageTemplatesTableModel;
    private JTextArea templateBodyArea;
    private JTextField templateSubjectField;
    private JComboBox<String> templateTypeComboBox;
    private JButton updateTemplateButton;
    private JButton deleteTemplateButton; 
    private MessageTemplate selectedTemplate;
    private JTable messageLogTable;
    private DefaultTableModel messageLogTableModel;

    // RENAMED: Field for Central DB URL
    private JTextField centralDbUrlInputField; 
    private JLabel currentConfiguredUrlLabel;
    private JButton saveDbConfigButton;
    private JButton testDbConnectionButton;
    private JButton clearDbConfigButton;


    private static final Color DARK_NAVY = new Color(26, 35, 126);
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    public DeveloperPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 247, 250));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Initialize managers
        this.syncConfigManager = new SyncConfigManager(this::logMessage);
        this.communicationManager = new CommunicationManager(this::logMessage);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(new Color(200, 220, 255)); 
        tabbedPane.setForeground(DARK_NAVY);
        tabbedPane.setPreferredSize(new Dimension(800, 600)); 

        tabbedPane.addTab("Sync Configuration", createSyncConfigPanel());
        tabbedPane.addTab("Peer Management", createPeerManagementPanel());
        tabbedPane.addTab("Sync Settings", createSyncSettingsPanel());
        tabbedPane.addTab("Live Sync Monitor", createLiveSyncMonitorPanel());
        tabbedPane.addTab("System Update Broadcast", createSystemUpdateBroadcastPanel());
        tabbedPane.addTab("Communication Settings", createCommunicationSettingsPanel());
        tabbedPane.addTab("Central DB Config", createCentralDbConfigPanel());
        tabbedPane.addTab("Data Export/Import", createDataExportImportPanel());

        add(tabbedPane, BorderLayout.CENTER); 

        logMessage("Developer Panel initialized.");
        refreshData(); 
        syncConfigManager.startSyncServices(); 
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
        if (syncLogArea != null) {
            syncLogArea.append(LocalDateTime.now().format(LOG_FORMATTER) + " - " + message + "\n");
            syncLogArea.setCaretPosition(syncLogArea.getDocument().getLength()); 
        }
        if (liveSyncMonitorArea != null) {
            liveSyncMonitorArea.append(LocalDateTime.now().format(LOG_FORMATTER) + " - " + message + "\n");
            liveSyncMonitorArea.setCaretPosition(liveSyncMonitorArea.getDocument().getLength());
        }
    }

    // --- Tab 1: Sync Role Configuration ---
    private JPanel createSyncConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE); 
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel title = new JLabel("Sync Role & Mode Configuration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(DARK_NAVY);
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        syncRoleComboBox = new JComboBox<>(new String[]{"SERVER", "CLIENT", "BOTH"});
        panel.add(new JLabel("Sync Role:"), gbc);
        gbc.gridx = 1;
        panel.add(syncRoleComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        hybridSyncModeComboBox = new JComboBox<>(new String[]{"P2P_ONLY", "CENTRAL_API_ONLY", "HYBRID"});
        panel.add(new JLabel("Hybrid Sync Mode:"), gbc);
        gbc.gridx = 1;
        panel.add(hybridSyncModeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        saveSyncConfigButton = createStyledButton("Save Sync Configuration", new Color(40, 167, 69));
        saveSyncConfigButton.addActionListener(e -> saveSyncConfig());
        panel.add(saveSyncConfigButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        loadSyncConfig();
        return panel;
    }

    private void loadSyncConfig() {
        syncRoleComboBox.setSelectedItem(syncConfigManager.getCurrentSyncRole().name());
        hybridSyncModeComboBox.setSelectedItem(syncConfigManager.getCurrentHybridSyncMode().name());
    }

    private void saveSyncConfig() {
        syncConfigManager.setSyncRole(SyncConfigManager.SyncRole.valueOf((String) syncRoleComboBox.getSelectedItem()));
        syncConfigManager.setHybridSyncMode(SyncConfigManager.HybridSyncMode.valueOf((String) hybridSyncModeComboBox.getSelectedItem()));
        syncConfigManager.saveSettings();
        JOptionPane.showMessageDialog(this, "Sync configuration saved and services restarted.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Tab 2: Peer Management ---
    private JPanel createPeerManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE); 
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top: Add Peer Form
        JPanel addPeerPanel = new JPanel(new GridBagLayout());
        addPeerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Add/Manage Peers");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(DARK_NAVY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        addPeerPanel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0; addPeerPanel.add(new JLabel("IP Address:"), gbc);
        gbc.gridx = 1; peerIpField = new JTextField(15); addPeerPanel.add(peerIpField, gbc);
        gbc.gridx = 2; addPeerPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 3; peerPortField = new JTextField(5); addPeerPanel.add(peerPortField, gbc);

        gbc.gridy++;
        gbc.gridx = 0; addPeerPanel.add(new JLabel("Trusted:"), gbc);
        gbc.gridx = 1; peerTrustedCheckBox = new JCheckBox(); peerTrustedCheckBox.setOpaque(false); addPeerPanel.add(peerTrustedCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 2;
        addPeerButton = createStyledButton("Add Peer", new Color(40, 167, 69));
        addPeerButton.addActionListener(e -> addPeer());
        addPeerPanel.add(addPeerButton, gbc);

        gbc.gridx = 2; gbc.gridwidth = 2;
        removePeerButton = createStyledButton("Remove Selected Peer", new Color(220, 53, 69));
        removePeerButton.addActionListener(e -> removePeer());
        removePeerButton.setEnabled(false);
        addPeerPanel.add(removePeerButton, gbc);

        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 4;
        discoverPeersButton = createStyledButton("Discover Peers (UDP Broadcast)", new Color(0, 123, 255));
        discoverPeersButton.addActionListener(e -> syncConfigManager.discoverPeers());
        addPeerPanel.add(discoverPeersButton, gbc);

        panel.add(addPeerPanel, BorderLayout.NORTH);

        // Center: Peer Table
        String[] columnNames = {"ID", "IP Address", "Port", "Last Sync", "Trusted", "Status"};
        peerTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 4) return Boolean.class;
                return super.getColumnClass(column);
            }
        };
        peerTable = new JTable(peerTableModel);
        peerTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        peerTable.setRowHeight(20);
        peerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        peerTable.getTableHeader().setBackground(new Color(230, 230, 230));
        peerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && peerTable.getSelectedRow() != -1) {
                removePeerButton.setEnabled(true);
                int peerId = (int) peerTable.getValueAt(peerTable.getSelectedRow(), 0);
                selectedPeer = Database.getAllPeers().stream().filter(p -> p.getId() == peerId).findFirst().orElse(null);
            } else {
                removePeerButton.setEnabled(false);
                selectedPeer = null;
            }
        });
        // Hide ID column
        TableColumn idColumn = peerTable.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0); idColumn.setMaxWidth(0); idColumn.setPreferredWidth(0); idColumn.setResizable(false);

        panel.add(new JScrollPane(peerTable), BorderLayout.CENTER);

        loadPeerData();
        return panel;
    }

    private void loadPeerData() {
        peerTableModel.setRowCount(0);
        List<Peer> peers = syncConfigManager.getKnownPeers();
        for (Peer peer : peers) {
            peerTableModel.addRow(new Object[]{
                peer.getId(),
                peer.getIpAddress(),
                peer.getPort(),
                peer.getLastSyncTime() != null ? peer.getLastSyncTime().format(TABLE_DATE_FORMATTER) : "N/A",
                peer.isTrusted(),
                peer.getStatus()
            });
        }
    }

    private void addPeer() {
        String ip = peerIpField.getText().trim();
        String portStr = peerPortField.getText().trim();
        boolean trusted = peerTrustedCheckBox.isSelected();

        if (ip.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "IP Address and Port cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) throw new NumberFormatException();
            if (syncConfigManager.addPeer(ip, port, trusted)) {
                JOptionPane.showMessageDialog(this, "Peer added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                peerIpField.setText("");
                peerPortField.setText("");
                peerTrustedCheckBox.setSelected(false);
                loadPeerData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add peer. It might already exist or invalid input.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number. Must be between 1 and 65535.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removePeer() {
        if (selectedPeer == null) {
            JOptionPane.showMessageDialog(this, "Please select a peer to remove.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove peer " + selectedPeer.getIpAddress() + ":" + selectedPeer.getPort() + "?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (syncConfigManager.removePeer(selectedPeer.getId())) {
                JOptionPane.showMessageDialog(this, "Peer removed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadPeerData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to remove peer.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- Tab 3: Sync Settings ---
    private JPanel createSyncSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE); 
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel title = new JLabel("Advanced Sync Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(DARK_NAVY);
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Sync Interval (minutes):"), gbc);
        gbc.gridx = 1;
        syncIntervalSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
        panel.add(syncIntervalSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Conflict Resolution:"), gbc);
        gbc.gridx = 1;
        conflictStrategyComboBox = new JComboBox<>(new String[]{"LAST_WRITE_WINS", "SERVER_WINS", "CLIENT_WINS"});
        panel.add(conflictStrategyComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Changelog Retention (days):"), gbc);
        gbc.gridx = 1;
        changelogRetentionSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 365, 1));
        panel.add(changelogRetentionSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Enable Auto-Sync:"), gbc);
        gbc.gridx = 1;
        autoSyncEnabledCheckBox = new JCheckBox();
        autoSyncEnabledCheckBox.setOpaque(false);
        panel.add(autoSyncEnabledCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Enable Compression (GZIP):"), gbc);
        gbc.gridx = 1;
        compressionEnabledCheckBox = new JCheckBox();
        compressionEnabledCheckBox.setOpaque(false);
        panel.add(compressionEnabledCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Enable Encryption (AES/TLS):"), gbc);
        gbc.gridx = 1;
        encryptionEnabledCheckBox = new JCheckBox();
        encryptionEnabledCheckBox.setOpaque(false);
        panel.add(encryptionEnabledCheckBox, gbc);

        // NEW: Delete Local on Central Pull Checkbox
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        deleteLocalOnCentralPullCheckBox = new JCheckBox("Delete Local Data on Central Pull (if not in central)");
        deleteLocalOnCentralPullCheckBox.setOpaque(false);
        panel.add(deleteLocalOnCentralPullCheckBox, gbc);

        // NEW: Central API URL and Auth Token fields
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("--- Central API Settings (for Hybrid Sync) ---"), gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Central API URL:"), gbc);
        gbc.gridx = 1; centralApiUrlInputField = new JTextField(25); panel.add(centralApiUrlInputField, gbc);

        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Central API Auth Token:"), gbc);
        gbc.gridx = 1; centralApiAuthTokenField = new JTextField(25); panel.add(centralApiAuthTokenField, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        testCentralApiConnectionButton = createStyledButton("Test Central API Connection", new Color(0, 123, 255));
        testCentralApiConnectionButton.addActionListener(e -> syncConfigManager.testCentralApiConnection());
        panel.add(testCentralApiConnectionButton, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        saveSyncSettingsButton = createStyledButton("Save Sync Settings", new Color(40, 167, 69));
        saveSyncSettingsButton.addActionListener(e -> saveSyncSettings());
        panel.add(saveSyncSettingsButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        loadSyncSettings();
        return panel;
    }

    private void loadSyncSettings() {
        syncIntervalSpinner.setValue(syncConfigManager.getSyncIntervalMinutes());
        conflictStrategyComboBox.setSelectedItem(syncConfigManager.getConflictResolutionStrategy().name());
        changelogRetentionSpinner.setValue(syncConfigManager.getChangelogRetentionDays());
        autoSyncEnabledCheckBox.setSelected(syncConfigManager.isAutoSyncEnabled());
        compressionEnabledCheckBox.setSelected(syncConfigManager.isCompressionEnabled());
        encryptionEnabledCheckBox.setSelected(syncConfigManager.isEncryptionEnabled());
        deleteLocalOnCentralPullCheckBox.setSelected(syncConfigManager.isDeleteLocalOnCentralPullEnabled()); // NEW: Load the setting
        
        // Load Central API settings
        centralApiUrlInputField.setText(syncConfigManager.getCentralApiUrl());
        centralApiAuthTokenField.setText(AppConfig.getProperty(AppConfig.CENTRAL_API_AUTH_TOKEN_KEY, ""));
    }

    private void saveSyncSettings() {
        syncConfigManager.setSyncIntervalMinutes((Integer) syncIntervalSpinner.getValue());
        syncConfigManager.setConflictResolutionStrategy(SyncConfigManager.ConflictResolutionStrategy.valueOf((String) conflictStrategyComboBox.getSelectedItem()));
        syncConfigManager.setChangelogRetentionDays((Integer) changelogRetentionSpinner.getValue());
        syncConfigManager.setAutoSyncEnabled(autoSyncEnabledCheckBox.isSelected());
        syncConfigManager.setCompressionEnabled(compressionEnabledCheckBox.isSelected());
        syncConfigManager.setEncryptionEnabled(encryptionEnabledCheckBox.isSelected());
        syncConfigManager.setDeleteLocalOnCentralPull(deleteLocalOnCentralPullCheckBox.isSelected()); // NEW: Save the setting
        
        // Save Central API settings
        String oldCentralApiUrl = AppConfig.getProperty(AppConfig.CENTRAL_API_URL_KEY, "");
        String newCentralApiUrl = centralApiUrlInputField.getText().trim();
        
        logMessage("Attempting to save Central API URL: '" + newCentralApiUrl + "'");

        syncConfigManager.setCentralApiUrl(newCentralApiUrl);
        syncConfigManager.setCentralApiAuthToken(centralApiAuthTokenField.getText().trim());

        JOptionPane.showMessageDialog(this, "Advanced sync settings saved and services restarted if needed.", "Success", JOptionPane.INFORMATION_MESSAGE);

        // Prompt for restart if Central API URL changed
        if (!oldCentralApiUrl.equals(newCentralApiUrl)) {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "The Central API URL has changed. For these changes to take full effect, the application needs to restart. Restart now?", 
                "Restart Application", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0); // This will trigger the shutdown hook and restart the app
            }
        }
    }

    // --- Tab 4: Live Sync Monitor ---
    private JPanel createLiveSyncMonitorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE); 
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Live Sync Activity Monitor");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(DARK_NAVY);
        panel.add(title, BorderLayout.NORTH);

        liveSyncMonitorArea = new JTextArea();
        liveSyncMonitorArea.setEditable(false);
        liveSyncMonitorArea.setLineWrap(true);
        liveSyncMonitorArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(liveSyncMonitorArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        forceSyncNowButton = createStyledButton("Force All Syncs Now", new Color(0, 123, 255));
        forceSyncNowButton.addActionListener(e -> syncConfigManager.forceSyncNow());
        bottomPanel.add(forceSyncNowButton);

        // NEW: Button for local-to-central DB sync
        forceLocalToCentralDbSyncButton = createStyledButton("Force Local-to-Central DB Sync", new Color(255, 165, 0));
        forceLocalToCentralDbSyncButton.addActionListener(e -> {
            logMessage("User initiated Force Local-to-Central DB Sync.");
            syncConfigManager.performLocalToCentralDbSync();
        });
        bottomPanel.add(forceLocalToCentralDbSyncButton);

        // NEW: Clear Log Button
        JButton clearLogButton = createStyledButton("Clear Log", new Color(108, 117, 125)); 
        clearLogButton.addActionListener(e -> liveSyncMonitorArea.setText(""));
        bottomPanel.add(clearLogButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // --- Tab 5: System Update Broadcast ---
    private JPanel createSystemUpdateBroadcastPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel mainTitle = new JLabel("System Update Broadcast / Announcement");
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        mainTitle.setForeground(DARK_NAVY);
        mainTitle.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(mainTitle, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.5); 
        splitPane.setResizeWeight(0.5);
        splitPane.setOpaque(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder()); 

        splitPane.setLeftComponent(createPastBroadcastsPanel());
        splitPane.setRightComponent(createBroadcastFormPanel());

        panel.add(splitPane, BorderLayout.CENTER);

        loadSystemUpdateLog(); 
        return panel;
    }

    private JPanel createPastBroadcastsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Past Broadcasts"));

        String[] logColumnNames = {"ID", "Timestamp", "Version", "Title", "Message"};
        systemUpdateLogTableModel = new DefaultTableModel(logColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int column) {
                // Column 4 is "Message", which is a String. No need to return Boolean.class.
                // The default Object.class handling by super.getColumnClass(column) is appropriate.
                // if (column == 4) return Boolean.class; // Removed this incorrect line
                return super.getColumnClass(column);
            }
        };
        systemUpdateLogTable = new JTable(systemUpdateLogTableModel);
        systemUpdateLogTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        systemUpdateLogTable.setRowHeight(20);
        systemUpdateLogTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        systemUpdateLogTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
        systemUpdateLogTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && systemUpdateLogTable.getSelectedRow() != -1) {
                    loadSelectedBroadcastDetails();
                } else if (systemUpdateLogTable.getSelectedRow() == -1) {
                    clearBroadcastForm();
                }
            }
        });

        // Hide ID column
        TableColumn idColumn = systemUpdateLogTable.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0); idColumn.setMaxWidth(0); idColumn.setPreferredWidth(0); idColumn.setResizable(false);

        JScrollPane logScrollPane = new JScrollPane(systemUpdateLogTable);
        panel.add(logScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setOpaque(false);

        deleteBroadcastButton = createStyledButton("Delete Selected", new Color(220, 53, 69)); 
        deleteBroadcastButton.addActionListener(e -> deleteSystemUpdate());
        deleteBroadcastButton.setEnabled(false);
        buttonPanel.add(deleteBroadcastButton);

        // Edit button will trigger loading into the form, the form itself will have the 'Update' button
        JButton editButton = createStyledButton("Edit Selected", new Color(0, 123, 255)); 
        editButton.addActionListener(e -> loadSelectedBroadcastDetails()); 
        editButton.setEnabled(false); 
        buttonPanel.add(editButton);

        // Enable/disable edit/delete buttons based on table selection
        systemUpdateLogTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = systemUpdateLogTable.getSelectedRow() != -1;
                deleteBroadcastButton.setEnabled(rowSelected);
                editButton.setEnabled(rowSelected);
            }
        });

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBroadcastFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "New / Edit Broadcast"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Version:"), gbc);
        gbc.gridx = 1; updateVersionField = new JTextField(20); panel.add(updateVersionField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; updateTitleField = new JTextField(20); panel.add(updateTitleField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Message:"), gbc);
        gbc.gridx = 1;
        updateMessageArea = new JTextArea(5, 20);
        updateMessageArea.setLineWrap(true);
        updateMessageArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(updateMessageArea), gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Notify Role:"), gbc); 
        gbc.gridx = 1;
        String[] roles = {"All Users", "Reception", "Planning", "Committee", "Director", "Structural", "Client", "Admin"};
        updateTargetRoleComboBox = new JComboBox<>(roles);
        panel.add(updateTargetRoleComboBox, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setOpaque(false);

        addNewBroadcastButton = createStyledButton("Add New Broadcast", new Color(40, 167, 69)); 
        addNewBroadcastButton.addActionListener(e -> addNewSystemUpdate());
        buttonPanel.add(addNewBroadcastButton);

        updateBroadcastButton = createStyledButton("Update Broadcast", new Color(0, 123, 255)); 
        updateBroadcastButton.addActionListener(e -> updateSystemUpdate());
        updateBroadcastButton.setEnabled(false);
        buttonPanel.add(updateBroadcastButton);

        clearBroadcastFormButton = createStyledButton("Clear Form", new Color(108, 117, 125)); 
        clearBroadcastFormButton.addActionListener(e -> clearBroadcastForm());
        buttonPanel.add(clearBroadcastFormButton);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0; 
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private void loadSelectedBroadcastDetails() {
        int selectedRow = systemUpdateLogTable.getSelectedRow();
        if (selectedRow != -1) {
            int updateId = (int) systemUpdateLogTable.getValueAt(selectedRow, 0);
            List<SystemUpdate> allUpdates = Database.getAllSystemUpdates();
            selectedSystemUpdate = allUpdates.stream().filter(u -> u.getId() == updateId).findFirst().orElse(null);

            if (selectedSystemUpdate != null) {
                updateVersionField.setText(selectedSystemUpdate.getVersion() != null ? selectedSystemUpdate.getVersion() : "");
                updateTitleField.setText(selectedSystemUpdate.getTitle());
                updateMessageArea.setText(selectedSystemUpdate.getMessage());

                updateTargetRoleComboBox.setSelectedItem("All Users"); 

                addNewBroadcastButton.setEnabled(false);
                updateBroadcastButton.setEnabled(true);
            }
        }
    }

    private void clearBroadcastForm() {
        selectedSystemUpdate = null;
        updateVersionField.setText("");
        updateTitleField.setText("");
        updateMessageArea.setText("");
        updateTargetRoleComboBox.setSelectedItem("All Users"); 
        addNewBroadcastButton.setEnabled(true);
        updateBroadcastButton.setEnabled(false);
        systemUpdateLogTable.clearSelection(); 
    }

    private void addNewSystemUpdate() { 
        String version = updateVersionField.getText().trim();
        String title = updateTitleField.getText().trim();
        String message = updateMessageArea.getText().trim();

        if (title.isEmpty() || message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and Message cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> targetRoles = null;
        String selectedRole = (String) updateTargetRoleComboBox.getSelectedItem();
        if (selectedRole != null && !selectedRole.equals("All Users")) {
            targetRoles = List.of(selectedRole); 
        }

        SystemUpdate update = new SystemUpdate(version.isEmpty() ? null : version, title, message);
        communicationManager.broadcastSystemUpdate(update, targetRoles);

        JOptionPane.showMessageDialog(this, "System update broadcast initiated.", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearBroadcastForm();
        loadSystemUpdateLog();
    }

    private void updateSystemUpdate() {
        if (selectedSystemUpdate == null) {
            JOptionPane.showMessageDialog(this, "No broadcast selected for update.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String version = updateVersionField.getText().trim();
        String title = updateTitleField.getText().trim();
        String message = updateMessageArea.getText().trim();

        if (title.isEmpty() || message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and Message cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        selectedSystemUpdate.setVersion(version.isEmpty() ? null : version);
        selectedSystemUpdate.setTitle(title);
        selectedSystemUpdate.setMessage(message);

        if (Database.updateSystemUpdate(selectedSystemUpdate)) {
            JOptionPane.showMessageDialog(this, "Broadcast updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearBroadcastForm();
            loadSystemUpdateLog();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update broadcast.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSystemUpdate() {
        if (selectedSystemUpdate == null) {
            JOptionPane.showMessageDialog(this, "No broadcast selected for deletion.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the broadcast '" + selectedSystemUpdate.getTitle() + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (Database.deleteSystemUpdate(selectedSystemUpdate.getId())) { 
                JOptionPane.showMessageDialog(this, "Broadcast deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearBroadcastForm();
                loadSystemUpdateLog();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete broadcast.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadSystemUpdateLog() {
        systemUpdateLogTableModel.setRowCount(0);
        List<SystemUpdate> updates = Database.getAllSystemUpdates();
        for (SystemUpdate update : updates) {
            systemUpdateLogTableModel.addRow(new Object[]{
                update.getId(),
                update.getCreatedAt().format(TABLE_DATE_FORMATTER),
                update.getVersion() != null ? update.getVersion() : "N/A",
                update.getTitle(),
                update.getMessage()
            });
        }
    }


    // --- Tab 6: Communication Settings ---
    private JPanel createCommunicationSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        JTabbedPane commTabbedPane = new JTabbedPane();
        commTabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        commTabbedPane.setBackground(Color.WHITE); 

        commTabbedPane.addTab("Email/SMS Config", createEmailSmsConfigPanel());
        commTabbedPane.addTab("Message Templates", createMessageTemplatesPanel());
        commTabbedPane.addTab("Message Logs", createMessageLogsPanel());

        panel.add(commTabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEmailSmsConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Email (SMTP) & SMS API Configuration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(DARK_NAVY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        // Email Settings
        gbc.gridy++;
        panel.add(new JLabel("--- Email (SMTP) Settings ---"), gbc);
        gbc.gridwidth = 1;
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("SMTP Host:"), gbc);
        gbc.gridx = 1; smtpHostField = new JTextField(25); panel.add(smtpHostField, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("SMTP Port:"), gbc);
        gbc.gridx = 1; smtpPortField = new JTextField(5); panel.add(smtpPortField, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("SMTP Username:"), gbc);
        gbc.gridx = 1; smtpUsernameField = new JTextField(25); panel.add(smtpUsernameField, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("SMTP Password:"), gbc);
        gbc.gridx = 1; smtpPasswordField = new JPasswordField(25); panel.add(smtpPasswordField, gbc); 
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Sender Email:"), gbc);
        gbc.gridx = 1; senderEmailField = new JTextField(25); panel.add(senderEmailField, gbc);

        // SMS Settings
        gbc.gridwidth = 2;
        gbc.gridy++;
        panel.add(new JLabel("--- SMS API Settings ---"), gbc);
        gbc.gridwidth = 1;
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("SMS API Key:"), gbc);
        gbc.gridx = 1; smsApiKeyField = new JTextField(25); panel.add(smsApiKeyField, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("SMS Sender ID:"), gbc);
        gbc.gridx = 1; smsSenderIdField = new JTextField(25); panel.add(smsSenderIdField, gbc);

        // Auto Notifications
        gbc.gridwidth = 2;
        gbc.gridy++;
        panel.add(new JLabel("--- Global Notification Settings ---"), gbc);
        gbc.gridy++;
        autoNotificationsEnabledCheckBox = new JCheckBox("Enable Automatic Notifications (Email/SMS)");
        autoNotificationsEnabledCheckBox.setOpaque(false);
        panel.add(autoNotificationsEnabledCheckBox, gbc);

        gbc.gridy++;
        saveCommSettingsButton = createStyledButton("Save Communication Settings", new Color(40, 167, 69));
        saveCommSettingsButton.addActionListener(e -> saveCommunicationSettings());
        panel.add(saveCommSettingsButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        loadCommunicationSettings();
        return panel;
    }

    private void loadCommunicationSettings() {
        smtpHostField.setText(AppConfig.getProperty(AppConfig.SMTP_HOST_KEY, ""));
        smtpPortField.setText(String.valueOf(AppConfig.getIntProperty(AppConfig.SMTP_PORT_KEY, 587)));
        smtpUsernameField.setText(AppConfig.getProperty(AppConfig.SMTP_USERNAME_KEY, ""));
        smtpPasswordField.setText(AppConfig.getProperty(AppConfig.SMTP_PASSWORD_KEY, "")); 
        senderEmailField.setText(AppConfig.getProperty(AppConfig.SENDER_EMAIL_KEY, ""));
        smsApiKeyField.setText(AppConfig.getProperty(AppConfig.SMS_API_KEY, ""));
        smsSenderIdField.setText(AppConfig.getProperty(AppConfig.SMS_SENDER_ID_KEY, ""));
        autoNotificationsEnabledCheckBox.setSelected(AppConfig.getBooleanProperty(AppConfig.AUTO_NOTIFICATIONS_ENABLED_KEY, false));
    }

    private void saveCommunicationSettings() {
        try {
            int smtpPort = Integer.parseInt(smtpPortField.getText().trim());
            if (smtpPort < 1 || smtpPort > 65535) throw new NumberFormatException();

            communicationManager.saveEmailSettings(
                smtpHostField.getText().trim(),
                smtpPort,
                smtpUsernameField.getText().trim(),
                new String(smtpPasswordField.getPassword()).trim(),
                senderEmailField.getText().trim()
            );
            communicationManager.saveSmsSettings(
                smsApiKeyField.getText().trim(),
                smsSenderIdField.getText().trim()
            );
            communicationManager.setAutoNotificationsEnabled(autoNotificationsEnabledCheckBox.isSelected());

            JOptionPane.showMessageDialog(this, "Communication settings saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid SMTP Port number. Must be between 1 and 65535.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createMessageTemplatesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Top: Template List
        String[] templateColumnNames = {"ID", "Template Name", "Type", "Subject"};
        messageTemplatesTableModel = new DefaultTableModel(templateColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 4) return Boolean.class;
                return super.getColumnClass(column);
            }
        };
        messageTemplatesTable = new JTable(messageTemplatesTableModel);
        messageTemplatesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageTemplatesTable.setRowHeight(20);
        messageTemplatesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        messageTemplatesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
        messageTemplatesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && messageTemplatesTable.getSelectedRow() != -1) {
                loadSelectedTemplateDetails();
            } else if (messageTemplatesTable.getSelectedRow() == -1) {
                clearTemplateForm(); 
            }
        });
        // Hide ID column
        TableColumn idColumn = messageTemplatesTable.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0); idColumn.setMaxWidth(0); idColumn.setPreferredWidth(0); idColumn.setResizable(false);

        JScrollPane tableScrollPane = new JScrollPane(messageTemplatesTable);
        tableScrollPane.setPreferredSize(new Dimension(400, 200)); 
        panel.add(tableScrollPane, BorderLayout.NORTH);

        // Center: Template Editor
        JPanel editorPanel = new JPanel(new GridBagLayout());
        editorPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Edit Message Template");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(DARK_NAVY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        editorPanel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++; gbc.gridx = 0; editorPanel.add(new JLabel("Subject (for Email):"), gbc);
        gbc.gridx = 1; templateSubjectField = new JTextField(30); editorPanel.add(templateSubjectField, gbc);

        gbc.gridy++; gbc.gridx = 0; editorPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1; templateTypeComboBox = new JComboBox<>(new String[]{"EMAIL", "SMS"}); templateTypeComboBox.setEnabled(false); editorPanel.add(templateTypeComboBox, gbc);

        gbc.gridy++; gbc.gridx = 0; editorPanel.add(new JLabel("Body:"), gbc);
        gbc.gridx = 1;
        templateBodyArea = new JTextArea(10, 30);
        templateBodyArea.setLineWrap(true);
        templateBodyArea.setWrapStyleWord(true);
        JScrollPane bodyScrollPane = new JScrollPane(templateBodyArea);
        gbc.weightx = 1.0; 
        gbc.weighty = 1.0; 
        gbc.fill = GridBagConstraints.BOTH; 
        editorPanel.add(bodyScrollPane, gbc);

        // Buttons
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1;
        gbc.weighty = 0; 
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        updateTemplateButton = createStyledButton("Update Template", new Color(0, 123, 255));
        updateTemplateButton.addActionListener(e -> updateMessageTemplate());
        updateTemplateButton.setEnabled(false);
        editorPanel.add(updateTemplateButton, gbc);

        gbc.gridx = 1; 
        deleteTemplateButton = createStyledButton("Delete Template", new Color(220, 53, 69)); 
        deleteTemplateButton.addActionListener(e -> deleteMessageTemplate());
        deleteTemplateButton.setEnabled(false);
        editorPanel.add(deleteTemplateButton, gbc);

        panel.add(editorPanel, BorderLayout.CENTER);

        loadMessageTemplates();
        return panel;
    }

    private void loadMessageTemplates() {
        messageTemplatesTableModel.setRowCount(0);
        List<MessageTemplate> templates = communicationManager.getAllMessageTemplates();
        for (MessageTemplate template : templates) {
            messageTemplatesTableModel.addRow(new Object[]{
                template.getId(),
                template.getTemplateName(),
                template.getType(),
                template.getSubject() != null ? template.getSubject() : "N/A"
            });
        }
    }

    private void loadSelectedTemplateDetails() {
        int selectedRow = messageTemplatesTable.getSelectedRow();
        if (selectedRow != -1) {
            int templateId = (int) messageTemplatesTable.getValueAt(selectedRow, 0);
            List<MessageTemplate> allTemplates = communicationManager.getAllMessageTemplates();
            selectedTemplate = allTemplates.stream().filter(t -> t.getId() == templateId).findFirst().orElse(null);

            if (selectedTemplate != null) {
                templateSubjectField.setText(selectedTemplate.getSubject());
                templateBodyArea.setText(selectedTemplate.getBody());
                templateTypeComboBox.setSelectedItem(selectedTemplate.getType());
                updateTemplateButton.setEnabled(true);
                deleteTemplateButton.setEnabled(true); 
            }
        } else {
            clearTemplateForm();
        }
    }

    private void clearTemplateForm() {
        selectedTemplate = null;
        templateSubjectField.setText("");
        templateBodyArea.setText("");
        templateTypeComboBox.setSelectedIndex(0);
        updateTemplateButton.setEnabled(false);
        deleteTemplateButton.setEnabled(false); 
    }

    private void updateMessageTemplate() {
        if (selectedTemplate == null) {
            JOptionPane.showMessageDialog(this, "Please select a template to update.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        selectedTemplate.setSubject(templateSubjectField.getText().trim());
        selectedTemplate.setBody(templateBodyArea.getText().trim());

        if (communicationManager.updateMessageTemplate(selectedTemplate)) {
            JOptionPane.showMessageDialog(this, "Message template updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadMessageTemplates();
            clearTemplateForm();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update message template.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteMessageTemplate() {
        if (selectedTemplate == null) {
            JOptionPane.showMessageDialog(this, "Please select a template to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete template '" + selectedTemplate.getTemplateName() + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (Database.deleteMessageTemplate(selectedTemplate.getId())) { 
                JOptionPane.showMessageDialog(this, "Message template deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadMessageTemplates();
                clearTemplateForm();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete message template.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createMessageLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        JLabel title = new JLabel("Message Delivery Logs");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(DARK_NAVY);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);

        String[] logColumnNames = {"ID", "Timestamp", "Recipient", "Type", "Subject", "Status", "Details"};
        messageLogTableModel = new DefaultTableModel(logColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        messageLogTable = new JTable(messageLogTableModel);
        messageLogTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageLogTable.setRowHeight(20);
        messageLogTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        // Hide ID column
        TableColumn idColumn = messageLogTable.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0); idColumn.setMaxWidth(0); idColumn.setPreferredWidth(0); idColumn.setResizable(false);

        panel.add(new JScrollPane(messageLogTable), BorderLayout.CENTER);

        loadMessageLogs();
        return panel;
    }

    private void loadMessageLogs() {
        messageLogTableModel.setRowCount(0);
        List<MessageLog> logs = communicationManager.getAllMessageLogs();
        for (MessageLog log : logs) {
            messageLogTableModel.addRow(new Object[]{
                log.getId(),
                log.getTimestamp().format(TABLE_DATE_FORMATTER),
                log.getRecipient(),
                log.getMessageType(),
                log.getSubject() != null ? log.getSubject() : "N/A",
                log.getStatus(),
                log.getDetails() != null ? log.getDetails() : "N/A"
            });
        }
    }

    // --- Tab 7: Central DB Configuration (Existing, moved to tab) ---
    private JPanel createCentralDbConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE); 
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel dbConfigTitle = new JLabel("Central Database Configuration:");
        dbConfigTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        dbConfigTitle.setForeground(DARK_NAVY);
        panel.add(dbConfigTitle, gbc);

        gbc.gridy++;
        panel.add(new JLabel("Current Configured URL:"), gbc);
        gbc.gridy++;
        currentConfiguredUrlLabel = new JLabel("N/A");
        currentConfiguredUrlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(currentConfiguredUrlLabel, gbc);

        gbc.gridy++;
        panel.add(new JLabel("New Central DB URL (JDBC format):"), gbc);
        gbc.gridy++;
        centralDbUrlInputField = new JTextField(50); // Now using the dedicated field
        centralDbUrlInputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        centralDbUrlInputField.putClientProperty("JTextField.placeholderText", "e.g., jdbc:postgresql://host:port/database?user=u&password=p");
        panel.add(centralDbUrlInputField, gbc);

        gbc.gridy++;
        JPanel dbButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        dbButtonsPanel.setOpaque(false);

        saveDbConfigButton = createStyledButton("Save Configuration", new Color(40, 167, 69)); 
        saveDbConfigButton.addActionListener(e -> saveCentralDbConfig());
        dbButtonsPanel.add(saveDbConfigButton);

        testDbConnectionButton = createStyledButton("Test Connection", new Color(0, 123, 255)); 
        testDbConnectionButton.addActionListener(e -> testCentralDbConnection());
        dbButtonsPanel.add(testDbConnectionButton);

        clearDbConfigButton = createStyledButton("Clear Configuration", new Color(255, 165, 0)); 
        clearDbConfigButton.addActionListener(e -> clearCentralDbConfig());
        dbButtonsPanel.add(clearDbConfigButton);

        panel.add(dbButtonsPanel, gbc);

        gbc.gridy++;
        JLabel restartNote = new JLabel("<html><i>Note: Application restart is required for new database configurations to take effect.</i></html>");
        restartNote.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        restartNote.setForeground(Color.GRAY);
        panel.add(restartNote, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0; 
        panel.add(Box.createVerticalGlue(), gbc);

        updateCentralDbUrlDisplay();
        return panel;
    }

    private void updateCentralDbUrlDisplay() {
        String currentUrl = AppConfig.getProperty(AppConfig.CENTRAL_DB_URL_KEY);
        if (currentUrl != null && !currentUrl.trim().isEmpty()) {
            currentConfiguredUrlLabel.setText(currentUrl);
            centralDbUrlInputField.setText(currentUrl); 
        } else {
            currentConfiguredUrlLabel.setText("Not configured (using local SQLite)");
            centralDbUrlInputField.setText("");
        }
    }

    private void saveCentralDbConfig() {
        String newUrl = centralDbUrlInputField.getText().trim();
        String oldUrl = AppConfig.getProperty(AppConfig.CENTRAL_DB_URL_KEY, "");

        if (newUrl.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this, "The URL field is empty. This will clear the central database configuration and the app will use local SQLite. Continue?", "Confirm Clear Configuration", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                AppConfig.removeProperty(AppConfig.CENTRAL_DB_URL_KEY);
                logMessage("Central DB URL configuration cleared.");
                
                int confirmRestart = JOptionPane.showConfirmDialog(this, 
                    "Central database configuration cleared. Restart the application for changes to take effect. Restart now?", 
                    "Restart Application", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);

                if (confirmRestart == JOptionPane.YES_OPTION) {
                    System.exit(0); // This will trigger the shutdown hook and restart the app
                }
            } else {
                return; 
            }
        } else {
            AppConfig.setProperty(AppConfig.CENTRAL_DB_URL_KEY, newUrl);
            logMessage("Central DB URL configuration saved: " + newUrl);
            
            // Prompt for restart if the URL actually changed
            if (!oldUrl.equals(newUrl)) {
                int confirmRestart = JOptionPane.showConfirmDialog(this, 
                    "Central database URL saved. For these changes to take full effect, the application needs to restart. Restart now?", 
                    "Restart Application", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);

                if (confirmRestart == JOptionPane.YES_OPTION) {
                    System.exit(0); // This will trigger the shutdown hook and restart the app
                }
            } else {
                JOptionPane.showMessageDialog(this, "Central database URL saved. No restart needed as URL did not change.", "Configuration Saved", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        updateCentralDbUrlDisplay();
    }

    private void testCentralDbConnection() {
        String testUrl = centralDbUrlInputField.getText().trim();
        if (testUrl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a URL to test.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        logMessage("Testing connection to: " + testUrl);
        boolean connected = Database.testConnection(testUrl);

        if (connected) {
            JOptionPane.showMessageDialog(this, "Connection successful!", "Test Result", JOptionPane.INFORMATION_MESSAGE);
            logMessage("Connection test successful.");
        } else {
            JOptionPane.showMessageDialog(this, "Connection failed. Please check the URL and ensure the database is accessible.", "Test Result", JOptionPane.ERROR_MESSAGE);
            logMessage("Connection test failed.");
        }
    }

    private void clearCentralDbConfig() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear the central database configuration? The application will use local SQLite on next restart.", "Confirm Clear", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            AppConfig.removeProperty(AppConfig.CENTRAL_DB_URL_KEY);
            logMessage("Central DB URL configuration cleared.");
            
            // Prompt for restart after clearing
            int confirmRestart = JOptionPane.showConfirmDialog(this, 
                "Central database configuration cleared. For these changes to take full effect, the application needs to restart. Restart now?", 
                "Restart Application", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE);

            if (confirmRestart == JOptionPane.YES_OPTION) {
                System.exit(0); // This will trigger the shutdown hook and restart the app
            }
        }
        updateCentralDbUrlDisplay();
    }

    // --- Tab 8: Data Export/Import (Existing, moved to tab) ---
    private JPanel createDataExportImportPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE); 
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JPanel syncButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        syncButtonPanel.setOpaque(false);

        exportDataButton = createStyledButton("Export All Data", new Color(40, 167, 69)); 
        exportDataButton.addActionListener(e -> exportAllData());
        syncButtonPanel.add(exportDataButton);

        importDataButton = createStyledButton("Import All Data", new Color(0, 123, 255)); 
        importDataButton.addActionListener(e -> importAllData());
        syncButtonPanel.add(importDataButton);

        viewInstancesButton = createStyledButton("View App Instances", new Color(108, 117, 125)); 
        viewInstancesButton.addActionListener(e -> viewAppInstances());
        syncButtonPanel.add(viewInstancesButton);

        gbc.gridy++;
        panel.add(syncButtonPanel, gbc);

        // Separator
        gbc.gridy++;
        JSeparator separator2 = new JSeparator();
        separator2.setPreferredSize(new Dimension(0, 1));
        panel.add(separator2, gbc);

        // Sync Log Area
        gbc.gridy++;
        JLabel logTitle = new JLabel("Activity Log:");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logTitle.setForeground(DARK_NAVY);
        panel.add(logTitle, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0; 
        gbc.fill = GridBagConstraints.BOTH;
        syncLogArea = new JTextArea(15, 60);
        syncLogArea.setEditable(false);
        syncLogArea.setLineWrap(true);
        syncLogArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(syncLogArea);
        panel.add(scrollPane, gbc);

        return panel;
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
            
            logMessage("Simulating import from: " + fileToLoad.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Data import simulated successfully from " + fileToLoad.getName() + "!", "Import Success", JOptionPane.INFORMATION_MESSAGE);
            logMessage("Data import completed. (Note: Actual data changes would require a rebuild/restart to reflect in UI if not dynamically loaded)");
            parentDashboard.revalidate();
            parentDashboard.repaint();
        } else {
            logMessage("Data import cancelled.");
        }
    }

    private void viewAppInstances() {
        logMessage("Attempting to view app instances...");
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
        // Update cards with relevant counts for Developer
        int totalPeers = Database.getAllPeers().size();
        int systemUpdates = Database.getAllSystemUpdates().size();
        int messageTemplates = Database.getAllMessageTemplates().size();

        cardsPanel.updateCard(0, "Total Peers", totalPeers, new Color(0, 123, 255)); 
        cardsPanel.updateCard(1, "System Updates Logged", systemUpdates, new Color(255, 165, 0)); 
        cardsPanel.updateCard(2, "Message Templates", messageTemplates, new Color(40, 167, 69)); 

        logMessage("Developer Panel data refreshed.");
        updateCentralDbUrlDisplay(); 

        // Refresh data for all sub-panels
        loadSyncConfig();
        loadPeerData();
        loadSyncSettings();
        loadSystemUpdateLog(); 
        loadCommunicationSettings();
        loadMessageTemplates();
        loadMessageLogs();
        clearBroadcastForm(); 
    }
}