package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * PasswordUtil for hashing & verifying password securely.
 */
public class PasswordUtil {

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean checkPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }
}
