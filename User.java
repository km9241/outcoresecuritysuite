import java.io.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class User {
    private static final String USER_DATA_FILE = "users.dat";
    private static Map<String, String> users = new HashMap<>();

    static {
        loadUsers();
    }

    public static boolean register(String username, String password) {
        if (users.containsKey(username)) {
            return false;
        }

        String passwordHash = hashPassword(password);
        users.put(username, passwordHash);
        saveUsers();
        return true;
    }

    public static boolean login(String username, String password) {
        if (!users.containsKey(username)) {
            return false;
        }

        String storedHash = users.get(username);
        String inputHash = hashPassword(password);
        return storedHash.equals(inputHash);
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USER_DATA_FILE))) {
            users = (Map<String, String>) ois.readObject();
        } catch (FileNotFoundException e) {
            users = new HashMap<>();
        } catch (Exception e) {
            System.out.println("Error loading users: " + e.getMessage());
            users = new HashMap<>();
        }
    }

    private static void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_DATA_FILE))) {
            oos.writeObject(users);
        } catch (Exception e) {
            System.out.println("Error saving users: " + e.getMessage());
        }
    }

    public static boolean userExists(String username) {
        return users.containsKey(username);
    }
}