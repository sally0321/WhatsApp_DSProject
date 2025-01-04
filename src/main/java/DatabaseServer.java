import java.io.*;
import java.util.*;

public class DatabaseServer {

    // Constants for directory paths
    private static final String CMD_DIRECTORY = "../resources/chat_history/";
    private static final String CHAT_DIRECTORY = "../resources/chat_history/";
    private static final String CONTACT_DIRECTORY = "../resources/contact_list/";
    private static final String USER_FILE = "../resources/user_list.txt";
    private static final String MVN_DIRECTORY = "src/main/resources/chat_history/";
    private static final String DIRECTORY = CMD_DIRECTORY; // Change as needed

    // Save the message in a text file named after the users
    public static void saveMessage(String userA, String userB, String message) {
        String fileName = getSortedFileName(userA, userB);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CHAT_DIRECTORY + fileName, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveContact(String user, String contact) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONTACT_DIRECTORY + user + ".txt", true))) {
            writer.write(contact + ",0");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONTACT_DIRECTORY + contact + ".txt", true))) {
            writer.write(user + ",0");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateMessageStatus(String recipient, String sender, Integer messageCount){
        Map<String,Integer> contacts = DatabaseServer.getContacts(recipient);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONTACT_DIRECTORY + recipient + ".txt"))) {
            contacts.put(sender, messageCount);
            for (String contact : contacts.keySet()) {
                writer.write(contact + "," + contacts.get(contact));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to print the content of the chat history file
    public static String getChatHistory(String userA, String userB) {
        System.out.println(userA + " " + userB);

        String fileName = getSortedFileName(userA, userB);
        String chat = "";

        // Create the full file path
        File file = new File(CHAT_DIRECTORY + fileName);

        // Check if the file exists
        if (!file.exists()) {
            try{
                new FileWriter(CHAT_DIRECTORY + fileName, true);
                return ("New chat created for " + userA + " and " + userB);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line == null) {
                // If the first line is null, the chat is new
                return "New chat created for " + userA + " and " + userB;
            }
            else{
                chat += ("Chat history between " + userA + " and " + userB + ":\n");
                while ((line = reader.readLine()) != null) {
                    chat = chat + line + '\n';
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chat;
    }

    public static Map<String, Integer> getContacts(String user) {
        File file = new File(CONTACT_DIRECTORY + user + ".txt");
        Map<String,Integer> contacts = new HashMap<String,Integer>();

        // Check if the file exists
        if (!file.exists()) {
            try{
                new FileWriter(CONTACT_DIRECTORY + user + ".txt", true);
                return contacts;
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] contactPair = line.split(",");
                contacts.put(contactPair[0], Integer.parseInt(contactPair[1]));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e){
            return contacts;
        }
        return contacts;
    }

    public static ArrayList<String> getUsers() {
        File file = new File(USER_FILE);
        ArrayList<String> users = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                users.add(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Ensure the file name is in sorted order (userA_userB.txt)
    private static String getSortedFileName(String userA, String userB) {
        return userA.compareTo(userB) < 0 ? userA + "_" + userB + ".txt" : userB + "_" + userA + ".txt";
    }

    // User Authentication Methods

    // Registers a new user. Returns true if successful, false if phone number already exists.
    public static boolean registerUser(String phoneNumber, String username) {
        Map<String, String> users = getUserMap();
        if (users.containsKey(phoneNumber)) {
            return false; // Phone number already exists
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_FILE, true))) {
            writer.write(phoneNumber + "," + username);
            writer.newLine();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Verifies user credentials. Returns true if phone number and username match.
    public static boolean verifyUser(String phoneNumber, String username) {
        Map<String, String> users = getUserMap();
        return username.equals(users.get(phoneNumber));
    }

    // Deletes a user account. Returns true if successful, false otherwise.
    public static boolean deleteUser(String phoneNumber, String username) {
        Map<String, String> users = getUserMap();
        if (!username.equals(users.get(phoneNumber))) {
            return false; // User not found
        }

        // Remove user from user_list.txt
        List<String> updatedUsers = new ArrayList<>();
        for (Map.Entry<String, String> entry : users.entrySet()) {
            if (!entry.getKey().equals(phoneNumber)) {
                updatedUsers.add(entry.getKey() + "," + entry.getValue());
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_FILE))) {
            for (String user : updatedUsers) {
                writer.write(user);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Optionally, delete user's chat history and contacts
        // deleteUserData(username);

        return true;
    }

    // Helper method to get a map of phoneNumber -> username
    private static Map<String, String> getUserMap() {
        Map<String, String> users = new HashMap<>();
        File file = new File(USER_FILE);

        // If the user file doesn't exist, create it
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return users;
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Optionally, delete user's chat history and contacts
    private static void deleteUserData(String username) {
        // Delete chat history files
        File chatDir = new File(CHAT_DIRECTORY);
        File[] chatFiles = chatDir.listFiles((dir, name) -> name.contains(username));
        if (chatFiles != null) {
            for (File file : chatFiles) {
                file.delete();
            }
        }

        // Delete contact list
        File contactFile = new File(CONTACT_DIRECTORY + username + ".txt");
        if (contactFile.exists()) {
            contactFile.delete();
        }

        // Additionally, remove this user from other users' contact lists
        File contactsDir = new File(CONTACT_DIRECTORY);
        File[] contactFiles = contactsDir.listFiles((dir, name) -> name.endsWith(".txt"));
        if (contactFiles != null) {
            for (File file : contactFiles) {
                try {
                    List<String> lines = new ArrayList<>();
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith(username + ",")) {
                            lines.add(line);
                        }
                    }
                    reader.close();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    for (String l : lines) {
                        writer.write(l);
                        writer.newLine();
                    }
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
