package us.calubrecht.lazerwiki.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void testMain() throws NoSuchAlgorithmException, InvalidKeySpecException {
        PasswordUtil.main(new String[]{"somePassword"});
        String firstHash = outContent.toString().trim();
        outContent.reset();
        PasswordUtil.main(new String[]{"somePassword"});
        String secondHash = outContent.toString().trim();
        outContent.reset();
        // Multiple hashes (without existing salt) of same password should return different values
        assertNotEquals(firstHash, secondHash);

        PasswordUtil.main(new String[]{"somePassword", firstHash});
        // If provided salt from first hashing, should return same value;
        assertEquals("Matches", outContent.toString().trim());

        outContent.reset();
        PasswordUtil.main(new String[]{"wrongPassword", firstHash});
        // If provided salt from first hashing, should return same value;
        assertEquals("Does not match", outContent.toString().trim());


    }
}