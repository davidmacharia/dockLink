package doclink.ui.components;

import doclink.Database;
import doclink.models.Document;
import doclink.models.DocumentChecklistItem;
import doclink.models.Plan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class DocumentViewerDialog extends JDialog {

    private JTable documentsTable;
    private DefaultTableModel documentsTableModel;
    private Plan currentPlan;
    private List<DocumentChecklistItem> allChecklistItems;

    private static final Color DARK_NAVY = new Color(26, 35, 126);

    public DocumentViewerDialog(Frame owner, Plan plan) {
        super(owner, "Documents for Plan ID: " + plan.getId(), true); // Modal dialog
        this.currentPlan = plan;
        this.allChecklistItems = Database.getAllChecklistItems(); // Fetch once

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));
        setPreferredSize(new Dimension(700, 450));
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(owner); // Center on parent frame

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(DARK_NAVY);
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel titleLabel = new JLabel("Documents for Plan: " + (plan.getReferenceNo() != null ? plan.getReferenceNo() : "ID " + plan.getId()));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        add(createDocumentsTablePanel(), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 247, 250));
        JButton closeButton = createStyledButton("Close", new Color(108, 117, 125));
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        loadDocumentsData();
    }

    private JPanel createDocumentsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        String[] docColumnNames = {"ID", "Document Name", "Attached", "Type", "Required by Planning", "Action", "DocumentObject"};
        documentsTableModel = new DefaultTableModel(docColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only the "Action" column is editable (for the button)
            }
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 2 || column == 4) return Boolean.class; // "Attached" and "Required by Planning" are booleans
                if (column == 5) return String.class; // "Action" is a String for the button text
                if (column == 6) return Document.class; // Hidden column for Document object
                return super.getColumnClass(column);
            }
        };
        documentsTable = new JTable(documentsTableModel);
        documentsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        documentsTable.setRowHeight(25);
        documentsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        documentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Hide ID column
        TableColumn idColumn = documentsTable.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0); idColumn.setMaxWidth(0); idColumn.setPreferredWidth(0); idColumn.setResizable(false);

        // Hide DocumentObject column
        TableColumn docObjectColumn = documentsTable.getColumnModel().getColumn(6);
        docObjectColumn.setMinWidth(0); docObjectColumn.setMaxWidth(0); docObjectColumn.setPreferredWidth(0); docObjectColumn.setResizable(false);

        // Set up button renderer and editor for the "Action" column
        documentsTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        documentsTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(documentsTable, this));

        // Make "Required by Planning" column non-editable (it's just an indicator)
        TableColumn requiredColumn = documentsTable.getColumnModel().getColumn(4);
        requiredColumn.setCellEditor(null); // No editor for this column

        JScrollPane scrollPane = new JScrollPane(documentsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void loadDocumentsData() {
        documentsTableModel.setRowCount(0);
        if (currentPlan != null) {
            List<Document> documents = Database.getDocumentsByPlanId(currentPlan.getId());
            for (Document doc : documents) {
                boolean isRequired = Database.isDocumentRequired(doc.getDocName(), allChecklistItems);
                documentsTableModel.addRow(new Object[]{
                    doc.getId(),
                    doc.getDocName(),
                    doc.isAttached(),
                    doc.getDocumentType(),
                    isRequired,
                    "Open File",
                    doc
                });
            }
        }
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

    // Inner classes for Button Renderer and Editor
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBackground(new Color(0, 123, 255)); // Blue
            setForeground(Color.WHITE);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private String label;
        private String clickedFilePath;
        private JTable table;
        private Component parentComponent; // Reference to the dialog itself

        public ButtonEditor(JTable table, Component parentComponent) {
            this.table = table;
            this.parentComponent = parentComponent;
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setBackground(new Color(0, 123, 255));
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false); // Corrected: Call on 'button'
            button.setFocusPainted(false); // Corrected: Call on 'button'
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.addActionListener(this);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            
            Document doc = (Document) table.getModel().getValueAt(row, 6);
            if (doc != null) {
                clickedFilePath = doc.getFilePath();
            } else {
                clickedFilePath = null;
            }
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return label;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
            Database.openDocumentFile(clickedFilePath, parentComponent);
        }
    }
}