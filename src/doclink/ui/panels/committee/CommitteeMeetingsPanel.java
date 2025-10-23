package doclink.ui.panels.committee;

import doclink.Database;
import doclink.models.Meeting;
import doclink.models.User;
import doclink.ui.Dashboard;
import doclink.ui.DashboardCardsPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Vector;

public class CommitteeMeetingsPanel extends JPanel implements Dashboard.Refreshable {
    private User currentUser;
    private Dashboard parentDashboard;
    private DashboardCardsPanel cardsPanel;

    private JTable meetingsTable;
    private DefaultTableModel tableModel;
    private JTextField titleField, dateField, timeField, locationField;
    private JTextArea agendaArea;
    private JComboBox<String> statusComboBox;
    private JButton addMeetingButton, updateMeetingButton, deleteMeetingButton;

    private Meeting selectedMeeting;
    private static final Color DARK_NAVY = new Color(26, 35, 126);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CommitteeMeetingsPanel(User user, Dashboard parentDashboard, DashboardCardsPanel cardsPanel) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.cardsPanel = cardsPanel;
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel panelTitle = new JLabel("Committee Meetings Management");
        panelTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panelTitle.setForeground(DARK_NAVY);
        panelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(panelTitle, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.6); // 60% for table, 40% for form
        splitPane.setResizeWeight(0.6);
        splitPane.setBackground(new Color(245, 247, 250));

        splitPane.setLeftComponent(createMeetingsTablePanel());
        splitPane.setRightComponent(createMeetingFormPanel());

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createMeetingsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel tableTitle = new JLabel("All Meetings");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(DARK_NAVY);
        tableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(tableTitle, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Title", "Date", "Time", "Location", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        meetingsTable = new JTable(tableModel);
        meetingsTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        meetingsTable.setRowHeight(25);
        meetingsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        meetingsTable.getTableHeader().setBackground(new Color(230, 230, 230));
        meetingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        meetingsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && meetingsTable.getSelectedRow() != -1) {
                    loadSelectedMeetingDetails();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(meetingsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMeetingFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel formTitle = new JLabel("Meeting Details / Add New");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formTitle.setForeground(DARK_NAVY);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(formTitle, gbc);

        gbc.gridwidth = 1; // Reset gridwidth
        int row = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        titleField = new JTextField(20);
        panel.add(titleField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        dateField = new JTextField(20);
        panel.add(dateField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Time (HH:MM AM/PM):"), gbc);
        gbc.gridx = 1;
        timeField = new JTextField(20);
        panel.add(timeField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        locationField = new JTextField(20);
        panel.add(locationField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Agenda:"), gbc);
        row++;
        gbc.gridy = row;
        agendaArea = new JTextArea(5, 20);
        agendaArea.setLineWrap(true);
        agendaArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(agendaArea);
        panel.add(scrollPane, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        String[] statuses = {"Scheduled", "Cancelled", "Completed"};
        statusComboBox = new JComboBox<>(statuses);
        panel.add(statusComboBox, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        addMeetingButton = createStyledButton("Add New Meeting", new Color(40, 167, 69)); // Green
        addMeetingButton.addActionListener(e -> addMeeting());
        panel.add(addMeetingButton, gbc);

        row++;
        gbc.gridy = row;
        updateMeetingButton = createStyledButton("Update Meeting", new Color(0, 123, 255)); // Blue
        updateMeetingButton.addActionListener(e -> updateMeeting());
        updateMeetingButton.setEnabled(false);
        panel.add(updateMeetingButton, gbc);

        row++;
        gbc.gridy = row;
        deleteMeetingButton = createStyledButton("Delete Meeting", new Color(220, 53, 69)); // Red
        deleteMeetingButton.addActionListener(e -> deleteMeeting());
        deleteMeetingButton.setEnabled(false);
        panel.add(deleteMeetingButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0; // Push components to top
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
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

    private void loadSelectedMeetingDetails() {
        int selectedRow = meetingsTable.getSelectedRow();
        if (selectedRow != -1) {
            int meetingId = (int) meetingsTable.getValueAt(selectedRow, 0);
            selectedMeeting = Database.getMeetingById(meetingId);

            if (selectedMeeting != null) {
                titleField.setText(selectedMeeting.getTitle());
                dateField.setText(selectedMeeting.getDate().format(DATE_FORMATTER));
                timeField.setText(selectedMeeting.getTime());
                locationField.setText(selectedMeeting.getLocation());
                agendaArea.setText(selectedMeeting.getAgenda());
                statusComboBox.setSelectedItem(selectedMeeting.getStatus());

                addMeetingButton.setEnabled(false);
                updateMeetingButton.setEnabled(true);
                deleteMeetingButton.setEnabled(true);
            }
        }
    }

    private void clearForm() {
        selectedMeeting = null;
        titleField.setText("");
        dateField.setText("");
        timeField.setText("");
        locationField.setText("");
        agendaArea.setText("");
        statusComboBox.setSelectedItem("Scheduled");

        addMeetingButton.setEnabled(true);
        updateMeetingButton.setEnabled(false);
        deleteMeetingButton.setEnabled(false);
    }

    private void addMeeting() {
        String title = titleField.getText().trim();
        String dateStr = dateField.getText().trim();
        String time = timeField.getText().trim();
        String location = locationField.getText().trim();
        String agenda = agendaArea.getText().trim();
        String status = (String) statusComboBox.getSelectedItem();

        if (title.isEmpty() || dateStr.isEmpty() || time.isEmpty() || location.isEmpty() || status == null) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields (Title, Date, Time, Location, Status).", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Meeting newMeeting = new Meeting(title, date, time, location, agenda, status);
        if (Database.addMeeting(newMeeting)) {
            JOptionPane.showMessageDialog(this, "Meeting added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add meeting.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMeeting() {
        if (selectedMeeting == null) {
            JOptionPane.showMessageDialog(this, "Please select a meeting to update.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String title = titleField.getText().trim();
        String dateStr = dateField.getText().trim();
        String time = timeField.getText().trim();
        String location = locationField.getText().trim();
        String agenda = agendaArea.getText().trim();
        String status = (String) statusComboBox.getSelectedItem();

        if (title.isEmpty() || dateStr.isEmpty() || time.isEmpty() || location.isEmpty() || status == null) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields (Title, Date, Time, Location, Status).", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        selectedMeeting.setTitle(title);
        selectedMeeting.setDate(date);
        selectedMeeting.setTime(time);
        selectedMeeting.setLocation(location);
        selectedMeeting.setAgenda(agenda);
        selectedMeeting.setStatus(status);

        if (Database.updateMeeting(selectedMeeting)) {
            JOptionPane.showMessageDialog(this, "Meeting updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update meeting.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteMeeting() {
        if (selectedMeeting == null) {
            JOptionPane.showMessageDialog(this, "Please select a meeting to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete meeting '" + selectedMeeting.getTitle() + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (Database.deleteMeeting(selectedMeeting.getId())) {
                JOptionPane.showMessageDialog(this, "Meeting deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete meeting.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void refreshData() {
        // Update cards with relevant counts for Committee
        int plansForReviewCount = Database.getPlansByStatus("Under Review (Committee)").size() +
                                  Database.getPlansByStatus("Approved by Structural (to Committee)").size();
        int upcomingMeetings = Database.getUpcomingMeetingsCount(); // NEW: Get upcoming meetings count
        int returnedPlans = Database.getPlansByStatus("Rejected").size() + Database.getPlansByStatus("Deferred").size();

        cardsPanel.updateCard(0, "Plans for Committee Review", plansForReviewCount, new Color(255, 193, 7)); // Yellow
        cardsPanel.updateCard(1, "Upcoming Meetings", upcomingMeetings, new Color(0, 123, 255)); // Blue for meetings
        cardsPanel.updateCard(2, "Rejected/Deferred Plans", returnedPlans, new Color(220, 53, 69)); // Red

        // Update table with all meetings
        tableModel.setRowCount(0); // Clear existing data
        List<Meeting> meetings = Database.getAllMeetings();
        for (int i = 0; i < meetings.size(); i++) {
            Meeting meeting = meetings.get(i);
            tableModel.addRow(new Object[]{
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDate().format(DATE_FORMATTER),
                meeting.getTime(),
                meeting.getLocation(),
                meeting.getStatus()
            });
        }
        clearForm(); // Clear form after refresh
    }
}