import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1110;
    private static final int PROFILE_SERVER_PORT = 1111; 

    private static Scanner scanner = new Scanner(System.in);
    private static String input;
    private static String username;

    public static void main(String[] args) {
        promptUsername();
        while (true){
            promptInput();
            if (input.equals("exit")){
                break;
            }
            
            switch (input){
                case "1":{
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
                            if (userInput.equals("exit")){
                                break;
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                case"2":{
                	profileSettings();
                	break; 
                }
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


}
