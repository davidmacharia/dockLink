package doclink.ui.components;

import doclink.models.DocumentChecklistItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ChecklistItemUploadPanel extends JPanel {
    private DocumentChecklistItem item;
    private JLabel itemNameLabel;
    private JTextField filePathField; // Kept for internal logic, but will be hidden
    public JButton browseButton;
    public JButton clearButton;
    private JCheckBox attachedCheckBox;

    private String originalFilePath; // Stores the path of the selected file from the user's system

    public ChecklistItemUploadPanel(DocumentChecklistItem item) {
        this.item = item;
        this.originalFilePath = null; // No file selected initially

        setLayout(new GridBagLayout());
        setOpaque(false); // Inherit background from parent
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0)); // Small vertical padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        // Attached CheckBox (moved to the left)
        attachedCheckBox = new JCheckBox();
        attachedCheckBox.setOpaque(false);
        attachedCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        add(attachedCheckBox, gbc);

        // Item Name Label (next to the checkbox)
        itemNameLabel = new JLabel(item.getItemName() + (item.isRequired() ? " (Required)" : " (Optional)"));
        itemNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Span across remaining space for label
        gbc.weightx = 1.0; // Allow label to expand
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(itemNameLabel, gbc);

        // File Path Field (hidden, but used internally)
        filePathField = new JTextField(20);
        filePathField.setEditable(false);
        filePathField.setVisible(false); // Hide the text field
        // No need to add to panel, as it's not visible. Its value will be managed programmatically.

        // Browse Button
        browseButton = new JButton("Browse...");
        browseButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        browseButton.setFocusPainted(false);
        browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Document for " + item.getItemName());
                int result = fileChooser.showOpenDialog(ChecklistItemUploadPanel.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    originalFilePath = selectedFile.getAbsolutePath(); // Store full path
                    filePathField.setText(selectedFile.getName()); // Update internal text field
                    attachedCheckBox.setSelected(true);
                    attachedCheckBox.setEnabled(false); // Disable if file is attached
                }
            }
        });

        // Clear Button
        clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearButton.setFocusPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                originalFilePath = null;
                filePathField.setText(""); // Clear internal text field
                attachedCheckBox.setSelected(false);
                // Re-enable attached checkbox for optional items if no file is selected
                if (!item.isRequired() && !item.requiresFileUpload()) {
                    attachedCheckBox.setEnabled(true);
                }
            }
        });

        // Conditional visibility and placement for buttons
        if (item.requiresFileUpload()) {
            gbc.gridx = 0;
            gbc.gridy = 1; // New row for buttons
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            add(browseButton, gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            add(clearButton, gbc);
        } else {
            // If no file upload is required, hide buttons
            browseButton.setVisible(false);
            clearButton.setVisible(false);
            attachedCheckBox.setEnabled(true); // Always enable for non-file upload items
        }
        
        // Initial state for attached checkbox
        if (item.isRequired() && !item.requiresFileUpload()) {
            // If required but no file upload, it must be manually checked
            attachedCheckBox.setSelected(false); // Start unchecked
            attachedCheckBox.setEnabled(true); // Allow user to check it
        } else if (item.isRequired() && item.requiresFileUpload()) {
            // If required and needs file upload, it's initially unchecked and disabled until a file is chosen
            attachedCheckBox.setSelected(false);
            attachedCheckBox.setEnabled(false);
        } else if (!item.isRequired() && !item.requiresFileUpload()) {
            // If optional and no file upload, it's initially unchecked and enabled
            attachedCheckBox.setSelected(false);
            attachedCheckBox.setEnabled(true);
        } else if (!item.isRequired() && item.requiresFileUpload()) {
            // If optional but needs file upload, it's initially unchecked and disabled until a file is chosen
            attachedCheckBox.setSelected(false);
            attachedCheckBox.setEnabled(false);
        }
    }

    public DocumentChecklistItem getChecklistItem() {
        return item;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public boolean isAttached() {
        return attachedCheckBox.isSelected();
    }
}