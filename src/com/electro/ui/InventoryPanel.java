package com.electro.ui;

import com.electro.model.Product;
import com.electro.service.CsvExportService;
import com.electro.service.DataStore;
import com.electro.service.InventoryService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Inventory management panel for adding, editing, restocking, and tracking electronics items.
 */
public class InventoryPanel extends JPanel {
    private final InventoryService inventoryService;
    private final Window parentWindow;

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JCheckBox chkLowStockOnly;
    private JButton btnRemoveAll;
    private List<Product> currentList = new ArrayList<>();

    public InventoryPanel(Window parentWindow, InventoryService inventoryService) {
        this.parentWindow = parentWindow;
        this.inventoryService = inventoryService;

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.COLOR_BG);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(createTopBar(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);

        refreshTable();
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 10));
        bar.setBackground(UITheme.COLOR_PANEL_BG);
        bar.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel title = new JLabel("Product Inventory & Stock Management");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.COLOR_PRIMARY_DARK);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setBackground(UITheme.COLOR_PANEL_BG);

        searchField = UITheme.createTextField(15);
        searchField.putClientProperty("JTextField.placeholderText", "Search products...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refreshTable();
            }
        });

        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(UITheme.FONT_REGULAR);
        categoryCombo.addItem("All Categories");
        for (String c : inventoryService.getAllCategories()) {
            categoryCombo.addItem(c);
        }
        categoryCombo.addActionListener(e -> refreshTable());

        chkLowStockOnly = new JCheckBox("Low Stock (<4) Only");
        chkLowStockOnly.setFont(UITheme.FONT_REGULAR_BOLD);
        chkLowStockOnly.setForeground(UITheme.COLOR_DANGER);
        chkLowStockOnly.setBackground(UITheme.COLOR_PANEL_BG);
        chkLowStockOnly.addActionListener(e -> refreshTable());

        controls.add(new JLabel("Search:"));
        controls.add(searchField);
        controls.add(new JLabel("Category:"));
        controls.add(categoryCombo);
        controls.add(chkLowStockOnly);

        bar.add(title, BorderLayout.WEST);
        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.COLOR_PANEL_BG);
        panel.setBorder(new LineBorder(UITheme.COLOR_BORDER, 1, true));

        String[] cols = {"ID", "SKU", "Brand", "Product Name", "Category", "Model", "Cost", "Retail", "Wholesale", "GST", "Stock", "Warranty", "Serial Req"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(170);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(70);
        table.getColumnModel().getColumn(7).setPreferredWidth(75);
        table.getColumnModel().getColumn(8).setPreferredWidth(75);
        table.getColumnModel().getColumn(9).setPreferredWidth(50);
        table.getColumnModel().getColumn(10).setPreferredWidth(55);
        table.getColumnModel().getColumn(11).setPreferredWidth(70);
        table.getColumnModel().getColumn(12).setPreferredWidth(70);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bar.setBackground(UITheme.COLOR_BG);

        JButton btnAdd = UITheme.createButton("+ Add New Product", UITheme.COLOR_SUCCESS, Color.WHITE);
        btnAdd.addActionListener(e -> showProductDialog(null));

        JButton btnEdit = UITheme.createButton("Edit Selected", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnEdit.addActionListener(e -> {
            Product p = getSelectedProduct();
            if (p != null) showProductDialog(p);
        });

        JButton btnRestock = UITheme.createButton("Restock / Add Units", new Color(13, 148, 136), Color.WHITE);
        btnRestock.addActionListener(e -> handleRestock());

        JButton btnDelete = UITheme.createButton("Delete Product", UITheme.COLOR_DANGER, Color.WHITE);
        btnDelete.addActionListener(e -> handleDelete());

        JButton btnExportCsv = UITheme.createButton("Export to CSV", new Color(16, 185, 129), Color.WHITE);
        btnExportCsv.setToolTipText("Export product inventory and stock catalog to CSV for Excel");
        btnExportCsv.addActionListener(e -> handleExportCsv());

        btnRemoveAll = UITheme.createButton("Remove All Stock Items", new Color(185, 28, 28), Color.WHITE);
        btnRemoveAll.setToolTipText("Admin only: Permanently remove all stock items and products from the application");
        btnRemoveAll.addActionListener(e -> handleRemoveAllStockItems());

        bar.add(btnAdd);
        bar.add(btnEdit);
        bar.add(btnRestock);
        bar.add(btnDelete);
        bar.add(btnExportCsv);
        bar.add(btnRemoveAll);
        return bar;
    }

    private Product getSelectedProduct() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return currentList.get(row);
    }

    private void handleRestock() {
        Product p = getSelectedProduct();
        if (p == null) return;

        String input = JOptionPane.showInputDialog(this, "Enter quantity to add to current stock (" + p.getStockQuantity() + "):", "Restock Product", JOptionPane.QUESTION_MESSAGE);
        if (input != null && !input.trim().isEmpty()) {
            try {
                int addQty = Integer.parseInt(input.trim());
                if (addQty > 0) {
                    inventoryService.restockProduct(p.getId(), addQty);
                    refreshTable();
                    JOptionPane.showMessageDialog(this, "Successfully added " + addQty + " units to " + p.getName(), "Restocked", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number entered.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDelete() {
        Product p = getSelectedProduct();
        if (p == null) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete '" + p.getName() + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            inventoryService.deleteProduct(p.getId());
            refreshTable();
        }
    }

    public void refreshTable() {
        String q = searchField.getText();
        String cat = (String) categoryCombo.getSelectedItem();
        if ("All Categories".equals(cat)) cat = null;

        List<Product> products = inventoryService.searchProducts(q, cat);
        if (chkLowStockOnly.isSelected()) {
            products.removeIf(p -> !p.isLowStock());
        }
        currentList = products;

        tableModel.setRowCount(0);
        String sym = DataStore.getInstance().getSettings().getCurrencySymbol();

        for (Product p : currentList) {
            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getSku(),
                    p.getBrand(),
                    p.getName(),
                    p.getCategory(),
                    p.getModelNumber(),
                    UITheme.formatCurrency(p.getCostPrice(), sym),
                    UITheme.formatCurrency(p.getSellingPrice(), sym),
                    UITheme.formatCurrency(p.getWholesalePrice(), sym),
                    (int) p.getTaxRate() + "%",
                    p.getStockQuantity() <= 0 ? "0 (OUT)" : (p.getStockQuantity() + (p.isLowStock() ? " (LOW)" : "")),
                    p.getWarrantyMonths() + " Mos",
                    p.isRequiresSerial() ? "Yes (IMEI/SN)" : "No"
            });
        }

        com.electro.model.User currentUser = com.electro.service.AuthService.getInstance().getCurrentUser();
        if (btnRemoveAll != null) {
            btnRemoveAll.setVisible(currentUser != null && currentUser.isAdmin());
        }
    }

    private void handleRemoveAllStockItems() {
        com.electro.model.User user = com.electro.service.AuthService.getInstance().getCurrentUser();
        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only Administrators can remove all stock items.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int count = inventoryService.getAllProducts().size();
        if (count == 0) {
            JOptionPane.showMessageDialog(this, "The inventory catalog is already empty. There are no stock items to remove.", "Inventory Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently remove all " + count + " stock item(s) from the application?\n"
                + "This will delete all products from the inventory catalog and stock list.\n\n"
                + "This action cannot be undone. Do you wish to proceed?",
                "Confirm Remove All Stock Items",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            inventoryService.removeAllProducts();
            refreshTable();
            JOptionPane.showMessageDialog(this, "All stock items have been successfully removed from the application!", "Stock Removed", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showProductDialog(Product existing) {
        JDialog dlg = new JDialog(parentWindow, existing == null ? "Add New Electronics Product" : "Edit Product", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(500, 600);
        dlg.setLocationRelativeTo(parentWindow);
        dlg.setLayout(new BorderLayout(10, 10));
        UITheme.applyAppIcon(dlg);

        JPanel form = new JPanel(new GridLayout(11, 2, 8, 8));
        form.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextField tfName = UITheme.createTextField(15);
        JTextField tfBrand = UITheme.createTextField(15);
        JTextField tfCategory = UITheme.createTextField(15);
        JTextField tfModel = UITheme.createTextField(15);
        JTextField tfSku = UITheme.createTextField(15);
        JTextField tfCost = UITheme.createTextField(15);
        JTextField tfPrice = UITheme.createTextField(15);
        JTextField tfWholesale = UITheme.createTextField(15);
        JTextField tfTax = UITheme.createTextField(15);
        JTextField tfStock = UITheme.createTextField(15);
        JTextField tfWarranty = UITheme.createTextField(15);
        JCheckBox chkSerial = new JCheckBox("Requires Serial / IMEI per unit");

        if (existing != null) {
            tfName.setText(existing.getName());
            tfBrand.setText(existing.getBrand());
            tfCategory.setText(existing.getCategory());
            tfModel.setText(existing.getModelNumber());
            tfSku.setText(existing.getSku());
            tfCost.setText(String.valueOf(existing.getCostPrice()));
            tfPrice.setText(String.valueOf(existing.getSellingPrice()));
            tfWholesale.setText(String.valueOf(existing.getWholesalePrice()));
            tfTax.setText(String.valueOf(existing.getTaxRate()));
            tfStock.setText(String.valueOf(existing.getStockQuantity()));
            tfWarranty.setText(String.valueOf(existing.getWarrantyMonths()));
            chkSerial.setSelected(existing.isRequiresSerial());
        } else {
            tfTax.setText("18.0");
            tfWarranty.setText("12");
            chkSerial.setSelected(true);
        }

        form.add(new JLabel("Product Name:")); form.add(tfName);
        form.add(new JLabel("Brand:")); form.add(tfBrand);
        form.add(new JLabel("Category:")); form.add(tfCategory);
        form.add(new JLabel("Model Number:")); form.add(tfModel);
        form.add(new JLabel("SKU / Barcode:")); form.add(tfSku);
        form.add(new JLabel("Cost Price:")); form.add(tfCost);
        form.add(new JLabel("Retail Price (MRP):")); form.add(tfPrice);
        form.add(new JLabel("Wholesale Price (B2B):")); form.add(tfWholesale);
        form.add(new JLabel("Tax Rate (%):")); form.add(tfTax);
        form.add(new JLabel("Initial Stock:")); form.add(tfStock);
        form.add(new JLabel("Warranty (Months):")); form.add(tfWarranty);

        JPanel checkWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        checkWrap.add(chkSerial);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.add(form, BorderLayout.CENTER);
        centerWrap.add(checkWrap, BorderLayout.SOUTH);
        dlg.add(centerWrap, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = UITheme.createButton("Cancel", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnSave = UITheme.createButton("Save Product", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnSave.addActionListener(e -> {
            try {
                String name = tfName.getText().trim();
                String brand = tfBrand.getText().trim();
                String category = tfCategory.getText().trim();
                String model = tfModel.getText().trim();
                String sku = tfSku.getText().trim();
                double cost = Double.parseDouble(tfCost.getText().trim());
                double price = Double.parseDouble(tfPrice.getText().trim());
                double wholesale = tfWholesale.getText().trim().isEmpty() ? Math.round(price * 0.92 * 100.0) / 100.0 : Double.parseDouble(tfWholesale.getText().trim());
                double tax = Double.parseDouble(tfTax.getText().trim());
                int stock = Integer.parseInt(tfStock.getText().trim());
                int warranty = Integer.parseInt(tfWarranty.getText().trim());
                boolean serial = chkSerial.isSelected();

                if (name.isEmpty() || brand.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Name and Brand are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Product p = existing != null ? existing : new Product();
                p.setName(name);
                p.setBrand(brand);
                p.setCategory(category.isEmpty() ? "Electronics" : category);
                p.setModelNumber(model);
                p.setSku(sku.isEmpty() ? ("SKU-" + System.currentTimeMillis() % 10000) : sku);
                p.setCostPrice(cost);
                p.setSellingPrice(price);
                p.setWholesalePrice(wholesale);
                p.setTaxRate(tax);
                p.setStockQuantity(stock);
                p.setWarrantyMonths(warranty);
                p.setRequiresSerial(serial);

                inventoryService.saveProduct(p);
                dlg.dispose();
                refreshTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Please ensure numeric fields (Price, Cost, Wholesale, Tax, Stock, Warranty) contain valid numbers.", "Format Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        actions.add(btnCancel);
        actions.add(btnSave);
        dlg.add(actions, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void handleExportCsv() {
        if (currentList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No products available to export.", "Empty Inventory", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDate.now());
        chooser.setSelectedFile(new File("inventory_catalog_" + dateStr + ".csv"));
        chooser.setDialogTitle("Export Inventory Catalog to CSV");

        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".csv")) {
                target = new File(target.getParentFile(), target.getName() + ".csv");
            }
            try {
                CsvExportService.exportInventoryToCsv(target, currentList);
                JOptionPane.showMessageDialog(this,
                        "Successfully exported " + currentList.size() + " products to:\n" + target.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "CSV Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
