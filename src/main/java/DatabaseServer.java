import java.io.*;
import java.util.ArrayList;

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
            writer.write(contact);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Function to print the content of the chat history file
    public static String getChatHistory(String userA, String userB) {
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

    public static ArrayList<String> getContacts(String user) {
        File file = new File(CONTACT_DIRECTORY + user + ".txt");
        ArrayList<String> contacts = new ArrayList<>();

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
                contacts.add(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    public static ArrayList<String> getUsers() {
        File file = new File(USER_FILE);
        ArrayList<String> users = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();

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

}
