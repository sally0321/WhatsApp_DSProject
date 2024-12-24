
import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessagingServer {
    private static final int PORT = 1234;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

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
        for (ClientHandler client : clients) {
            if (client != sender && sender.getUsername().equals(client.getRecipient())) {
                client.out.println(message);
                break;
            }
        }
        // Save the message to the file for this conversation
        DatabaseServer.saveMessage(sender.getUsername(), recipient, message);
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
