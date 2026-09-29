package com.electro.ui;

import com.electro.model.User;
import com.electro.service.AuthService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Modal dialog for securely updating account passwords with verification of the current password.
 */
public class ChangePasswordDialog extends JDialog {
    private final AuthService authService;
    private final User targetUser;

    private JPasswordField tfCurrentPass;
    private JPasswordField tfNewPass;
    private JPasswordField tfConfirmPass;
    private JLabel lblStatus;

    public ChangePasswordDialog(Window owner, User targetUser) {
        super(owner, "Change Password - " + (targetUser != null ? targetUser.getUsername() : "User"), ModalityType.APPLICATION_MODAL);
        this.authService = AuthService.getInstance();
        this.targetUser = targetUser;

        initUI();
    }

    private void initUI() {
        setSize(420, 390);
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.COLOR_BG);

        UITheme.applyAppIcon(this);

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 2, 2));
        header.setBackground(UITheme.COLOR_PRIMARY_DARK);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("Change Account Password");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(Color.WHITE);

        String name = targetUser != null ? targetUser.getFullName() + " (" + targetUser.getUsername() + ")" : "User";
        JLabel subtitle = new JLabel("Updating credentials for: " + name);
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(new Color(203, 213, 225));

        header.add(title);
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        // Form Card
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(UITheme.COLOR_BG);
        centerWrap.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel formCard = new JPanel(new GridLayout(6, 1, 4, 4));
        formCard.setBackground(UITheme.COLOR_PANEL_BG);
        formCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        tfCurrentPass = new JPasswordField();
        tfCurrentPass.setFont(UITheme.FONT_REGULAR);
        tfCurrentPass.setBorder(new CompoundBorder(new LineBorder(UITheme.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));

        tfNewPass = new JPasswordField();
        tfNewPass.setFont(UITheme.FONT_REGULAR);
        tfNewPass.setBorder(new CompoundBorder(new LineBorder(UITheme.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));

        tfConfirmPass = new JPasswordField();
        tfConfirmPass.setFont(UITheme.FONT_REGULAR);
        tfConfirmPass.setBorder(new CompoundBorder(new LineBorder(UITheme.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));

        JLabel lbl1 = new JLabel("Current Password:");
        lbl1.setFont(UITheme.FONT_REGULAR_BOLD);

        JLabel lbl2 = new JLabel("New Password:");
        lbl2.setFont(UITheme.FONT_REGULAR_BOLD);

        JLabel lbl3 = new JLabel("Confirm New Password:");
        lbl3.setFont(UITheme.FONT_REGULAR_BOLD);

        formCard.add(lbl1);
        formCard.add(tfCurrentPass);
        formCard.add(lbl2);
        formCard.add(tfNewPass);
        formCard.add(lbl3);
        formCard.add(tfConfirmPass);

        centerWrap.add(formCard, BorderLayout.CENTER);

        lblStatus = new JLabel(" ");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.COLOR_DANGER);
        lblStatus.setBorder(new EmptyBorder(6, 4, 2, 4));
        centerWrap.add(lblStatus, BorderLayout.SOUTH);

        add(centerWrap, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(UITheme.COLOR_PANEL_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton btnCancel = UITheme.createButton("Cancel", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UITheme.createButton("Update Password", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnSave.addActionListener(e -> handleUpdatePassword());

        footer.add(btnCancel);
        footer.add(btnSave);
        add(footer, BorderLayout.SOUTH);
    }

    private void handleUpdatePassword() {
        if (targetUser == null) {
            lblStatus.setText("User account not found.");
            return;
        }

        String curPass = new String(tfCurrentPass.getPassword());
        String newPass = new String(tfNewPass.getPassword());
        String confirmPass = new String(tfConfirmPass.getPassword());

        if (curPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            lblStatus.setText("All fields are required.");
            return;
        }

        // Verify current password
        String hashedCurrent = AuthService.hashPassword(curPass, targetUser.getSalt());
        if (!hashedCurrent.equals(targetUser.getPasswordHash())) {
            lblStatus.setText("Current password is incorrect.");
            tfCurrentPass.setText("");
            tfCurrentPass.requestFocus();
            return;
        }

        if (newPass.length() < 4) {
            lblStatus.setText("New password must be at least 4 characters long.");
            tfNewPass.requestFocus();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            lblStatus.setText("New password and confirmation do not match.");
            tfConfirmPass.setText("");
            tfConfirmPass.requestFocus();
            return;
        }

        try {
            authService.changePassword(targetUser.getUsername(), newPass);
            JOptionPane.showMessageDialog(this,
                    "Password updated successfully for " + targetUser.getUsername() + "!\nPlease use your new password next time you log in.",
                    "Password Changed",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            lblStatus.setText(ex.getMessage());
        }
    }
}
