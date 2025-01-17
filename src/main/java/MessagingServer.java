
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Map;

public class MessagingServer {
    private static final int PORT = 1110;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static Timestamp timestamp = null;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is running and waiting for connections..");

            // Accept incoming connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Create a new client handler for the connected client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Send message to the intended recipient
    public static void sendMessage(String message, ClientHandler sender, String recipient) {
        timestamp = new Timestamp(System.currentTimeMillis());
        String time = timeFormat.format(timestamp);
        String lastUser = getLastUser(sender.getUsername(), recipient);

        // add space between messages sent by different users for easier reading
        if (lastUser.equals(sender.getUsername())) {
            message = "[" + time + "]\n" + message;
        } else{
            message = "\n[" + time + "]\n" + message;
        }

        Integer newMessagesCount = DatabaseServer.getContacts(recipient).get(sender.getUsername());
        newMessagesCount ++;
        DatabaseServer.updateMessageStatus(recipient, sender.getUsername(), newMessagesCount);

        // Save the message to the file for this conversation
        DatabaseServer.saveMessage(sender.getUsername(), recipient, message);
        String chat = DatabaseServer.getChatHistory(sender.getUsername(), recipient);

        for (ClientHandler client : clients) {
            if (client != sender && sender.getUsername().equals(client.getRecipient())) {
                // print whole chat on recipient side
                newMessagesCount = 0;
                client.out.println(chat);
                client.out.println("Type your message (Type exit to exit chat): ");
                DatabaseServer.updateMessageStatus(recipient, sender.getUsername(), newMessagesCount);
            }
            // print whole chat on sender side
            sender.out.println(chat);
        }
    }

    // Get the user who sent last message
    public static String getLastUser(String sender, String recipient){
        String chat = DatabaseServer.getChatHistory(sender, recipient);

        try {
            String[] lines = chat.split("\n");
            String[] lastMessage = lines[lines.length - 1].split("[\\[\\]]");
            return lastMessage[1];

        } catch (ArrayIndexOutOfBoundsException e) {
            return sender;
        }
    }

    // Internal class to handle client connections
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private String recipient;

        // Constructor
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;

            try {
                // Create input and output streams for communication
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Releases resources associated with the client connection.
        private void cleanupResources(){
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                System.out.println("Connection with " + username + " closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void promptAddContact() throws IOException {
            try {
                ArrayList<String> users = DatabaseServer.getUsers();

                while (true) {
                    out.println("Enter user to add (Enter 0 to cancel):");
                    String input = in.readLine();

                    if (input.equals("0")) {
                        if (DatabaseServer.getContacts(username).isEmpty()) {
                            out.println("Contact list empty, please add a new contact.\n");
                        } else {
                            break;
                        }
                    } else if (users.contains(input)) {
                        if (DatabaseServer.getContacts(username).containsKey(input)) {
                            out.println("Contact already exists.\n");
                        } else {
                            DatabaseServer.saveContact(username, input);
                            out.println("User added to contacts.\n");
                        }
                        break;
                    } else {
                        out.println("User does not exist.\n");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                throw e; // Re-throwing exception if needed for external handling
            }
        }

        private void showContactList() {
            Map<String, Integer> contacts = (DatabaseServer.getContacts(username));
            out.println(username + "'s contact list:");

            for (String contact : contacts.keySet()) {
                out.println(contact + " [" + contacts.get(contact) + " new messages]");
            }

            out.println();
        }

        private void promptRecipient() throws IOException {
            try {
                while (true) {
                    out.println("1 - Add new contact");
                    out.println("Back - Return to Menu");
                    out.println("Enter the person you want to chat with:");
                    String input = in.readLine();

                    if (input.equals("1")) {
                        promptAddContact();
                        showContactList();
                    } else if (input.equalsIgnoreCase("back")) {
                        recipient = input;
                        break;
                    }
                    else if (!DatabaseServer.getContacts(username).containsKey(input)) {
                        out.println("Please enter a valid contact.");
                    } else {
                        recipient = input;
                        DatabaseServer.updateMessageStatus(username, recipient,0);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw e; // Re-throw the exception for further handling if needed
            }
        }

        public String getUsername() {
            return username;
        }

        public String getRecipient() {
            return recipient;
        }

        public void promptMessage() throws IOException {
            try {
                String input;

                while (true) {
                out.println("Type your message (Type exit to exit chat): ");
                // Send the message
                input = in.readLine();
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
                sendMessage("[" + username + "]: " + input, this, recipient);
                }

            } catch (IOException e) {
                System.err.println("Error in communication with user " + username + ": " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                // Get the username from the client
                username = in.readLine();
                System.out.println("User " + username + " connected.");

                while (true) {
                    Map<String, Integer> contacts = (DatabaseServer.getContacts(username));

                    if (contacts.isEmpty()) {
                        out.println("No contacts found.");
                        promptAddContact();
                    }

                    showContactList();

                    promptRecipient();

                    if (recipient.equalsIgnoreCase("back")) {
                        break;
                    }

                    else {
                        out.println();
                        out.println("\n" + DatabaseServer.getChatHistory(username, recipient));

                        // Prompt client for message
                        promptMessage();
                    }
                }

                clients.remove(this);
                System.out.println("User " + username + " has exited. Active clients: " + clients.size());
            } catch (IOException e) {
                System.err.println("Error in communication with user " + username + ": " + e.getMessage());
            } finally {
                cleanupResources();
            }

        }
    }
}
