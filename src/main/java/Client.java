import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1234;
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
            }
        }
    }

    private static void promptInput() {
        System.out.println();
        System.out.println("Menu:");
        System.out.println("1 - Messaging");
        System.out.println("exit - Quit WhatsApp");
        input = scanner.nextLine();
    }

    private static void promptUsername() {
        System.out.println("Enter your username:");
        username = scanner.nextLine();
    }


}