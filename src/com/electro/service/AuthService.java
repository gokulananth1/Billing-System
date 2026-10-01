package com.electro.service;

import com.electro.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;

/**
 * Service managing staff authentication, salted SHA-256 password hashing,
 * user sessions, and role-based permissions.
 */

public class AuthService {
    private static AuthService instance;

    private final Path usersFile;
    private final Map<String, User> users = new LinkedHashMap<>();
    private User currentUser;

    private AuthService() {
        Path dataDir = Paths.get("data");
        this.usersFile = dataDir.resolve("users.json");

        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadUsers();
        if (users.isEmpty()) {
            seedDefaultUsers();
            saveUsers();
        }
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    // --- AUTHENTICATION & SESSION ---

    public synchronized boolean login(String username, String password) {
        if (username == null || password == null) return false;
        User user = users.get(username.trim().toLowerCase());
        if (user == null) return false;

        String hashed = hashPassword(password, user.getSalt());
        if (hashed.equals(user.getPasswordHash())) {
            this.currentUser = user;
            return true;
        }
        return false;
    }

    public synchronized void logout() {
        this.currentUser = null;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public synchronized boolean loginAsDefaultOwner() {
        User owner = users.values().stream()
                .filter(User::isStoreOwner)
                .findFirst()
                .orElse(null);
        if (owner == null) {
            owner = users.get("owner");
        }
        if (owner == null) {
            owner = users.get("admin");
        }
        if (owner != null) {
            this.currentUser = owner;
            return true;
        }
        return false;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    // --- USER MANAGEMENT ---

    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized User getUserByUsername(String username) {
        if (username == null) return null;
        return users.get(username.trim().toLowerCase());
    }

    public synchronized void createUser(String username, String password, String fullName, User.Role role) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password cannot be empty");
        }

        if (role == User.Role.ADMIN) {
            throw new IllegalArgumentException("Restriction: Creating user accounts with the ADMIN role is strictly forbidden.");
        }

        String uname = username.trim().toLowerCase();
        if (users.containsKey(uname)) {
            throw new IllegalArgumentException("Username '" + uname + "' already exists");
        }

        String salt = UUID.randomUUID().toString().substring(0, 8);
        String hash = hashPassword(password, salt);
        String now = LocalDate.now().toString();

        User newUser = new User(uname, hash, salt, fullName, role != null ? role : User.Role.CASHIER, now);
        users.put(uname, newUser);
        saveUsers();
    }

    public synchronized void changePassword(String username, String newPassword) {
        if (username == null || newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        User user = users.get(username.trim().toLowerCase());
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        String newSalt = UUID.randomUUID().toString().substring(0, 8);
        user.setSalt(newSalt);
        user.setPasswordHash(hashPassword(newPassword, newSalt));
        saveUsers();
    }

    public synchronized void updateFullName(String username, String newFullName) {
        if (username == null || newFullName == null || newFullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and full name cannot be empty");
        }
        User user = users.get(username.trim().toLowerCase());
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        user.setFullName(newFullName.trim());
        if (currentUser != null && currentUser.getUsername().equalsIgnoreCase(username.trim())) {
            currentUser.setFullName(newFullName.trim());
        }
        saveUsers();
    }

    public synchronized boolean deleteUser(String username) {
        if (username == null) return false;
        String uname = username.trim().toLowerCase();

        // Prevent deleting the currently logged-in user
        if (currentUser != null && currentUser.getUsername().equalsIgnoreCase(uname)) {
            throw new IllegalStateException("Cannot delete the currently logged in user.");
        }

        User target = users.get(uname);
        if (target != null && target.isAdmin()) {
            throw new IllegalStateException("Restriction: User accounts with the ADMIN role cannot be deleted.");
        }

        boolean removed = users.remove(uname) != null;
        if (removed) {
            saveUsers();
        }
        return removed;
    }

    // --- CRYPTO HELPER ---

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = (salt != null ? salt : "") + password;
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    // --- SEEDING & PERSISTENCE ---

    private void seedDefaultUsers() {
        String adminSalt = "a1b2c3d4";
        users.put("admin", new User(
                "admin",
                hashPassword("admin123", adminSalt),
                adminSalt,
                "Gokul Admin (Developer)",
                User.Role.ADMIN,
                LocalDate.now().toString()
        ));

        String ownerSalt = "f1e2d3c4";
        users.put("owner", new User(
                "owner",
                hashPassword("owner123", ownerSalt),
                ownerSalt,
                "Store Owner",
                User.Role.STORE_OWNER,
                LocalDate.now().toString()
        ));

        String cashierSalt = "e5f6g7h8";
        users.put("cashier", new User(
                "cashier",
                hashPassword("cashier123", cashierSalt),
                cashierSalt,
                "Sam Cashier (Staff)",
                User.Role.CASHIER,
                LocalDate.now().toString()
        ));
    }

    private void loadUsers() {
        if (!Files.exists(usersFile)) return;
        try {
            String json = Files.readString(usersFile, StandardCharsets.UTF_8);
            SimpleJson.JsonValue val = SimpleJson.parse(json);
            users.clear();

            for (SimpleJson.JsonValue item : val.asArray()) {
                String roleStr = item.get("role").asString("CASHIER").toUpperCase();
                User.Role role = User.Role.CASHIER;
                try {
                    role = User.Role.valueOf(roleStr);
                } catch (Exception ignored) {}

                User u = new User(
                        item.get("username").asString(""),
                        item.get("passwordHash").asString(""),
                        item.get("salt").asString(""),
                        item.get("fullName").asString(""),
                        role,
                        item.get("createdAt").asString("")
                );
                if (!u.getUsername().isEmpty()) {
                    users.put(u.getUsername().toLowerCase(), u);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            int i = 0;
            for (User u : users.values()) {
                if (i > 0) sb.append(",\n");
                sb.append("  {\n");
                sb.append("    \"username\": \"").append(SimpleJson.escape(u.getUsername())).append("\",\n");
                sb.append("    \"passwordHash\": \"").append(SimpleJson.escape(u.getPasswordHash())).append("\",\n");
                sb.append("    \"salt\": \"").append(SimpleJson.escape(u.getSalt())).append("\",\n");
                sb.append("    \"fullName\": \"").append(SimpleJson.escape(u.getFullName())).append("\",\n");
                sb.append("    \"role\": \"").append(u.getRole().name()).append("\",\n");
                sb.append("    \"createdAt\": \"").append(SimpleJson.escape(u.getCreatedAt())).append("\"\n");
                sb.append("  }");
                i++;
            }
            sb.append("\n]\n");
            Files.writeString(usersFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
