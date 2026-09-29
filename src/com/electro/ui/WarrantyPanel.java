package com.electro.ui;

import com.electro.model.WarrantyRecord;
import com.electro.service.DataStore;
import com.electro.service.WarrantyService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Warranty and Serial Number Tracker for customer support and post-sale service claims.
 */
public class WarrantyPanel extends JPanel {
    private final WarrantyService warrantyService;
    private final DataStore dataStore;

    private JTextField searchField;
    private JTable warrantyTable;
    private DefaultTableModel warrantyTableModel;

    // Quick lookup status card
    private JPanel lookupCard;
    private JLabel lblStatusBadge;
    private JLabel lblProductName;
    private JLabel lblCustomerName;
    private JLabel lblInvoiceId;
    private JLabel lblPurchaseDate;
    private JLabel lblExpiryDate;
    private JLabel lblDaysRemaining;
    private JButton btnCancelWarranty;
    private WarrantyRecord activeRecord;

    private List<WarrantyRecord> currentRecords = new ArrayList<>();

    public WarrantyPanel(WarrantyService warrantyService) {
        this.warrantyService = warrantyService;
        this.dataStore = DataStore.getInstance();

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.COLOR_BG);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(createTopLookupBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLookupCardPanel(), createRegistryTablePanel());
        split.setResizeWeight(0.35);
        split.setDividerSize(6);
        split.setBorder(null);

        add(split, BorderLayout.CENTER);
        refreshTable();
    }

    private JPanel createTopLookupBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 10));
        bar.setBackground(UITheme.COLOR_PANEL_BG);
        bar.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel title = new JLabel("Warranty & Serial / IMEI Claim Tracker");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_PRIMARY_DARK);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setBackground(UITheme.COLOR_PANEL_BG);

        searchField = UITheme.createTextField(18);
        searchField.putClientProperty("JTextField.placeholderText", "Enter Serial #, IMEI, or Phone...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                handleSearch();
            }
        });

        JButton btnSearch = UITheme.createButton("Instant Check", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnSearch.addActionListener(e -> handleSearch());

        controls.add(new JLabel("Quick Lookup:"));
        controls.add(searchField);
        controls.add(btnSearch);

        bar.add(title, BorderLayout.WEST);
        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    private JPanel createLookupCardPanel() {
        lookupCard = new JPanel();
        lookupCard.setLayout(new BoxLayout(lookupCard, BoxLayout.Y_AXIS));
        lookupCard.setBackground(UITheme.COLOR_PANEL_BG);
        lookupCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel("Warranty Claim Status");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_TEXT_PRIMARY);

        lblStatusBadge = new JLabel("ENTER SERIAL OR PHONE", SwingConstants.CENTER);
        lblStatusBadge.setFont(UITheme.FONT_REGULAR_BOLD);
        lblStatusBadge.setOpaque(true);
        lblStatusBadge.setBackground(UITheme.COLOR_BORDER);
        lblStatusBadge.setForeground(UITheme.COLOR_TEXT_MUTED);
        lblStatusBadge.setBorder(new EmptyBorder(8, 14, 8, 14));
        lblStatusBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblProductName = createDetailRow("Product:");
        lblCustomerName = createDetailRow("Customer:");
        lblInvoiceId = createDetailRow("Invoice #:");
        lblPurchaseDate = createDetailRow("Purchase Date:");
        lblExpiryDate = createDetailRow("Warranty Ends:");
        lblDaysRemaining = createDetailRow("Validity:");

        lookupCard.add(title);
        lookupCard.add(Box.createVerticalStrut(14));
        lookupCard.add(lblStatusBadge);
        lookupCard.add(Box.createVerticalStrut(18));
        lookupCard.add(lblProductName);
        lookupCard.add(Box.createVerticalStrut(8));
        lookupCard.add(lblCustomerName);
        lookupCard.add(Box.createVerticalStrut(8));
        lookupCard.add(lblInvoiceId);
        lookupCard.add(Box.createVerticalStrut(8));
        lookupCard.add(lblPurchaseDate);
        lookupCard.add(Box.createVerticalStrut(8));
        lookupCard.add(lblExpiryDate);
        lookupCard.add(Box.createVerticalStrut(8));
        lookupCard.add(lblDaysRemaining);
        lookupCard.add(Box.createVerticalStrut(18));

        btnCancelWarranty = UITheme.createButton("Cancel Warranty", new Color(225, 29, 72), Color.WHITE);
        btnCancelWarranty.setToolTipText("Permanently cancel and void warranty coverage for this item");
        btnCancelWarranty.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCancelWarranty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnCancelWarranty.setEnabled(false);
        btnCancelWarranty.addActionListener(e -> handleCancelWarranty());
        lookupCard.add(btnCancelWarranty);

        lookupCard.add(Box.createVerticalGlue());

        return lookupCard;
    }

    private JLabel createDetailRow(String prefix) {
        JLabel lbl = new JLabel("<html><strong>" + prefix + "</strong> -</html>");
        lbl.setFont(UITheme.FONT_REGULAR);
        lbl.setForeground(UITheme.COLOR_TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel createRegistryTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel title = new JLabel("All Registered Serial Numbers & Warranties");
        title.setFont(UITheme.FONT_REGULAR_BOLD);
        title.setForeground(UITheme.COLOR_SECONDARY);

        String[] cols = {"Serial / IMEI", "Product Name", "Customer Name", "Phone", "Invoice #", "Purchase Date", "Expiry Date", "Status"};
        warrantyTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        warrantyTable = new JTable(warrantyTableModel);
        UITheme.styleTable(warrantyTable);
        warrantyTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        warrantyTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        warrantyTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        warrantyTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        warrantyTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        warrantyTable.getColumnModel().getColumn(5).setPreferredWidth(85);
        warrantyTable.getColumnModel().getColumn(6).setPreferredWidth(85);
        warrantyTable.getColumnModel().getColumn(7).setPreferredWidth(100);

        warrantyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && warrantyTable.getSelectedRow() >= 0) {
                showRecordInCard(currentRecords.get(warrantyTable.getSelectedRow()));
            }
        });

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bottomBar.setBackground(UITheme.COLOR_PANEL_BG);

        JButton btnCancelSelected = UITheme.createButton("Cancel Selected Warranty", new Color(225, 29, 72), Color.WHITE);
        btnCancelSelected.setToolTipText("Cancel the warranty for the selected serial / IMEI row");
        btnCancelSelected.addActionListener(e -> handleCancelWarranty());
        bottomBar.add(btnCancelSelected);

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(warrantyTable), BorderLayout.CENTER);
        panel.add(bottomBar, BorderLayout.SOUTH);
        return panel;
    }

    private void handleSearch() {
        String q = searchField.getText().trim();
        refreshTable();

        if (!q.isEmpty()) {
            WarrantyRecord rec = warrantyService.findBySerial(q);
            if (rec != null) {
                showRecordInCard(rec);
                return;
            }
            List<WarrantyRecord> byPhone = warrantyService.findByCustomerPhone(q);
            if (!byPhone.isEmpty()) {
                showRecordInCard(byPhone.get(0));
                return;
            }
        }
    }

    private void showRecordInCard(WarrantyRecord rec) {
        this.activeRecord = rec;
        if (rec == null) {
            btnCancelWarranty.setEnabled(false);
            return;
        }

        btnCancelWarranty.setEnabled(!rec.isVoided());

        if (rec.isCancelled()) {
            lblStatusBadge.setText("WARRANTY CANCELLED");
            lblStatusBadge.setBackground(new Color(185, 28, 28));
            lblStatusBadge.setForeground(Color.WHITE);
        } else if (rec.isVoided()) {
            lblStatusBadge.setText("VOID (ITEM RETURNED)");
            lblStatusBadge.setBackground(new Color(185, 28, 28));
            lblStatusBadge.setForeground(Color.WHITE);
        } else if (rec.isExpired()) {
            lblStatusBadge.setText("WARRANTY EXPIRED");
            lblStatusBadge.setBackground(UITheme.COLOR_DANGER);
            lblStatusBadge.setForeground(Color.WHITE);
        } else {
            lblStatusBadge.setText("ACTIVE WARRANTY");
            lblStatusBadge.setBackground(UITheme.COLOR_SUCCESS);
            lblStatusBadge.setForeground(Color.WHITE);
        }

        lblProductName.setText("<html><strong>Product:</strong> " + rec.getBrand() + " " + rec.getProductName() + "</html>");
        lblCustomerName.setText("<html><strong>Customer:</strong> " + rec.getCustomerName() + " (" + rec.getCustomerPhone() + ")</html>");
        lblInvoiceId.setText("<html><strong>Invoice #:</strong> " + rec.getInvoiceId() + "</html>");
        lblPurchaseDate.setText("<html><strong>Purchase Date:</strong> " + rec.getPurchaseDate() + "</html>");
        lblExpiryDate.setText("<html><strong>Warranty Ends:</strong> " + rec.getExpiryDate() + " (" + rec.getWarrantyMonths() + "M)</html>");

        if (rec.isCancelled()) {
            lblDaysRemaining.setText("<html><strong>Validity:</strong> <span style='color:red; font-weight:bold;'>Cancelled</span></html>");
        } else if (rec.isVoided()) {
            lblDaysRemaining.setText("<html><strong>Validity:</strong> <span style='color:red; font-weight:bold;'>Void (Returned)</span></html>");
        } else if (rec.isExpired()) {
            lblDaysRemaining.setText("<html><strong>Validity:</strong> <span style='color:red;'>Expired</span></html>");
        } else {
            lblDaysRemaining.setText("<html><strong>Validity:</strong> <span style='color:green; font-weight:bold;'>" + rec.getRemainingDays() + " days remaining</span></html>");
        }
    }

    public void refreshTable() {
        String q = searchField.getText().trim().toLowerCase();
        List<WarrantyRecord> all = dataStore.getAllWarranties();
        currentRecords = new ArrayList<>();

        for (WarrantyRecord wr : all) {
            if (q.isEmpty() ||
                wr.getSerialNumber().toLowerCase().contains(q) ||
                (wr.getCustomerName() != null && wr.getCustomerName().toLowerCase().contains(q)) ||
                (wr.getCustomerPhone() != null && wr.getCustomerPhone().contains(q)) ||
                (wr.getProductName() != null && wr.getProductName().toLowerCase().contains(q)) ||
                (wr.getInvoiceId() != null && wr.getInvoiceId().toLowerCase().contains(q))) {
                currentRecords.add(wr);
            }
        }

        warrantyTableModel.setRowCount(0);
        for (WarrantyRecord wr : currentRecords) {
            String statusText;
            if (wr.isCancelled()) {
                statusText = "CANCELLED";
            } else if (wr.isVoided()) {
                statusText = "VOID (RETURNED)";
            } else if (wr.isExpired()) {
                statusText = "EXPIRED";
            } else {
                statusText = "ACTIVE (" + wr.getRemainingDays() + "d)";
            }

            warrantyTableModel.addRow(new Object[]{
                    wr.getSerialNumber(),
                    wr.getBrand() + " " + wr.getProductName(),
                    wr.getCustomerName(),
                    wr.getCustomerPhone(),
                    wr.getInvoiceId(),
                    wr.getPurchaseDate(),
                    wr.getExpiryDate(),
                    statusText
            });
        }
    }

    private void handleCancelWarranty() {
        WarrantyRecord target = activeRecord;
        int selectedRow = warrantyTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < currentRecords.size()) {
            target = currentRecords.get(selectedRow);
        }

        if (target == null) {
            JOptionPane.showMessageDialog(this, "Please select or search for a warranty record to cancel.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (target.isCancelled()) {
            JOptionPane.showMessageDialog(this, "Warranty for serial '" + target.getSerialNumber() + "' is already CANCELLED.", "Already Cancelled", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (target.isVoided()) {
            JOptionPane.showMessageDialog(this, "Warranty for serial '" + target.getSerialNumber() + "' is already VOID (Item Returned / Refunded).", "Already Void", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel the warranty coverage for this item?\n\n" +
                "Product: " + target.getBrand() + " " + target.getProductName() + "\n" +
                "Serial / IMEI: " + target.getSerialNumber() + "\n" +
                "Customer: " + target.getCustomerName() + " (" + target.getCustomerPhone() + ")\n" +
                "Invoice #: " + target.getInvoiceId() + "\n\n" +
                "This action will permanently void all warranty service rights.\nDo you wish to proceed?",
                "Confirm Cancel Warranty",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = warrantyService.cancelWarranty(target.getSerialNumber());
            if (success) {
                refreshTable();
                showRecordInCard(target);
                JOptionPane.showMessageDialog(this, "Warranty for serial '" + target.getSerialNumber() + "' has been successfully CANCELLED.", "Warranty Cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to cancel warranty.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
