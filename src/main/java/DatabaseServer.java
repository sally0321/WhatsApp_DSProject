import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    public static void updateMessageStatus(String recipient, String sender, int messageCount){
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

}
