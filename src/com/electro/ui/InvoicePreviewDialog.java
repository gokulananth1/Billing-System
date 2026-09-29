package com.electro.ui;

import com.electro.model.Invoice;
import com.electro.model.ShopSettings;
import com.electro.service.DataStore;
import com.electro.service.InvoiceGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
        UITheme.applyAppIcon(this);

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

        JButton btnDirectPrint = UITheme.createButton("Direct Print A4", new Color(79, 70, 229), Color.WHITE);
        btnDirectPrint.setToolTipText("Immediately opens Windows printer dialog to print this A4 invoice");
        btnDirectPrint.addActionListener(e -> {
            try {
                boolean complete = editorPane.print(null, null, true, null, null, true);
                if (complete) {
                    JOptionPane.showMessageDialog(this, "Invoice successfully sent to printer!", "Print Complete", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        });

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

        footer.add(btnDirectPrint);
        footer.add(btnOpenBrowser);
        footer.add(btnThermal);
        footer.add(btnClose);

        add(footer, BorderLayout.SOUTH);
    }
}
