package us.calubrecht.lazerwiki.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Random;

@Service
public class RandomService {

    Random random = new Random();
    SecureRandom secureRandom = new SecureRandom();

    public int nextInt() {
        return random.nextInt();
    }

    public String randomKey(int passwordTokenLength) {
        final String tokenChars = "ABCDEFGHJKPRTUVWXY2346789";
        int numValidChars = tokenChars.length();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < passwordTokenLength; i++) {
            token.append(tokenChars.charAt(secureRandom.nextInt(numValidChars)));
            if (i == passwordTokenLength / 2 - 1) {
                token.append('-');
            }
        }
        return token.toString();
    }
}
