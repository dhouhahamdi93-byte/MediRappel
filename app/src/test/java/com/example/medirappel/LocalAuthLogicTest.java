package com.example.medirappel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests JVM de l'authentification locale : crypto et décisions, sans Android. */
public class LocalAuthLogicTest {

    @Test
    public void password_is_hashed_with_pbkdf2_and_verifies() {
        byte[] salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("MediRappel2026".toCharArray(), salt);
        assertTrue(PasswordHasher.ITERATIONS >= 10_000);
        assertFalse(hash.contains("MediRappel2026"));
        assertTrue(PasswordHasher.verify("MediRappel2026".toCharArray(), salt, hash));
    }

    @Test
    public void wrong_password_fails_verification() {
        byte[] salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("MediRappel2026".toCharArray(), salt);
        assertFalse(PasswordHasher.verify("MediRappel2027".toCharArray(), salt, hash));
    }

    @Test
    public void each_account_uses_a_distinct_salt() {
        byte[] first = PasswordHasher.newSalt();
        byte[] second = PasswordHasher.newSalt();
        String firstHash = PasswordHasher.hash("MediRappel2026".toCharArray(), first);
        String secondHash = PasswordHasher.hash("MediRappel2026".toCharArray(), second);
        assertFalse(firstHash.equals(secondHash));
        assertFalse(PasswordHasher.constantTimeEquals(first, second));
    }

    @Test
    public void constant_time_comparison_rejects_different_lengths() {
        byte[] value = {1, 2, 3};
        assertTrue(PasswordHasher.constantTimeEquals(value, new byte[]{1, 2, 3}));
        assertFalse(PasswordHasher.constantTimeEquals(value, new byte[]{1, 2}));
        assertFalse(PasswordHasher.constantTimeEquals(value, new byte[]{1, 2, 4}));
        assertFalse(PasswordHasher.constantTimeEquals(null, value));
    }

    @Test
    public void registering_an_existing_email_is_rejected() {
        assertEquals(AuthLogic.AuthError.EMAIL_ALREADY_IN_USE, AuthLogic.registerError(true));
        assertNull(AuthLogic.registerError(false));
    }

    @Test
    public void unknown_email_is_distinct_from_bad_password() {
        assertEquals(AuthLogic.AuthError.USER_NOT_FOUND, AuthLogic.loginError(false, false));
        assertEquals(AuthLogic.AuthError.BAD_PASSWORD, AuthLogic.loginError(true, false));
        assertNull(AuthLogic.loginError(true, true));
    }

    @Test
    public void emails_are_normalized_before_storage_and_lookup() {
        assertEquals("user@example.com",
                AuthLogic.normalizeEmail("  User@Example.COM  "));
        assertEquals("", AuthLogic.normalizeEmail(null));
    }

    @Test
    public void registering_a_new_email_succeeds() {
        assertNull(AuthLogic.registerError(false));
    }

    @Test
    public void login_succeeds_with_existing_account_and_matching_password() {
        assertNull(AuthLogic.loginError(true, true));
    }

    @Test
    public void base64_round_trip_works_without_android_api() {
        byte[] salt = PasswordHasher.newSalt();
        assertTrue(PasswordHasher.constantTimeEquals(
                salt, PasswordHasher.decode(PasswordHasher.encode(salt))));
    }
}
