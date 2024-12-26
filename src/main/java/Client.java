import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1110;
    private static final int PROFILE_SERVER_PORT = 1111;
    private static final int VIDEO_CALL_SERVER_PORT = 1112;

    private static Scanner scanner = new Scanner(System.in);
    private static String input;
    private static String username;

    public static void main(String[] args) {
        promptUsername();
        while (true) {
            promptInput();
            if (input.equals("exit")) {
                break;
            }

            switch (input) {
                case "1": {
                    try {
                        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                        System.out.println("\nConnected to the chat server!");

                        // Setting up input and output streams
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        // send username to server
                        out.println(username);

                        // Start a thread to handle incoming messages
                        new Thread(() -> {
                            try {
                                String serverResponse;
                                while ((serverResponse = in.readLine()) != null) {
                                    System.out.println(serverResponse);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();

                        // Read messages from the console and send to the server
                        Scanner scanner = new Scanner(System.in);
                        String userInput;
                        while (true) {
                            userInput = scanner.nextLine();
                            out.println(userInput);
                            if (userInput.equals("exit")) {
                                break;
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case "2": {
                    profileSettings();
                    break;
                }
                case "3": // Video calling functionality
                    startVideoCall();
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void promptInput() {
        System.out.println();
        System.out.println("Menu:");
        System.out.println("1 - Messaging");
        System.out.println("2 - Profile Settings");
        System.out.println("3 - VideoCall");
        System.out.println("exit - Quit WhatsApp");
        input = scanner.nextLine();
    }

    private static void promptUsername() {
        System.out.println("Enter your username:");
        username = scanner.nextLine();
    }

    private static void profileSettings() {
        while (true) {
            System.out.println("\n-- Profile Settings --");
            System.out.println("1 - View Profile");
            System.out.println("2 - Edit Username");
            System.out.println("3 - Edit Bio");
            System.out.println("back - Go Back");

            String input = scanner.nextLine();
            if (input.equals("back")) {
                break;
            }

            switch (input) {
                case "1":
                    handleProfileCommand("VIEW_PROFILE", null, null);
                    break;
                case "2":
                    System.out.print("Enter new username: ");
                    String newUsername = scanner.nextLine();
                    handleProfileCommand("EDIT_USERNAME", newUsername, null);
                    break;
                case "3":
                    System.out.print("Enter new bio: ");
                    String newBio = scanner.nextLine();
                    handleProfileCommand("EDIT_BIO", null, newBio);
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void handleProfileCommand(String command, String newUsername, String newBio) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PROFILE_SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send the command
            out.println(command);
            out.println(username);

            if ("EDIT_USERNAME".equals(command)) {
                out.println(newUsername);
            } else if ("EDIT_BIO".equals(command)) {
                out.println(newBio);
            }

            // Receive and display server response
            String response = in.readLine();
            System.out.println(response);

            // Update local username if successful
            if ("EDIT_USERNAME".equals(command) && response.contains("successfully")) {
                username = newUsername;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startVideoCall() {
        try (Socket socket = new Socket(SERVER_ADDRESS, VIDEO_CALL_SERVER_PORT)) {
            System.out.println("Connected to the video call server!");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send the username directly without prompting again
            out.println(username);

            AtomicBoolean awaitingResponse = new AtomicBoolean(false);
            AtomicBoolean inCall = new AtomicBoolean(false);
            AtomicBoolean running = new AtomicBoolean(true);

            // Thread to listen for server responses
            new Thread(() -> {
                try {
                    String serverMessage;
                    while (running.get() && (serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);

                        // Handle "not online" message
                        if (serverMessage.contains("is not online")) {
                            awaitingResponse.set(false);
                            inCall.set(false);
                            System.out.println("Target user is not online, resetting states...");
                            System.out.println("\nMenu:");
                            System.out.println("1 - Make a Video Call");
                            System.out.println("2 - Quit");
                            continue;
                        }

                        // Handle incoming call notification
                        if (serverMessage.contains("is calling you")) {
                            awaitingResponse.set(true);
                        }

                        // Handle call accepted
                        if (serverMessage.contains("Starting video call...")) {
                            awaitingResponse.set(false);
                            inCall.set(true);
                        }

                        // Handle call ended
                        if (serverMessage.contains("Call ended by")) {
                            inCall.set(false);
                            awaitingResponse.set(false);
                            System.out.println("The call has ended.");
                            System.out.println("\nMenu:");
                            System.out.println("1 - Make a Video Call");
                            System.out.println("2 - Quit");
                        }

                        // Handle call rejected
                        if (serverMessage.contains("rejected your call")) {
                            awaitingResponse.set(false);
                            inCall.set(false);
                            System.out.println("\nCall rejected. Returning to menu.");
                            System.out.println("\nMenu:");
                            System.out.println("1 - Make a Video Call");
                            System.out.println("2 - Quit");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Exiting...");
                }
            }).start();

            // Handle user inputs
            while (running.get()) {
                if (!awaitingResponse.get() && !inCall.get()) {
                    System.out.println("\nMenu:");
                    System.out.println("1 - Make a Video Call");
                    System.out.println("2 - Quit");
                }

                String input = scanner.nextLine();

                if (awaitingResponse.get()) {
                    // Accept or reject the call
                    if (input.equals("Y") || input.equals("N")) {
                        out.println(input);
                        awaitingResponse.set(false);
                    } else {
                        System.out.println("Invalid input. Press Y to accept or N to reject.");
                    }
                } else if (inCall.get()) {
                    if (input.equals("0")) {
                        out.println("0");
                        System.out.println("You ended the call.");
                        inCall.set(false);
                    } else {
                        System.out.println("Invalid input. Press 0 to end the call.");
                    }
                } else if (input.equals("1")) {
                    System.out.println("Enter the username to call:");
                    String targetUser = scanner.nextLine();
                    out.println("CALL " + targetUser); // Notify the server about the call
                    awaitingResponse.set(true);
                } else if (input.equals("2")) {
                    System.out.println("Exiting...");
                    out.println("exit"); // Notify the server
                    running.set(false);
                    break;
                } else {
                    System.out.println("Invalid input. Please try again.");
                }
            }
        } catch (IOException e) {
            System.out.println("Exiting...");
        }
    }
}