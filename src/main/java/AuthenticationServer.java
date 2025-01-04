import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthenticationServer {
    private static final int AUTH_SERVER_PORT = 1100;
    private static final int THREAD_POOL_SIZE = 10; // Adjust as needed

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(AUTH_SERVER_PORT)) {
            System.out.println("Authentication Server is running on port " + AUTH_SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new AuthHandler(clientSocket));
            }

        } catch (IOException e) {
            System.out.println("Authentication Server encountered an error.");
            e.printStackTrace();
        }
    }
}

class AuthHandler implements Runnable {
    private Socket socket;

    public AuthHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true);
        ) {
            String command = in.readLine();
            if (command == null) {
                socket.close();
                return;
            }

            switch (command) {
                case "LOGIN":
                    handleLogin(in, out);
                    break;
                case "REGISTER":
                    handleRegister(in, out);
                    break;
                case "DELETE_ACCOUNT":
                    handleDeleteAccount(in, out);
                    break;
                default:
                    out.println("Invalid Command");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleLogin(BufferedReader in, PrintWriter out) throws IOException {
        String phoneNumber = in.readLine();
        String username = in.readLine();

        boolean isValid = DatabaseServer.verifyUser(phoneNumber, username);
        if (isValid) {
            out.println("SUCCESS");
        } else {
            out.println("Invalid phone number or username.");
        }
    }

    private void handleRegister(BufferedReader in, PrintWriter out) throws IOException {
        String phoneNumber = in.readLine();
        String username = in.readLine();

        boolean isRegistered = DatabaseServer.registerUser(phoneNumber, username);
        if (isRegistered) {
            out.println("SUCCESS");
        } else {
            out.println("Phone number already exists. Please login.");
        }
    }

    private void handleDeleteAccount(BufferedReader in, PrintWriter out) throws IOException {
        String phoneNumber = in.readLine();
        String username = in.readLine();

        boolean isDeleted = DatabaseServer.deleteUser(phoneNumber, username);
        if (isDeleted) {
            out.println("SUCCESS");
        } else {
            out.println("Failed to delete account. Please check your credentials.");
        }
    }
}
