
import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

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

        // Save the message to the file for this conversation
        DatabaseServer.saveMessage(sender.getUsername(), recipient, message);
        String chat = DatabaseServer.getChatHistory(sender.getUsername(), recipient);

        for (ClientHandler client : clients) {
            if (client != sender && sender.getUsername().equals(client.getRecipient())) {
                // print whole chat on recipient side
                client.out.println(chat);
                client.out.println("Type your message: ");
            }

            // print whole chat on sender side
            sender.out.println(chat);
            sender.out.println("Type your message: ");
        }

    }

    // Get the user who sent last message
    public static String getLastUser(String sender, String recipient){
        String chat = DatabaseServer.getChatHistory(sender, recipient);
        String[] lines = chat.split("\n");
        String[] lastMessage = lines[lines.length - 1].split("[\\[\\]]");
        return lastMessage[1];
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

        @Override
        public void run() {
            try {
                // Get the username from the client
                username = in.readLine();
                System.out.println("User " + username + " connected.");

                out.println("Welcome to the chat, " + username + "!");

                recipient = promptRecipient();
                out.println(DatabaseServer.getChatHistory(username, recipient));

                out.println("Type your message: ");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    // System.out.println("[" + username + "]: " + inputLine);
                    // System.out.println(clients);
                    if (inputLine.equalsIgnoreCase("exit")) {
                        // Remove the client handler from the list
                        clients.remove(this);
                        System.out.println("User " + username + " has exited. Active clients: " + clients.size());
                        break; // Exit the loop
                    }
                    // Send the message
                    sendMessage("[" + username + "]: " + inputLine, this, recipient);
                }
            } catch (IOException e) {
                System.err.println("Error in communication with user " + username + ": " + e.getMessage());
            } finally {
                cleanupResources();
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

        private String promptRecipient() throws IOException {
            out.println("Enter recipient name:");
            return in.readLine();
        }

        public String getUsername() {
            return username;
        }

        public String getRecipient() {
            return recipient;
        }
    }
}
