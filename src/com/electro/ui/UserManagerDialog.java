package com.electro.ui;

import com.electro.model.User;
import com.electro.service.AuthService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Administrative dialog to manage staff accounts, roles, full names, and passwords.
 */
public class UserManagerDialog extends JDialog {
    private final AuthService authService;
    private final Runnable onUserUpdated;
    private JTable userTable;
    private DefaultTableModel userTableModel;

    public UserManagerDialog(Window parent) {
        this(parent, null);
    }

    public UserManagerDialog(Window parent, Runnable onUserUpdated) {
        super(parent, "Staff & User Account Management", ModalityType.APPLICATION_MODAL);
        this.authService = AuthService.getInstance();
        this.onUserUpdated = onUserUpdated;

        initUI();
    }

    private void initUI() {
        setSize(740, 500);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UITheme.COLOR_BG);

        UITheme.applyAppIcon(this);

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 2, 2));
        header.setBackground(UITheme.COLOR_PRIMARY_DARK);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel("Staff Accounts & Access Control");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Create cashiers, assign roles, edit names, and manage credentials", SwingConstants.LEFT);
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(new Color(203, 213, 225));

        header.add(title);
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        // Table
        String[] cols = {"Username", "Full Name", "Role", "Created Date"};
        userTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        userTable = new JTable(userTableModel);
        UITheme.styleTable(userTable);

        // Double-click row to edit full name
        userTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && userTable.getSelectedRow() >= 0) {
                    showEditFullNameDialog();
                }
            }
        });

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(UITheme.COLOR_PANEL_BG);
        tableWrapper.setBorder(new CompoundBorder(
                new EmptyBorder(10, 16, 6, 16),
                new LineBorder(UITheme.COLOR_BORDER, 1, true)
        ));
        tableWrapper.add(new JScrollPane(userTable), BorderLayout.CENTER);
        add(tableWrapper, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(UITheme.COLOR_PANEL_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton btnAdd = UITheme.createButton("+ Add New User", UITheme.COLOR_SUCCESS, Color.WHITE);
        btnAdd.addActionListener(e -> showAddUserDialog());

        JButton btnEditName = UITheme.createButton("Edit Full Name", new Color(14, 165, 233), Color.WHITE);
        btnEditName.setToolTipText("Edit the displayed Full Name of the selected cashier or staff member");
        btnEditName.addActionListener(e -> showEditFullNameDialog());

        JButton btnChangePass = UITheme.createButton("Change Password", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnChangePass.addActionListener(e -> handleChangePassword());

        JButton btnDelete = UITheme.createButton("Delete User", UITheme.COLOR_DANGER, Color.WHITE);
        btnDelete.addActionListener(e -> handleDeleteUser());

        JButton btnClose = UITheme.createButton("Close", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnClose.addActionListener(e -> dispose());

        footer.add(btnAdd);
        footer.add(btnEditName);
        footer.add(btnChangePass);
        footer.add(btnDelete);
        footer.add(btnClose);
        add(footer, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        userTableModel.setRowCount(0);
        List<User> list = authService.getAllUsers();
        for (User u : list) {
            userTableModel.addRow(new Object[]{
                    u.getUsername(),
                    u.getFullName(),
                    u.getRole().name().replace('_', ' '),
                    u.getCreatedAt()
            });
        }
    }

    private void showEditFullNameDialog() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a staff user from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String username = (String) userTableModel.getValueAt(row, 0);
        User user = authService.getUserByUsername(username);
        if (user == null) {
            JOptionPane.showMessageDialog(this, "User not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog(this, "Edit Staff Full Name", ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 240);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.getContentPane().setBackground(UITheme.COLOR_BG);
        UITheme.applyAppIcon(dlg);

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 14));
        form.setBackground(UITheme.COLOR_PANEL_BG);
        form.setBorder(new EmptyBorder(20, 24, 16, 24));

        JTextField tfUser = UITheme.createTextField(15);
        tfUser.setText(user.getUsername());
        tfUser.setEditable(false);
        tfUser.setBackground(new Color(241, 245, 249));

        JTextField tfRole = UITheme.createTextField(15);
        tfRole.setText(user.getRole().name().replace('_', ' '));
        tfRole.setEditable(false);
        tfRole.setBackground(new Color(241, 245, 249));

        JTextField tfName = UITheme.createTextField(15);
        tfName.setText(user.getFullName());

        JLabel lblU = new JLabel("Username:");
        lblU.setFont(UITheme.FONT_REGULAR_BOLD);
        form.add(lblU);
        form.add(tfUser);

        JLabel lblR = new JLabel("Role:");
        lblR.setFont(UITheme.FONT_REGULAR_BOLD);
        form.add(lblR);
        form.add(tfRole);

        JLabel lblN = new JLabel("Full Name:");
        lblN.setFont(UITheme.FONT_REGULAR_BOLD);
        form.add(lblN);
        form.add(tfName);

        dlg.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        actions.setBackground(UITheme.COLOR_PANEL_BG);
        actions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton btnCancel = UITheme.createButton("Cancel", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnSave = UITheme.createButton("Save Changes", UITheme.COLOR_SUCCESS, Color.WHITE);
        btnSave.addActionListener(e -> {
            String newFullName = tfName.getText().trim();
            if (newFullName.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Full Name cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                authService.updateFullName(user.getUsername(), newFullName);
                refreshTable();
                dlg.dispose();
                JOptionPane.showMessageDialog(this, "Full Name for '" + user.getUsername() + "' updated successfully to '" + newFullName + "'!", "Success", JOptionPane.INFORMATION_MESSAGE);

                if (onUserUpdated != null) {
                    onUserUpdated.run();
                }

                // If currently logged in user was modified, refresh MainFrame user badge
                Window ancestor = SwingUtilities.getWindowAncestor(this);
                if (ancestor instanceof MainFrame) {
                    ((MainFrame) ancestor).updateForCurrentUser();
                } else if (getOwner() instanceof MainFrame) {
                    ((MainFrame) getOwner()).updateForCurrentUser();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        actions.add(btnCancel);
        actions.add(btnSave);
        dlg.add(actions, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void showAddUserDialog() {
        JDialog dlg = new JDialog(this, "Add New Staff User", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(380, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.getContentPane().setBackground(UITheme.COLOR_BG);
        UITheme.applyAppIcon(dlg);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 10));
        form.setBackground(UITheme.COLOR_PANEL_BG);
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField tfUser = UITheme.createTextField(15);
        JTextField tfName = UITheme.createTextField(15);
        JPasswordField tfPass = new JPasswordField(15);
        JComboBox<User.Role> comboRole = new JComboBox<>(User.Role.values());
        comboRole.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof User.Role) {
                    setText(((User.Role) value).name().replace('_', ' '));
                }
                return this;
            }
        });

        form.add(new JLabel("Username:")); form.add(tfUser);
        form.add(new JLabel("Full Name:")); form.add(tfName);
        form.add(new JLabel("Password:")); form.add(tfPass);
        form.add(new JLabel("Role:")); form.add(comboRole);

        dlg.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actions.setBackground(UITheme.COLOR_PANEL_BG);
        actions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton btnCancel = UITheme.createButton("Cancel", UITheme.COLOR_BORDER, UITheme.COLOR_TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnSave = UITheme.createButton("Create User", UITheme.COLOR_PRIMARY, Color.WHITE);
        btnSave.addActionListener(e -> {
            try {
                String u = tfUser.getText().trim();
                String n = tfName.getText().trim();
                String p = new String(tfPass.getPassword());
                User.Role r = (User.Role) comboRole.getSelectedItem();

                if (u.isEmpty() || p.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Username and password are required.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                authService.createUser(u, p, n.isEmpty() ? u : n, r);
                dlg.dispose();
                refreshTable();
                if (onUserUpdated != null) {
                    onUserUpdated.run();
                }
                JOptionPane.showMessageDialog(this, "User '" + u + "' created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        actions.add(btnCancel);
        actions.add(btnSave);
        dlg.add(actions, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void handleChangePassword() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String username = (String) userTableModel.getValueAt(row, 0);
        User user = authService.getUserByUsername(username);
        if (user != null) {
            ChangePasswordDialog dlg = new ChangePasswordDialog(this, user);
            dlg.setVisible(true);
        }
    }

    private void handleDeleteUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String username = (String) userTableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete user account '" + username + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                authService.deleteUser(username);
                refreshTable();
                if (onUserUpdated != null) {
                    onUserUpdated.run();
                }
                JOptionPane.showMessageDialog(this, "User deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Action Blocked", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
