package com.example.medirappel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests JVM de la validation des formulaires, sans Android ni Robolectric. */
public class AuthValidationTest {

    @Test
    public void valid_emails_are_accepted() {
        assertTrue(AuthHelper.isValidEmail("user@example.com"));
        assertTrue(AuthHelper.isValidEmail("user.name+tag@sub.example.co.uk"));
        assertTrue(AuthHelper.isValidEmail("a@b.c"));
    }

    @Test
    public void invalid_emails_are_rejected() {
        assertFalse(AuthHelper.isValidEmail(null));
        assertFalse(AuthHelper.isValidEmail(""));
        assertFalse(AuthHelper.isValidEmail("email-invalide"));
        assertFalse(AuthHelper.isValidEmail("user@"));
        assertFalse(AuthHelper.isValidEmail("@example.com"));
        assertFalse(AuthHelper.isValidEmail("user@example"));
    }

    @Test
    public void strong_password_requires_eight_characters_letter_and_digit() {
        assertTrue(AuthHelper.isStrongPassword("MediRappel2026"));
        assertFalse(AuthHelper.isStrongPassword("MediRappel"));
        assertFalse(AuthHelper.isStrongPassword("12345678"));
        assertFalse(AuthHelper.isStrongPassword("Med1"));
        assertFalse(AuthHelper.isStrongPassword("Medi_Rappel"));
        assertFalse(AuthHelper.isStrongPassword(null));
    }

    @Test
    public void password_confirmation_accepts_only_identical_values() {
        assertTrue(AuthHelper.passwordsMatch("MediRappel2026", "MediRappel2026"));
        assertFalse(AuthHelper.passwordsMatch("MediRappel2026", "MediRappel2027"));
        assertFalse(AuthHelper.passwordsMatch(null, "MediRappel2026"));
        assertFalse(AuthHelper.passwordsMatch("MediRappel2026", null));
        assertFalse(AuthHelper.passwordsMatch(null, null));
    }
}
