package com.nhom02.streamwire;

import com.nhom02.streamwire.cloud.AuthErrorMessage;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthErrorMessageTest {
    @Test public void configurationReasonIsRecognizedInsideInternalError() {
        String message = AuthErrorMessage.describe("ERROR_INTERNAL_ERROR", "An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]", "AUTH", false);
        assertTrue(message.contains("Email/Password"));
        assertTrue(message.contains("Authentication"));
        assertFalse(message.contains("Tài khoản đã được tạo"));
    }
    @Test public void firestoreFailureDoesNotTellUserToRegisterAgain() {
        String message = AuthErrorMessage.describe("PERMISSION_DENIED", "Permission denied", "SAVE_PROFILE", true);
        assertTrue(message.contains("Tài khoản đã được tạo"));
        assertTrue(message.contains("firestore.rules"));
        assertTrue(message.contains("không cần đăng ký lại"));
    }
    @Test public void rawServerDetailsAreNotDisplayed() {
        String message = AuthErrorMessage.describe(null, "Unknown server response containing confidential details", "AUTH", false);
        assertFalse(message.contains("confidential"));
    }
    @Test public void networkErrorsAreDifferentFromCredentials() {
        assertTrue(AuthErrorMessage.describe("NETWORK_REQUEST_FAILED", "", "AUTH", false).contains("Internet"));
        assertTrue(AuthErrorMessage.describe("ERROR_INVALID_CREDENTIAL", "", "AUTH", false).contains("Email hoặc mật khẩu"));
    }
    @Test public void unavailableProfileIncludesFirestoreSetupRatherThanOnlyNetworkAdvice() {
        String message = AuthErrorMessage.describe("UNAVAILABLE", "Failed to get document because the client is offline", "READ_PROFILE", true);
        assertTrue(message.contains("Cloud Firestore API"));
        assertTrue(message.contains("(default)"));
        assertTrue(message.contains("không cần đăng ký lại"));
    }
}
