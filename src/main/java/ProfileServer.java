import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.logging.*;

public class ProfileServer {
    private static HashMap<String, String[]> profiles = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(ProfileServer.class.getName());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(1111)) {
            System.out.println("Profile Server is running...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
        	LOGGER.log(Level.SEVERE, "Server error", e);
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String command = in.readLine();
            String username = in.readLine();
            LOGGER.info("Received command: " + command + " for username: " + username);

            // Ensure profile exists for the user
            createDefaultProfileIfNotExists(username);

            switch (command) {
                case "VIEW_PROFILE":
                    handleViewProfile(username, out);
                    break;
                case "EDIT_USERNAME":
                    String newUsername = in.readLine();
                    handleEditUsername(username, newUsername, out);
                    break;
                case "EDIT_BIO":
                    String newBio = in.readLine();
                    handleEditBio(username, newBio, out);
                    break;
                default:
                	LOGGER.warning("Invalid command received: " + command);
                    out.println("Invalid command.");
            }
        } catch (IOException e) {
        	LOGGER.log(Level.SEVERE, "Error handling client request", e);
        }
    }

    private static void createDefaultProfileIfNotExists(String username) {
        if (!profiles.containsKey(username)) {
            // Create a default profile with the username as the default name and an empty bio
            profiles.put(username, new String[]{username, "No bio set"});
            LOGGER.info("Default profile created for username: " + username);
        }
    }
    
    private static void handleViewProfile(String username, PrintWriter out) {
        if (profiles.containsKey(username)) {
            String[] profile = profiles.get(username);
            out.println("Name: " + profile[0] + ", Bio: " + profile[1]);
        } else {
            out.println("Profile not found for username: " + username);
        }
    }
    
    private static void handleEditUsername(String currentUsername, String newUsername, PrintWriter out) {
    	if (profiles.containsKey(currentUsername)) {
            // Get the current profile data
            String[] profileData = profiles.get(currentUsername);

            // Remove the old username key and add the new username key
            profiles.remove(currentUsername);
            profiles.put(newUsername, profileData);

            // Update the name in the profile data
            profileData[0] = newUsername;

            out.println("Username updated successfully!");
            LOGGER.info("Username updated: " + currentUsername + " -> " + newUsername);
        } else {
            String error = "Username not found: " + currentUsername;
            out.println(error);
            LOGGER.warning(error);
        }
    }

    private static void handleEditBio(String username, String newBio, PrintWriter out) {
        if (profiles.containsKey(username)) {
            // Update bio
            String[] profile = profiles.get(username);
            profile[1] = newBio;
            out.println("Bio updated successfully!");
        } else {
            out.println("Username not found.");
        }
    }
}
