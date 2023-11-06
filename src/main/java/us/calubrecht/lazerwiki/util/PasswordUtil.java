package us.calubrecht.lazerwiki.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
public class PasswordUtil {

    final long iterationCount = 600000;
    final String versionMark = ":v1:";

    PasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hashPassword(String password) {
        return versionMark + encoder.encode(password);
    }

    public boolean matches(String password, String hashedPassword) {
        String existingCrypt = hashedPassword.substring(versionMark.length());
        return encoder.matches(password, existingCrypt);

    }

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PasswordUtil util = new PasswordUtil();
        if (args.length == 1) {
            // Hash a password
            String password = args[0];
            String hashed = util.hashPassword(password);
            System.out.println(hashed);
        }
        if (args.length ==2 ) {
            // compare password with hashhed password
            String password = args[0];
            String hash = args[1];
            System.out.println(util.matches(password, hash) ? "Matches" : "Does not match");
        }


    }
}
