import java.io.*;
import java.net.*;
import java.util.Scanner;

public class UserClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1110;
    private static Scanner scanner = new Scanner(System.in);
    private static PrintWriter out;
    private static BufferedReader in;
    private static Socket socket;

    public static void main(String[] args) {
        try {
            //Connect to the server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //Start a separate thread to handle server responses
            new Thread(UserClient::handleServerResponses).start();

            //Main loop for user input
            while (true) {
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
                out.println(userInput);
            }

        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private static void handleServerResponses() {
        try {
            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                //For verification codes sent to "main client"
                if (serverResponse.contains("Verification code for linking device:")) {
                    System.out.println("\n=== IMPORTANT NOTIFICATION ===");
                    System.out.println(serverResponse);
                    System.out.println("===============================");
                } else {
                    System.out.println(serverResponse);
                }
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.out.println("Lost connection to server: " + e.getMessage());
            }
        }
    }

    private static void cleanup() {
        try {
            if (scanner != null) scanner.close();
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Error during cleanup: " + e.getMessage());
        }
    }
}