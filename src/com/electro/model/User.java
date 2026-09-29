package com.electro.model;

/**
 * Represents an authenticated staff user (Admin, Cashier).
 */
public class User {
    public enum Role {
        ADMIN,
        STORE_OWNER,
        CASHIER
    }

    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private Role role;
    private String createdAt;

    public User() {}

    public User(String username, String passwordHash, String salt, String fullName, Role role, String createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.fullName = fullName;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStoreOwner() {
        return role == Role.STORE_OWNER;
    }

    public boolean isCashier() {
        return role == Role.CASHIER;
    }

    public boolean canAccessAnalytics() {
        return role == Role.ADMIN || role == Role.STORE_OWNER;
    }

    public boolean canAccessSettings() {
        return role == Role.ADMIN;
    }

    @Override
    public String toString() {
        return fullName + " (" + username + ") [" + role + "]";
    }
}
