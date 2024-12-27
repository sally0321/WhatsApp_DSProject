import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class VideoCallServer {
    private static final int VIDEO_CALL_SERVER_PORT = 1112;
    public static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(VIDEO_CALL_SERVER_PORT)) {
            System.out.println("Server is running and waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println(" ");
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        System.out.println("User " + clientHandler.getUsername() + " disconnected. Active clients: " + clients.size());
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private ClientHandler inCall;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                System.out.println("Exiting...");
            }
        }

        @Override
        public void run() {
            try {
                // The username is sent immediately after the connection is established
                username = in.readLine();
                System.out.println("User " + username + " connected.");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("CALL")) {
                        handleCallRequest(inputLine.split(" ")[1]);
                    } else if (inputLine.equalsIgnoreCase("Y") || inputLine.equalsIgnoreCase("N")) {
                        handleCallResponse(inputLine);
                    } else if (inputLine.equals("0")) {
                        handleCallEnd();
                    } else if (inputLine.equals("exit")) {
                        System.out.println("User " + username + " has exited.");
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Exiting...");
            } finally {
                cleanupResources();
            }
        }

        private void handleCallRequest(String targetUser) {
            for (ClientHandler client : VideoCallServer.clients) {
                if (client.getUsername().equals(targetUser)) {
                    client.out.println("\n" + username + " is calling you.\nY = Receive\nN = Reject.\n");
                    client.inCall = this;
                    this.inCall = client;
                    return;
                }
            }
            out.println("User " + targetUser + " is not online.");

        }

        private void handleCallResponse(String input) {
            if (inCall != null) {
                if (input.equalsIgnoreCase("Y")) { // Call accepted
                    inCall.out.println("Call accepted by " + username + ". Starting video call... Press 0 to end.");
                    out.println("Call with " + inCall.getUsername() + " connected. Press 0 to end.");
                    inCall.inCall = this; // Set inCall for Client A
                    this.inCall = inCall; // Set inCall for Client B
                } else if (input.equalsIgnoreCase("N")) { // Call rejected
                    inCall.out.println(username + " rejected your call.");
                    out.println("You rejected the call from " + inCall.getUsername() + ".");
                    inCall.inCall = null; // Clear inCall for Client A
                    inCall = null; // Clear inCall for Client B

                }
            }
        }

        private void handleCallEnd() {
            if (inCall != null) {
                String caller = inCall.getUsername(); // The other client
                String recipient = username;         // This client

                // Notify the other client
                inCall.out.println("Call ended by " + recipient + ".");
                inCall.inCall = null; // Clear inCall state for the other client
                inCall = null; // Clear inCall state for this client
                System.out.println("\nMenu:");
                System.out.println("1 - Make a Video Call");
                System.out.println("2 - Quit");
                System.out.println("Call End: Caller = " + caller + ", Ended by = " + recipient);
            }
        }

        private void cleanupResources() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                VideoCallServer.removeClient(this);
            } catch (IOException e) {
                System.out.println("Exiting...");
            }
        }

        public String getUsername() {
            return username;
        }
    }
}