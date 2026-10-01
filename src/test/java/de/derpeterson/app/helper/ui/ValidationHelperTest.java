package de.derpeterson.app.helper.ui;

import de.derpeterson.app.helper.ui.ValidationHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationHelperTest {

    @Test
    public void testIsPasswordSecure_Blank() {
        assertFalse(ValidationHelper.isPasswordSecure(null));
        assertFalse(ValidationHelper.isPasswordSecure(""));
        assertFalse(ValidationHelper.isPasswordSecure("   "));
    }

    @Test
    public void testIsPasswordSecure_MinLength() {
        // Test with less than 8 characters - should all fail
        assertFalse(ValidationHelper.isPasswordSecure("abc123!"));
        assertFalse(ValidationHelper.isPasswordSecure("aB!"));
        
        // Test with exactly 8 characters (should fail without required elements)
        assertFalse(ValidationHelper.isPasswordSecure("abc12345"));   // 8 chars but no uppercase
        assertFalse(ValidationHelper.isPasswordSecure("ABC12345"));   // 8 chars but no special char
        assertFalse(ValidationHelper.isPasswordSecure("ABCabcde"));   // 8 chars but no special char and no uppercase
    }

    @Test
    public void testIsPasswordSecure_Requirements() {
        // Test with valid passwords (8+ characters minimum with required elements)
        assertTrue(ValidationHelper.isPasswordSecure("Abc123!A"));  // 8 chars: A-b-c-1-2-3-!-A
        assertTrue(ValidationHelper.isPasswordSecure("123ABC!B"));  // 8 chars: 1-2-3-A-B-C-!-B
        assertTrue(ValidationHelper.isPasswordSecure("!@#abcDE"));  // 8 chars: !-@-#-a-b-c-D-E
        assertTrue(ValidationHelper.isPasswordSecure("MyP@ssw0rd")); // 9 chars: M-y-P-@-s-s-w-0-r-d
        
        // Test with invalid passwords (missing requirement)
        assertFalse(ValidationHelper.isPasswordSecure("abc12345"));   // no uppercase  
        assertFalse(ValidationHelper.isPasswordSecure("ABC12345"));   // no special char
        assertFalse(ValidationHelper.isPasswordSecure("ABCabcde"));   // no special char and no uppercase
        assertFalse(ValidationHelper.isPasswordSecure("abc!@#123"));   // no uppercase
        assertFalse(ValidationHelper.isPasswordSecure("MyPassw0rd"));   // no special char
    }

    @Test
    public void testIsPasswordSecure_WithSpaces() {
        // Test with spaces - password should be trimmed and validated
        assertTrue(ValidationHelper.isPasswordSecure(" Abc123! ")); 
        assertFalse(ValidationHelper.isPasswordSecure(" abc123! "));
    }

    @Test
    public void testIsPasswordSecureAllowBlank() {
        assertFalse(ValidationHelper.isPasswordSecure(null, true));
        assertTrue(ValidationHelper.isPasswordSecure("", true));
        assertTrue(ValidationHelper.isPasswordSecure("   ", true));

        // Test with actual valid passwords
        assertTrue(ValidationHelper.isPasswordSecure("Abc123!", true));
        assertTrue(ValidationHelper.isPasswordSecure("MyP@ssw0rd", true));
        
        // Test with invalid password - should fail even with allowBlank = true
        assertFalse(ValidationHelper.isPasswordSecure("abc12345", true)); 
    }

    @Test
    public void testIsEmailValid() {
        // Valid emails (Vaadin EmailValidator pattern)
        assertTrue(ValidationHelper.isEmailValid("test@example.com"));
        assertTrue(ValidationHelper.isEmailValid("user.name@domain.co.uk"));
        assertTrue(ValidationHelper.isEmailValid("user123@test-domain.org"));
        assertTrue(ValidationHelper.isEmailValid("a@b.co"));

        // Invalid emails
        assertFalse(ValidationHelper.isEmailValid(null));
        assertFalse(ValidationHelper.isEmailValid(""));
        assertFalse(ValidationHelper.isEmailValid("   "));
        assertFalse(ValidationHelper.isEmailValid("invalid.email"));
        assertFalse(ValidationHelper.isEmailValid("@example.com"));
        assertFalse(ValidationHelper.isEmailValid("test@"));
        assertFalse(ValidationHelper.isEmailValid("test..test@example.com"));
        
        // Test with whitespace - the trimmed email should be invalid
        assertFalse(ValidationHelper.isEmailValid(" test@example.com "));
    }

    @Test
    public void testIsBirthDateValid() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        // Valid dates (past or present)
        assertTrue(ValidationHelper.isBirthDateValid(yesterday));
        assertTrue(ValidationHelper.isBirthDateValid(today));
        
        // Invalid dates (future)
        assertFalse(ValidationHelper.isBirthDateValid(tomorrow));
        
        // Null date
        assertFalse(ValidationHelper.isBirthDateValid(null));
    }

    @Test
    public void testIsRequiredTextValid() {
        // Valid text
        assertTrue(ValidationHelper.isRequiredTextValid("Hello"));
        assertTrue(ValidationHelper.isRequiredTextValid("  Hello  "));
        assertTrue(ValidationHelper.isRequiredTextValid("a"));

        // Invalid text
        assertFalse(ValidationHelper.isRequiredTextValid(null));
        assertFalse(ValidationHelper.isRequiredTextValid(""));
        assertFalse(ValidationHelper.isRequiredTextValid("   "));
        assertFalse(ValidationHelper.isRequiredTextValid("\t\n"));
    }

    @Test
    public void testNormalize() {
        // Normal cases
        assertEquals("", ValidationHelper.normalize(null));
        assertEquals("", ValidationHelper.normalize(""));
        assertEquals("", ValidationHelper.normalize("   "));
        assertEquals("hello", ValidationHelper.normalize("Hello"));
        assertEquals("hello world", ValidationHelper.normalize("  Hello World  "));
        assertEquals("test@example.com", ValidationHelper.normalize("TEST@EXAMPLE.COM"));
    }
}