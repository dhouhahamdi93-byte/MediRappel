package com.example.medirappel;

import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hachage PBKDF2 pur Java : testable en JVM et compatible API 24+.
 *
 * API 26+ : PBKDF2WithHmacSHA256. Sur API 24/25 cet algorithme est absent,
 * on bascule automatiquement sur PBKDF2WithHmacSHA1 avec le même nombre
 * d'itérations (120 000, bien au-delà du minimum de 10 000 demandé).
 * Aucun mot de passe n'est journalisé ni conservé en clair.
 */
public final class PasswordHasher {

    public static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final String SHA256 = "PBKDF2WithHmacSHA256";
    private static final String SHA1 = "PBKDF2WithHmacSHA1";
    private static final String ALGORITHM = resolveAlgorithm();

    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final int[] REVERSE = new int[128];

    static {
        Arrays.fill(REVERSE, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            REVERSE[ALPHABET[i]] = i;
        }
    }

    private PasswordHasher() { }

    /** True uniquement sur les appareils où SHA256 est indisponible (API 24/25). */
    public static boolean usesSha1Fallback() {
        return SHA1.equals(ALGORITHM);
    }

    private static String resolveAlgorithm() {
        try {
            SecretKeyFactory.getInstance(SHA256);
            return SHA256;
        } catch (NoSuchAlgorithmException e) {
            return SHA1;
        }
    }

    /** Nouveau sel cryptographique pour chaque compte. */
    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    /** Hash en Base64 ; le tableau de caractères du mot de passe est effacé après usage. */
    public static String hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return encode(factory.generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponible", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Vérification en temps constant contre le hash enregistré. */
    public static boolean verify(char[] password, byte[] salt, String expectedHash) {
        byte[] expected = decode(expectedHash);
        byte[] actual = decode(hash(password, salt));
        return constantTimeEquals(actual, expected);
    }

    /** Comparaison qui ne court-circuite pas sur le premier octet différent. */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    /** Base64 maison : java.util.Base64 n'existe pas avant API 26. */
    public static String encode(byte[] data) {
        StringBuilder out = new StringBuilder(((data.length + 2) / 3) * 4);
        int i = 0;
        for (; i + 2 < data.length; i += 3) {
            int value = ((data[i] & 0xFF) << 16)
                    | ((data[i + 1] & 0xFF) << 8)
                    | (data[i + 2] & 0xFF);
            out.append(ALPHABET[(value >> 18) & 63])
                    .append(ALPHABET[(value >> 12) & 63])
                    .append(ALPHABET[(value >> 6) & 63])
                    .append(ALPHABET[value & 63]);
        }
        int remaining = data.length - i;
        if (remaining == 1) {
            int value = (data[i] & 0xFF) << 16;
            out.append(ALPHABET[(value >> 18) & 63])
                    .append(ALPHABET[(value >> 12) & 63]).append("==");
        } else if (remaining == 2) {
            int value = ((data[i] & 0xFF) << 16) | ((data[i + 1] & 0xFF) << 8);
            out.append(ALPHABET[(value >> 18) & 63])
                    .append(ALPHABET[(value >> 12) & 63])
                    .append(ALPHABET[(value >> 6) & 63]).append('=');
        }
        return out.toString();
    }

    /** Décodage Base64 standard, sans dépendance Android. */
    public static byte[] decode(String value) {
        if (value == null) throw new IllegalArgumentException("Base64 null");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int[] group = new int[4];
        int count = 0;
        for (char c : value.toCharArray()) {
            if (c == '=') break;
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
            int decoded = c < 128 ? REVERSE[c] : -1;
            if (decoded < 0) throw new IllegalArgumentException("Base64 invalide");
            group[count++] = decoded;
            if (count == 4) {
                int bits = (group[0] << 18) | (group[1] << 12) | (group[2] << 6) | group[3];
                out.write((bits >> 16) & 0xFF);
                out.write((bits >> 8) & 0xFF);
                out.write(bits & 0xFF);
                count = 0;
            }
        }
        if (count == 2) {
            int bits = (group[0] << 18) | (group[1] << 12);
            out.write((bits >> 16) & 0xFF);
        } else if (count == 3) {
            int bits = (group[0] << 18) | (group[1] << 12) | (group[2] << 6);
            out.write((bits >> 16) & 0xFF);
            out.write((bits >> 8) & 0xFF);
        } else if (count != 0) {
            throw new IllegalArgumentException("Base64 invalide");
        }
        return out.toByteArray();
    }
}
