import java.util.HashMap;
import java.util.Random;

public class UserDatabase {
    private HashMap<String, String> users = new HashMap<>(); //Verified users
    private HashMap<String, String> pendingUsers = new HashMap<>(); //Users awaiting verification
    private Random random = new Random();

    public boolean userExists(String phoneNumber) {
        return users.containsKey(phoneNumber);
    }

    public String generateVerificationCode(String phoneNumber) {
        String code = String.format("%06d", random.nextInt(1000000));
        pendingUsers.put(phoneNumber, code);  //Store in pending map instead
        return code;
    }

    public boolean verifyCode(String phoneNumber, String code) {
        String storedCode = pendingUsers.get(phoneNumber);
        if (storedCode != null && storedCode.equals(code)) {
            //Only add to users if verification is correct
            if (!users.containsKey(phoneNumber)) {
                users.put(phoneNumber, code);
            }
            pendingUsers.remove(phoneNumber);  // Clean up pending entry
            return true;
        }
        return false;
    }

    public void deleteUser(String phoneNumber) {
        users.remove(phoneNumber);
        pendingUsers.remove(phoneNumber); 
    }
}