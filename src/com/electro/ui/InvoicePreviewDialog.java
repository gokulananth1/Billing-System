package com.electro.ui;

import com.electro.model.Invoice;
import com.electro.model.ShopSettings;
import com.electro.service.DataStore;
import com.electro.service.InvoiceGenerator;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Dialog displaying invoice preview with print, thermal print, and browser export options.
 */
public class InvoicePreviewDialog extends JDialog {
    private final Invoice invoice;
    private final ShopSettings settings;

    public InvoicePreviewDialog(Window owner, Invoice invoice) {
        super(owner, "Tax Invoice - " + invoice.getInvoiceId(), ModalityType.APPLICATION_MODAL);
        this.invoice = invoice;
        this.settings = DataStore.getInstance().getSettings();

        initUI();
    }

    private void initUI() {
        setSize(780, 700);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(0, 0));

        // Invoice Preview in JEditorPane (HTML)
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setText(InvoiceGenerator.generateHtmlInvoice(invoice, settings));
        editorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(UITheme.COLOR_PANEL_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton btnOpenBrowser = UITheme.createButton("Open / Save PDF in Browser", new Color(13, 148, 136), Color.WHITE);
        btnOpenBrowser.setToolTipText("Opens the full A4 invoice in your browser to print or save as PDF");
        btnOpenBrowser.addActionListener(e -> {
            File htmlFile = InvoiceGenerator.saveHtmlInvoiceToFile(invoice, settings);
            if (htmlFile != null && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(htmlFile);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Saved to: " + htmlFile.getAbsolutePath(), "Invoice Saved", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        JButton btnThermal = UITheme.createButton("Print Thermal POS", UITheme.COLOR_SECONDARY, Color.WHITE);
        btnThermal.addActionListener(e -> InvoiceGenerator.printReceipt(invoice, settings));

        JButton btnClose = UITheme.createButton("Close", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnClose.addActionListener(e -> dispose());

        footer.add(btnOpenBrowser);
        footer.add(btnThermal);
        footer.add(btnClose);

        add(footer, BorderLayout.SOUTH);
    }
}
