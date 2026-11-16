package com.nhom02.streamwire;
import org.junit.Test;
import static org.junit.Assert.*;
import com.nhom02.streamwire.utils.PasswordHasher;
public class PasswordHasherTest {
    @Test public void saltedPasswordsVerifyWithoutStoringPlaintext() {
        String first = PasswordHasher.hash("secret123");
        assertNotEquals(first, PasswordHasher.hash("secret123"));
        assertTrue(PasswordHasher.verify("secret123", first));
        assertFalse(PasswordHasher.verify("wrong", first));
        assertFalse(PasswordHasher.verify("secret123", "pbkdf2$bad$bad"));
        assertTrue(PasswordHasher.verify("old-password", "old-password"));
    }
}
