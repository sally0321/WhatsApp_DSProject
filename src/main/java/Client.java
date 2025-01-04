import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int AUTH_SERVER_PORT = 1100; // New Authentication Server Port
    private static final int CHAT_SERVER_PORT = 1110;
    private static final int PROFILE_SERVER_PORT = 1111;
    private static final int VIDEO_CALL_SERVER_PORT = 1112;

    private static Scanner scanner = new Scanner(System.in);
    private static String input;
    private static String username;
    private static String phoneNumber;
    private static boolean isLoggedIn = false;

    public static void main(String[] args) {
        initialMenu();
        while (isLoggedIn) {
            menu();
            if (input.equals("exit")) {
                break;
            }
            switch (input) {
                case "1": {
                    startChatting();
                    break;
                }
                case "2": {
                    profileSettings();
                    break;
                }
                case "3": { // Video calling functionality
                    startVideoCall();
                    break;
                }
                case "4": { // Account Settings
                    accountSettings();
                    break;
                }
                default:
                    System.out.println("Invalid option.");
            }
        }
        System.out.println("Exiting application. Goodbye!");
    }

    private static void initialMenu() {
        while (!isLoggedIn) {
            System.out.println("\n=== Welcome to WhatsApp ===");
            System.out.println("1 - Login");
            System.out.println("2 - Register");
            System.out.println("exit - Quit");
            System.out.print("Choose an option: ");
            input = scanner.nextLine();

            switch (input) {
                case "1":
                    login();
                    break;
                case "2":
                    register();
                    break;
                case "exit":
                    System.exit(0);
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void login() {
        try (Socket socket = new Socket(SERVER_ADDRESS, AUTH_SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.print("Enter phone number: ");
            phoneNumber = scanner.nextLine();
            System.out.print("Enter username: ");
            username = scanner.nextLine();

            // Send login request
            out.println("LOGIN");
            out.println(phoneNumber);
            out.println(username);

            // Receive response
            String response = in.readLine();
            if ("SUCCESS".equals(response)) {
                System.out.println("Login successful!");
                isLoggedIn = true;
            } else {
                System.out.println("Login failed: " + response);
            }

        } catch (IOException e) {
            System.out.println("Unable to connect to Authentication Server. Please try again later.");
        }
    }

    private static void register() {
        try (Socket socket = new Socket(SERVER_ADDRESS, AUTH_SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.print("Enter phone number: ");
            phoneNumber = scanner.nextLine();
            System.out.print("Enter desired username: ");
            username = scanner.nextLine();

            // Send registration request
            out.println("REGISTER");
            out.println(phoneNumber);
            out.println(username);

            // Receive response
            String response = in.readLine();
            if ("SUCCESS".equals(response)) {
                System.out.println("Registration successful! You are now logged in.");
                isLoggedIn = true;
            } else {
                System.out.println("Registration failed: " + response);
            }

        } catch (IOException e) {
            System.out.println("Unable to connect to Authentication Server. Please try again later.");
        }
    }

    private static void menu() {
        System.out.println();
        System.out.println("=== Main Menu ===");
        System.out.println("1 - Messaging");
        System.out.println("2 - Profile Settings");
        System.out.println("3 - Video Call");
        System.out.println("4 - Account Settings");
        System.out.println("exit - Quit WhatsApp");
        System.out.print("Choose an option: ");
        input = scanner.nextLine();
    }

    private static void startChatting(){
        try {
            Socket socket = new Socket(SERVER_ADDRESS, CHAT_SERVER_PORT);
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
                    System.out.println("Connection to chat server lost.");
                }
            }).start();

            // Read messages from the console and send to the server
            Scanner scanner = new Scanner(System.in);
            String userInput;

            while (true) {
                userInput = scanner.nextLine();
                out.println(userInput);
                if (userInput.equals("back")) {
                    break;
                }
            }
            socket.close();

        } catch (IOException e) {
            System.out.println("Unable to connect to server. Please try again later.");
        }
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
            System.out.println("Unable to connect to Profile Server.");
        }
    }

    private static void startVideoCall() {
        try (Socket socket = new Socket(SERVER_ADDRESS, VIDEO_CALL_SERVER_PORT)) {
            System.out.println("Connected to the call server!");

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
                            showVideoCallMenu();
                            continue;
                        }

                        // Handle incoming call notification
                        if (serverMessage.contains("is calling you")) {
                            awaitingResponse.set(true);
                        }

                        // Handle call accepted
                        if (serverMessage.contains("Starting call...")) {
                            awaitingResponse.set(false);
                            inCall.set(true);
                        }

                        // Handle call ended
                        if (serverMessage.contains("Call ended by")) {
                            inCall.set(false);
                            awaitingResponse.set(false);
                            System.out.println("The call has ended.");
                            showVideoCallMenu();
                        }

                        // Handle call rejected
                        if (serverMessage.contains("rejected your call")) {
                            awaitingResponse.set(false);
                            inCall.set(false);
                            System.out.println("\nCall rejected. Returning to menu.");
                            showVideoCallMenu();
                        }
                        if (serverMessage.contains("You rejected the call from")) {
                            awaitingResponse.set(false);
                            inCall.set(false);
                            showVideoCallMenu();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection to call server lost.");
                }
            }).start();

            // Handle user inputs
            while (running.get()) {
                if (!awaitingResponse.get() && !inCall.get()) {
                    showVideoCallMenu();
                }

                String input = scanner.nextLine();

                if (awaitingResponse.get()) {
                    // Accept or reject the call
                    if (input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("N")) {
                        out.println(input.toUpperCase());
                        awaitingResponse.set(false);
                        if (input.equalsIgnoreCase("Y")) {
                            inCall.set(true);
                        }
                    } else {
                        System.out.println("Invalid input. Press Y to accept or N to reject.");
                    }
                } else if (inCall.get()) {
                    if (input.equals("0")) {
                        out.println("0"); // Notify the server to end the call
                        System.out.println("You ended the call.");
                        inCall.set(false); // Reset inCall state
                    } else {
                        System.out.println("Invalid input. Press 0 to end the call.");
                    }
                } else {
                    if (input.equals("1")) {
                        System.out.print("Enter the username to call: ");
                        String targetUser = scanner.nextLine();
                        out.println("CALL " + targetUser); // Notify the server about the call
                        awaitingResponse.set(true);
                    } else if (input.equals("back")) {
                        System.out.println("Returning to main menu...");
                        running.set(false);
                        break;
                    } else {
                        System.out.println("Invalid input. Please try again.");
                    }
                }
            }
            socket.close();
        } catch (IOException e) {
            System.out.println("Exiting...");
        }
    }

    private static void showVideoCallMenu() {
        System.out.println("\n=== Video Call Menu ===");
        System.out.println("1 - Make a Call");
        System.out.println("back - Go Back");
        System.out.print("Choose an option: ");
    }

    private static void accountSettings() {
        while (true) {
            System.out.println("\n-- Account Settings --");
            System.out.println("1 - Logout");
            System.out.println("2 - Delete Account");
            System.out.println("back - Go Back");

            String input = scanner.nextLine();
            switch (input) {
                case "1":
                    logout();
                    return;
                case "2":
                    deleteAccount();
                    return;
                case "back":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void logout() {
        isLoggedIn = false;
        username = null;
        phoneNumber = null;
        System.out.println("You have been logged out.");
        initialMenu();
    }

    private static void deleteAccount() {
        try (Socket socket = new Socket(SERVER_ADDRESS, AUTH_SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send delete account request
            out.println("DELETE_ACCOUNT");
            out.println(phoneNumber);
            out.println(username);

            // Receive response
            String response = in.readLine();
            if ("SUCCESS".equals(response)) {
                System.out.println("Your account has been deleted.");
                isLoggedIn = false;
                username = null;
                phoneNumber = null;
                initialMenu();
            } else {
                System.out.println("Failed to delete account: " + response);
            }

        } catch (IOException e) {
            System.out.println("Unable to connect to Authentication Server.");
        }
    }
}
