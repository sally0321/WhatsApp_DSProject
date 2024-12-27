import java.io.*;
import java.net.*;
import java.util.HashMap;

public class AccountServer {
    private static final int PORT = 1110;
    private static UserDatabase userDatabase = new UserDatabase();
    private static HashMap<String, PrintWriter> activeClients = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Account server is running!");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String currentUser = null;
            String deviceId = clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort();
            boolean isLinkedDevice = false;
            
            while (true) {
                if (currentUser == null && !isLinkedDevice) {
                    showLoginMenu(out);
                } else {
                    showLoggedInMenu(out);
                }
                
                String choice = in.readLine();

                if (choice == null || choice.equalsIgnoreCase("exit")) {
                    out.println("Exited");
                    break;
                }

                if (currentUser == null && !isLinkedDevice) {
                    switch (choice) {
                        case "1":
                            currentUser = handleSignup(in, out);
                            break;
                        case "2":
                            currentUser = handleLogin(in, out);
                            break;
                        case "3":
                            if (handleLinkDevice(in, out)) {
                                isLinkedDevice = true;
                            }
                            break;
                        default:
                            out.println("Invalid choice. Try again.");
                    }
                } else {
                    switch (choice) {
                        case "1":
                            if (handleDeleteAccount(in, out, currentUser)) {
                                currentUser = null;
                                isLinkedDevice = false;
                            }
                            break;
                        case "2":
                            activeClients.remove(currentUser);
                            currentUser = null;
                            isLinkedDevice = false;
                            out.println("Logged out successfully!");
                            break;
                        default:
                            out.println("Invalid choice. Try again.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void showLoginMenu(PrintWriter out) {
        out.println("\nMain Menu:");
        out.println("1: Signup");
        out.println("2: Login");
        out.println("3: Link Device");
        out.println("Type 'exit' to quit.");
        //out.print("Enter your choice: ");
    }

    private static void showLoggedInMenu(PrintWriter out) {
        out.println("\nLogged In Menu:");
        out.println("1: Delete Account");
        out.println("2: Logout");
        out.println("Type 'exit' to quit.");
        //out.print("Enter your choice: ");
    }

    private static String handleSignup(BufferedReader in, PrintWriter out) throws IOException {
        out.println("Enter your phone number:");
        String phoneNumber = in.readLine();

        if (userDatabase.userExists(phoneNumber)) {
            out.println("User already exists. Try LOGIN instead.");
            return null;
        } else {
            String code = userDatabase.generateVerificationCode(phoneNumber);
            out.println("Verification code sent to " + phoneNumber + ": " + code);
            out.println("Enter the verification code:");
            String enteredCode = in.readLine();

            if (userDatabase.verifyCode(phoneNumber, enteredCode)) {
                out.println("Signup successful! Welcome!");
                activeClients.put(phoneNumber, out);
                return phoneNumber;
            } else {
                out.println("Incorrect code. Signup failed.");
                return null;
            }
        }
    }
    
    private static String handleLogin(BufferedReader in, PrintWriter out) throws IOException {
        out.println("Enter your phone number:");
        String phoneNumber = in.readLine();

        if (!userDatabase.userExists(phoneNumber)) {
            out.println("This account does not exist. Try SIGNUP instead.");
            return null;
        } else {
            String code = userDatabase.generateVerificationCode(phoneNumber);
            out.println("Verification code sent to " + phoneNumber + ": " + code);
            out.println("Enter the verification code:");
            String enteredCode = in.readLine();

            if (userDatabase.verifyCode(phoneNumber, enteredCode)) {
                out.println("Login successful! Welcome back!");
                activeClients.put(phoneNumber, out);
                return phoneNumber;
            } else {
                out.println("Incorrect code. Login failed.");
                return null;
            }
        }
    }


    private static boolean handleLinkDevice(BufferedReader in, PrintWriter out) throws IOException {
        out.println("Enter your phone number:");
        String phoneNumber = in.readLine();

        if (userDatabase.userExists(phoneNumber) && activeClients.containsKey(phoneNumber)) {
            out.println("Requesting verification code from main device...");
            String primaryCode = userDatabase.generateVerificationCode(phoneNumber);

            PrintWriter primaryOut = activeClients.get(phoneNumber);
            if (primaryOut != null) {
                primaryOut.println("Verification code for linking device: " + primaryCode);
            }

            out.println("Enter the verification code from your mobile phone:");
            String enteredCode = in.readLine();

            if (userDatabase.verifyCode(phoneNumber, enteredCode)) {
                out.println("Device linked successfully!");
                if (primaryOut != null) {
                primaryOut.println("A new device has been linked to your account");
                }
                return true;
            } else {
                out.println("Verification failed. Device not linked.");
                return false;
            }
        } else {
            out.println("User does not exist or user is not logged in.");
            return false;
        }
    }

    private static boolean handleDeleteAccount(BufferedReader in, PrintWriter out, String phoneNumber) throws IOException {
        out.println("Are you sure you want to delete your account? (yes/no):");
        String confirmation = in.readLine();
        
        if (confirmation.equalsIgnoreCase("yes")) {
            String code = userDatabase.generateVerificationCode(phoneNumber);
            out.println("For security, please verify. Code sent to " + phoneNumber + ": " + code);
            out.println("Enter the verification code:");
            String enteredCode = in.readLine();
            
            if (userDatabase.verifyCode(phoneNumber, enteredCode)) {
                userDatabase.deleteUser(phoneNumber);
                activeClients.remove(phoneNumber);
                out.println("Account deleted successfully.");
                return true;
            } else {
                out.println("Incorrect code. Account deletion cancelled.");
                return false;
            }
        } else {
            out.println("Account deletion cancelled.");
            return false;
        }
    }
}