package doclink.ui.panels.common;

import doclink.AppConfig;
import doclink.Database;
import doclink.communication.CommunicationManager;
import doclink.models.MessageLog;
import doclink.models.MessageTemplate;
import doclink.ui.Dashboard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class CommunicationSettingsPanel extends JPanel implements Dashboard.Refreshable {
    private CommunicationManager communicationManager;
    private Consumer<String> logConsumer;

    // Email/SMS Config components
    private JTextField smtpHostField, smtpPortField, smtpUsernameField, senderEmailField;
    private JPasswordField smtpPasswordField; 
    private JTextField smsApiKeyField, smsSenderIdField;
    private JCheckBox autoNotificationsEnabledCheckBox;
    private JButton saveCommSettingsButton;

    // Message Templates components
    private JTable messageTemplatesTable;
    private DefaultTableModel messageTemplatesTableModel;
    private JTextArea templateBodyArea;
    private JTextField templateSubjectField;
    private JComboBox<String> templateTypeComboBox;
    private JButton updateTemplateButton;
    private JButton deleteTemplateButton; 
    private MessageTemplate selectedTemplate;

    // Message Logs components
    private JTable messageLogTable;
    private DefaultTableModel messageLogTableModel;

    private static final Color DARK_NAVY = new Color(26, 35, 126);
    private static final DateTimeFormatter TABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CommunicationSettingsPanel(CommunicationManager communicationManager, Consumer<String> logConsumer) {
        this.communicationManager = communicationManager;
        this.logConsumer = logConsumer;
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel mainTitle = new JLabel("Communication Settings");
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        mainTitle.setForeground(DARK_NAVY);
        mainTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(mainTitle, BorderLayout.NORTH);

        JTabbedPane commTabbedPane = new JTabbedPane();
        commTabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        commTabbedPane.setBackground(Color.WHITE); 

        commTabbedPane.addTab("Email/SMS Config", createEmailSmsConfigPanel());
        commTabbedPane.addTab("Message Templates", createMessageTemplatesPanel());
        commTabbedPane.addTab("Message Logs", createMessageLogsPanel());

        add(commTabbedPane, BorderLayout.CENTER);
        
        loadCommunicationSettings();
        loadMessageTemplates();
        loadMessageLogs();
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
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
    }

    private JPanel createEmailSmsConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbcMain = new GridBagConstraints();
        gbcMain.insets = new Insets(10, 10, 10, 10);
        gbcMain.fill = GridBagConstraints.BOTH;
        gbcMain.weighty = 1.0;

        JPanel leftConfigPanel = new JPanel(new GridBagLayout());
        leftConfigPanel.setOpaque(false);
        leftConfigPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Email (SMTP) Settings"));
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.insets = new Insets(5, 5, 5, 5);
        gbcLeft.fill = GridBagConstraints.HORIZONTAL;
        gbcLeft.anchor = GridBagConstraints.WEST;
        gbcLeft.gridx = 0;
        int rowLeft = 0;

        gbcLeft.gridy = rowLeft++; gbcLeft.gridx = 0; leftConfigPanel.add(new JLabel("SMTP Host:"), gbcLeft);
        gbcLeft.gridx = 1; smtpHostField = new JTextField(15); leftConfigPanel.add(smtpHostField, gbcLeft);
        gbcLeft.gridy = rowLeft++; gbcLeft.gridx = 0; leftConfigPanel.add(new JLabel("SMTP Port:"), gbcLeft);
        gbcLeft.gridx = 1; smtpPortField = new JTextField(5); leftConfigPanel.add(smtpPortField, gbcLeft);
        gbcLeft.gridy = rowLeft++; gbcLeft.gridx = 0; leftConfigPanel.add(new JLabel("SMTP Username:"), gbcLeft);
        gbcLeft.gridx = 1; smtpUsernameField = new JTextField(15); leftConfigPanel.add(smtpUsernameField, gbcLeft);
        gbcLeft.gridy = rowLeft++; gbcLeft.gridx = 0; leftConfigPanel.add(new JLabel("SMTP Password:"), gbcLeft);
        gbcLeft.gridx = 1; smtpPasswordField = new JPasswordField(15); leftConfigPanel.add(smtpPasswordField, gbcLeft); 
        gbcLeft.gridy = rowLeft++; gbcLeft.gridx = 0; leftConfigPanel.add(new JLabel("Sender Email:"), gbcLeft);
        gbcLeft.gridx = 1; senderEmailField = new JTextField(15); leftConfigPanel.add(senderEmailField, gbcLeft);
        
        gbcLeft.gridy = rowLeft++;
        gbcLeft.weighty = 1.0;
        leftConfigPanel.add(Box.createVerticalGlue(), gbcLeft);

        JPanel rightConfigPanel = new JPanel(new GridBagLayout());
        rightConfigPanel.setOpaque(false);
        rightConfigPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "SMS & Global Settings"));
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.insets = new Insets(5, 5, 5, 5);
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        gbcRight.anchor = GridBagConstraints.WEST;
        gbcRight.gridx = 0;
        int rowRight = 0;

        gbcRight.gridx = 0; gbcRight.gridy = rowRight++; gbcRight.gridwidth = 2;
        JLabel smsTitle = new JLabel("SMS API Settings");
        smsTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        smsTitle.setForeground(DARK_NAVY);
        rightConfigPanel.add(smsTitle, gbcRight);

        gbcRight.gridwidth = 1;
        gbcRight.gridy++; gbcRight.gridx = 0; rightConfigPanel.add(new JLabel("SMS API Key:"), gbcRight);
        gbcRight.gridx = 1; smsApiKeyField = new JTextField(15); rightConfigPanel.add(smsApiKeyField, gbcRight);
        gbcRight.gridy++; gbcRight.gridx = 0; rightConfigPanel.add(new JLabel("SMS Sender ID:"), gbcRight);
        gbcRight.gridx = 1; smsSenderIdField = new JTextField(15); rightConfigPanel.add(smsSenderIdField, gbcRight);

        gbcRight.gridwidth = 2;
        gbcRight.gridy++;
        JLabel globalNotifTitle = new JLabel("Global Notification Settings");
        globalNotifTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        globalNotifTitle.setForeground(DARK_NAVY);
        rightConfigPanel.add(globalNotifTitle, gbcRight);

        gbcRight.gridy++;
        autoNotificationsEnabledCheckBox = new JCheckBox("Enable Automatic Notifications (Email/SMS)");
        autoNotificationsEnabledCheckBox.setOpaque(false);
        rightConfigPanel.add(autoNotificationsEnabledCheckBox, gbcRight);

        gbcRight.gridy++;
        saveCommSettingsButton = createStyledButton("Save Settings", new Color(40, 167, 69));
        saveCommSettingsButton.addActionListener(e -> saveCommunicationSettings());
        rightConfigPanel.add(saveCommSettingsButton, gbcRight);

        gbcRight.gridy++;
        gbcRight.weighty = 1.0;
        rightConfigPanel.add(Box.createVerticalGlue(), gbcRight);

        gbcMain.gridx = 0;
        gbcMain.weightx = 0.5;
        panel.add(leftConfigPanel, gbcMain);

        gbcMain.gridx = 1;
        gbcMain.weightx = 0.5;
        panel.add(rightConfigPanel, gbcMain);

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
            logMessage("Communication settings saved.");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid SMTP Port number. Must be between 1 and 65535.", "Input Error", JOptionPane.ERROR_MESSAGE);
            logMessage("Error saving communication settings: Invalid SMTP Port.");
        }
    }

    private JPanel createMessageTemplatesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Message Templates"));

        String[] templateColumnNames = {"ID", "Template Name", "Type", "Subject"};
        messageTemplatesTableModel = new DefaultTableModel(templateColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
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
        TableColumn idColumn = messageTemplatesTable.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0); idColumn.setMaxWidth(0); idColumn.setPreferredWidth(0); idColumn.setResizable(false);

        JScrollPane tableScrollPane = new JScrollPane(messageTemplatesTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 150));
        panel.add(tableScrollPane, BorderLayout.NORTH);

        JPanel editorPanel = new JPanel(new GridBagLayout());
        editorPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; editorPanel.add(new JLabel("Subject (for Email):"), gbc);
        gbc.gridx = 1; templateSubjectField = new JTextField(15); editorPanel.add(templateSubjectField, gbc);

        gbc.gridx = 0; gbc.gridy++; editorPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1; templateTypeComboBox = new JComboBox<>(new String[]{"EMAIL", "SMS"}); templateTypeComboBox.setEnabled(false); editorPanel.add(templateTypeComboBox, gbc);

        gbc.gridx = 0; gbc.gridy++; editorPanel.add(new JLabel("Body:"), gbc);
        gbc.gridx = 1;
        templateBodyArea = new JTextArea(5, 15);
        templateBodyArea.setLineWrap(true);
        templateBodyArea.setWrapStyleWord(true);
        JScrollPane bodyScrollPane = new JScrollPane(templateBodyArea);
        bodyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        gbc.weightx = 1.0; 
        gbc.weighty = 1.0; 
        gbc.fill = GridBagConstraints.BOTH; 
        editorPanel.add(bodyScrollPane, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        buttonPanel.setOpaque(false);

        updateTemplateButton = createStyledButton("Update", new Color(0, 123, 255));
        updateTemplateButton.addActionListener(e -> updateMessageTemplate());
        updateTemplateButton.setEnabled(false);
        buttonPanel.add(updateTemplateButton);

        deleteTemplateButton = createStyledButton("Delete", new Color(220, 53, 69)); 
        deleteTemplateButton.addActionListener(e -> deleteMessageTemplate());
        deleteTemplateButton.setEnabled(false);
        buttonPanel.add(deleteTemplateButton);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        editorPanel.add(buttonPanel, gbc);

        panel.add(editorPanel, BorderLayout.CENTER);

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
            logMessage("Message template '" + selectedTemplate.getTemplateName() + "' updated.");
            loadMessageTemplates();
            clearTemplateForm();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update message template.", "Error", JOptionPane.ERROR_MESSAGE);
            logMessage("Error updating message template '" + selectedTemplate.getTemplateName() + "'.");
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
                logMessage("Message template '" + selectedTemplate.getTemplateName() + "' deleted.");
                loadMessageTemplates();
                clearTemplateForm();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete message template.", "Error", JOptionPane.ERROR_MESSAGE);
                logMessage("Error deleting message template '" + selectedTemplate.getTemplateName() + "'.");
            }
        }
    }

    private JPanel createMessageLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Message Delivery Logs"));

        JLabel title = new JLabel("Message Delivery Logs");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
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
        TableColumn idColumn = messageLogTable.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0); idColumn.setMaxWidth(0); idColumn.setPreferredWidth(0); idColumn.setResizable(false);

        panel.add(new JScrollPane(messageLogTable), BorderLayout.CENTER);

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

    @Override
    public void refreshData() {
        loadCommunicationSettings();
        loadMessageTemplates();
        loadMessageLogs();
        clearTemplateForm();
    }
}