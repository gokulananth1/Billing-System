package com.electro.ui;

import com.electro.model.ColumnConfig;
import com.electro.model.ShopSettings;
import com.electro.model.User;
import com.electro.service.AuthService;
import com.electro.service.DataStore;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Administrative dialog providing full CRUD (Create, Read, Update, Delete)
 * operations for catalog table column configurations.
 * 
 * - Create: Add new custom columns (e.g. Color, Location, Rack No, Supplier).
 * - Read: View all columns (system & custom) in an interactive table with visibility & descriptions.
 * - Update: Edit column display name, description, and visibility.
 * - Delete: Permanently remove custom columns or reset system columns to default.
 */
public class ColumnConfigDialog extends JDialog {

    private final Window parent;
    private final DataStore dataStore;
    private final Runnable onUpdatedCallback;

    private JTable columnTable;
    private DefaultTableModel tableModel;
    private List<ColumnConfig> currentConfigs = new ArrayList<>();
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnToggleVisibility;

    public ColumnConfigDialog(Window parent, Runnable onUpdatedCallback) {
        super(parent, "Column Details Manager (Admin CRUD)", ModalityType.APPLICATION_MODAL);
        this.parent = parent;
        this.dataStore = DataStore.getInstance();
        this.onUpdatedCallback = onUpdatedCallback;

        setSize(850, 620);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        UITheme.applyAppIcon(this);

        initUI();
        refreshTable();
    }

    private void initUI() {
        User currentUser = AuthService.getInstance().getCurrentUser();
        boolean isAdmin = (currentUser != null && currentUser.isAdmin());

        // Header Panel
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.COLOR_PRIMARY_DARK);
        header.setBorder(new EmptyBorder(16, 22, 16, 22));

        JLabel title = new JLabel("Catalog Table Columns Manager");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Full CRUD access: Create custom columns, customize display names (ID, SKU, Brand, Product Name, etc.), toggle visibility, or delete.");
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(new Color(203, 213, 225));

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Center Content
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(UITheme.COLOR_BG);
        contentPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(isAdmin ? new Color(240, 253, 244) : new Color(254, 242, 242));
        banner.setBorder(new CompoundBorder(
                new LineBorder(isAdmin ? new Color(187, 247, 208) : new Color(254, 202, 202), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel lblBanner = new JLabel(isAdmin
                ? "✓ Administrator Privileges: You can Add, Edit, Rename, Toggle Visibility, and Delete column details."
                : "⚠ Access Restricted: You must be logged in as Administrator to perform CRUD operations on columns.");
        lblBanner.setFont(UITheme.FONT_REGULAR_BOLD);
        lblBanner.setForeground(isAdmin ? new Color(22, 101, 52) : UITheme.COLOR_DANGER);
        banner.add(lblBanner, BorderLayout.CENTER);
        contentPanel.add(banner, BorderLayout.NORTH);

        // Table
        String[] columns = {"#", "Key / Field ID", "Display Name", "Type", "Status", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        columnTable = new JTable(tableModel);
        columnTable.setRowHeight(32);
        columnTable.setFont(UITheme.FONT_REGULAR);
        columnTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        columnTable.getTableHeader().setFont(UITheme.FONT_REGULAR_BOLD);
        columnTable.getTableHeader().setBackground(new Color(241, 245, 249));

        columnTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        columnTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        columnTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        columnTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        columnTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        columnTable.getColumnModel().getColumn(5).setPreferredWidth(260);

        // Custom Cell Renderers for Status & Type
        columnTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(CENTER);
                if (!isSelected) {
                    if ("System".equals(value)) {
                        setForeground(new Color(30, 64, 175));
                        setFont(UITheme.FONT_REGULAR_BOLD);
                    } else {
                        setForeground(new Color(13, 148, 136));
                        setFont(UITheme.FONT_REGULAR_BOLD);
                    }
                }
                return c;
            }
        });

        columnTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(CENTER);
                if (!isSelected) {
                    if ("Visible".equals(value)) {
                        setForeground(new Color(22, 101, 52));
                    } else {
                        setForeground(UITheme.COLOR_DANGER);
                    }
                }
                return c;
            }
        });

        // Double-click to edit
        columnTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && isAdmin) {
                    handleEditColumn();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(columnTable);
        scrollPane.setBorder(new LineBorder(UITheme.COLOR_BORDER, 1));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // CRUD Toolbar Buttons Panel (Right side or bottom)
        JPanel crudBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        crudBar.setBackground(UITheme.COLOR_BG);

        if (isAdmin) {
            btnAdd = UITheme.createButton("+ Add New Column", UITheme.COLOR_SUCCESS, Color.WHITE);
            btnAdd.setToolTipText("Create a new custom column for catalog items (e.g. Color, Supplier, Rack)");
            btnAdd.addActionListener(e -> handleAddColumn());
            crudBar.add(btnAdd);

            btnEdit = UITheme.createButton("Edit Display Name / Details", UITheme.COLOR_PRIMARY, Color.WHITE);
            btnEdit.setToolTipText("Change what title displays for this column or edit its details");
            btnEdit.addActionListener(e -> handleEditColumn());
            crudBar.add(btnEdit);

            btnToggleVisibility = UITheme.createButton("Toggle Visibility", new Color(100, 116, 139), Color.WHITE);
            btnToggleVisibility.setToolTipText("Show or Hide this column in catalog tables");
            btnToggleVisibility.addActionListener(e -> handleToggleVisibility());
            crudBar.add(btnToggleVisibility);

            btnDelete = UITheme.createButton("Delete / Reset Column", UITheme.COLOR_DANGER, Color.WHITE);
            btnDelete.setToolTipText("Delete custom column permanently, or reset system column back to default title");
            btnDelete.addActionListener(e -> handleDeleteColumn());
            crudBar.add(btnDelete);

            JButton btnRetailPreset = new JButton("Apply Retail Preset");
            btnRetailPreset.setFont(UITheme.FONT_SMALL);
            btnRetailPreset.addActionListener(e -> applyRetailPreset());
            crudBar.add(btnRetailPreset);

            JButton btnResetAll = new JButton("Reset All to Defaults");
            btnResetAll.setFont(UITheme.FONT_SMALL);
            btnResetAll.addActionListener(e -> handleResetAllDefaults());
            crudBar.add(btnResetAll);
        }

        contentPanel.add(crudBar, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(UITheme.COLOR_PANEL_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton btnClose = UITheme.createButton("Close", UITheme.COLOR_PRIMARY_DARK, Color.WHITE);
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);

        add(footer, BorderLayout.SOUTH);
    }

    private void refreshTable() {
        ShopSettings settings = dataStore.getSettings();
        currentConfigs = settings.getColumnConfigs();
        tableModel.setRowCount(0);

        int idx = 1;
        for (ColumnConfig c : currentConfigs) {
            tableModel.addRow(new Object[]{
                    idx++,
                    c.getKey(),
                    c.getDisplayName(),
                    c.isSystem() ? "System" : "Custom",
                    c.isVisible() ? "Visible" : "Hidden",
                    c.getDescription()
            });
        }
    }

    private ColumnConfig getSelectedColumn() {
        int row = columnTable.getSelectedRow();
        if (row < 0 || row >= currentConfigs.size()) {
            JOptionPane.showMessageDialog(this, "Please select a column from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return currentConfigs.get(row);
    }

    // CREATE: Add a new custom column
    private void handleAddColumn() {
        JDialog dlg = new JDialog(this, "Add New Catalog Column (Admin)", ModalityType.APPLICATION_MODAL);
        dlg.setSize(440, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));
        UITheme.applyAppIcon(dlg);

        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        p.setBorder(new EmptyBorder(16, 16, 8, 16));

        JTextField tfKey = UITheme.createTextField(14);
        JTextField tfName = UITheme.createTextField(14);
        JTextField tfDesc = UITheme.createTextField(14);
        JCheckBox chkVis = new JCheckBox("Show column in catalog tables by default", true);

        p.add(new JLabel("Internal Field Key:")); p.add(tfKey);
        p.add(new JLabel("Display Name (Header Title):")); p.add(tfName);
        p.add(new JLabel("Description / Purpose:")); p.add(tfDesc);
        p.add(new JLabel("Initial Visibility:")); p.add(chkVis);

        dlg.add(p, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = UITheme.createButton("Cancel", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnCreate = UITheme.createButton("Create Column", UITheme.COLOR_SUCCESS, Color.WHITE);
        btnCreate.addActionListener(e -> {
            String key = tfKey.getText().trim();
            String name = tfName.getText().trim();
            String desc = tfDesc.getText().trim();
            boolean vis = chkVis.isSelected();

            if (key.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Field Key cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ShopSettings settings = dataStore.getSettings();
            if (settings.getColumnConfig(key) != null) {
                JOptionPane.showMessageDialog(dlg, "A column with key '" + key + "' already exists.", "Duplicate Key", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ColumnConfig newCol = new ColumnConfig(key, name.isEmpty() ? key : name, desc, false, vis);
            settings.addColumnConfig(newCol);
            dataStore.saveSettings();

            refreshTable();
            if (onUpdatedCallback != null) onUpdatedCallback.run();
            dlg.dispose();

            JOptionPane.showMessageDialog(this, "New column '" + newCol.getDisplayName() + "' created successfully!", "Column Created", JOptionPane.INFORMATION_MESSAGE);
        });

        actions.add(btnCancel);
        actions.add(btnCreate);
        dlg.add(actions, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    // UPDATE: Edit column display name, description, and visibility
    private void handleEditColumn() {
        ColumnConfig selected = getSelectedColumn();
        if (selected == null) return;

        JDialog dlg = new JDialog(this, "Edit Column Details: " + selected.getKey(), ModalityType.APPLICATION_MODAL);
        dlg.setSize(440, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));
        UITheme.applyAppIcon(dlg);

        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        p.setBorder(new EmptyBorder(16, 16, 8, 16));

        JLabel lblKeyVal = new JLabel(selected.getKey() + (selected.isSystem() ? " (Core System Column)" : " (Custom Field)"));
        lblKeyVal.setFont(UITheme.FONT_REGULAR_BOLD);

        JTextField tfName = UITheme.createTextField(14);
        tfName.setText(selected.getDisplayName());

        JTextField tfDesc = UITheme.createTextField(14);
        tfDesc.setText(selected.getDescription());

        JCheckBox chkVis = new JCheckBox("Visible in tables", selected.isVisible());

        p.add(new JLabel("Field Key:")); p.add(lblKeyVal);
        p.add(new JLabel("Display Name (Header Title):")); p.add(tfName);
        p.add(new JLabel("Description:")); p.add(tfDesc);
        p.add(new JLabel("Visibility:")); p.add(chkVis);

        dlg.add(p, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = UITheme.createButton("Cancel", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnSave = UITheme.createButton("Save Changes", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnSave.addActionListener(e -> {
            String newName = tfName.getText().trim();
            String newDesc = tfDesc.getText().trim();
            boolean vis = chkVis.isSelected();

            ShopSettings settings = dataStore.getSettings();
            settings.updateColumnConfig(selected.getKey(), newName.isEmpty() ? selected.getKey() : newName, newDesc, vis);
            dataStore.saveSettings();

            refreshTable();
            if (onUpdatedCallback != null) onUpdatedCallback.run();
            dlg.dispose();

            JOptionPane.showMessageDialog(this, "Column details for '" + selected.getKey() + "' updated successfully!", "Column Updated", JOptionPane.INFORMATION_MESSAGE);
        });

        actions.add(btnCancel);
        actions.add(btnSave);
        dlg.add(actions, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    // UPDATE Visibility toggle
    private void handleToggleVisibility() {
        ColumnConfig selected = getSelectedColumn();
        if (selected == null) return;

        ShopSettings settings = dataStore.getSettings();
        boolean newVis = !selected.isVisible();
        settings.updateColumnConfig(selected.getKey(), selected.getDisplayName(), selected.getDescription(), newVis);
        dataStore.saveSettings();

        refreshTable();
        if (onUpdatedCallback != null) onUpdatedCallback.run();
    }

    // DELETE: Delete custom column or reset system column
    private void handleDeleteColumn() {
        ColumnConfig selected = getSelectedColumn();
        if (selected == null) return;

        ShopSettings settings = dataStore.getSettings();

        if (selected.isSystem()) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "'" + selected.getKey() + "' is a core system column.\n"
                    + "Reset its display name back to default ('" + selected.getKey() + "')?",
                    "Reset System Column",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                settings.deleteColumnConfig(selected.getKey());
                dataStore.saveSettings();
                refreshTable();
                if (onUpdatedCallback != null) onUpdatedCallback.run();
                JOptionPane.showMessageDialog(this, "Column '" + selected.getKey() + "' reset to default.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to permanently delete custom column '" + selected.getDisplayName() + "' (" + selected.getKey() + ")?\n"
                    + "This action cannot be undone.",
                    "Confirm Delete Column",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                settings.deleteColumnConfig(selected.getKey());
                dataStore.saveSettings();
                refreshTable();
                if (onUpdatedCallback != null) onUpdatedCallback.run();
                JOptionPane.showMessageDialog(this, "Custom column '" + selected.getKey() + "' deleted successfully.", "Column Deleted", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void applyRetailPreset() {
        ShopSettings settings = dataStore.getSettings();
        settings.setColumnDisplayName("ID", "Item Code");
        settings.setColumnDisplayName("SKU", "Barcode");
        settings.setColumnDisplayName("Brand", "Brand / Maker");
        settings.setColumnDisplayName("Product Name", "Item Description");
        settings.setColumnDisplayName("Category", "Department");
        settings.setColumnDisplayName("Model", "Model No.");
        settings.setColumnDisplayName("Cost", "Purchase Cost");
        settings.setColumnDisplayName("Retail", "MRP (Retail)");
        settings.setColumnDisplayName("Wholesale", "Wholesale Rate");
        settings.setColumnDisplayName("GST", "Tax Rate (%)");
        settings.setColumnDisplayName("Stock", "Qty on Hand");
        settings.setColumnDisplayName("Warranty", "Warranty Period");
        settings.setColumnDisplayName("Serial Req", "IMEI / Serial Req");
        dataStore.saveSettings();

        refreshTable();
        if (onUpdatedCallback != null) onUpdatedCallback.run();
        JOptionPane.showMessageDialog(this, "Retail preset applied to column display names!", "Preset Applied", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleResetAllDefaults() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Reset all column configurations and display names back to standard defaults?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            ShopSettings settings = dataStore.getSettings();
            settings.resetColumnNamesToDefault();
            dataStore.saveSettings();

            refreshTable();
            if (onUpdatedCallback != null) onUpdatedCallback.run();
            JOptionPane.showMessageDialog(this, "All columns reset to defaults.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
