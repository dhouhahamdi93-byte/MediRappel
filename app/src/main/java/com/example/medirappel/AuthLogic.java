package com.example.medirappel;

import java.util.Locale;

/** Décisions d'authentification pures, sans Android : vérifiables par tests JVM. */
public final class AuthLogic {

    public enum AuthError {
        EMAIL_ALREADY_IN_USE,
        USER_NOT_FOUND,
        BAD_PASSWORD,
        /** Défaillance Room, sel ou PBKDF2 : message « Erreur de stockage local ». */
        STORAGE_ERROR
    }

    private AuthLogic() { }

    /** Email canonique : minuscules et sans espaces, avant insertion ou recherche. */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Un email déjà présent bloque l'inscription ; sinon le compte est créé. */
    public static AuthError registerError(boolean emailAlreadyExists) {
        return emailAlreadyExists ? AuthError.EMAIL_ALREADY_IN_USE : null;
    }

    /** Email inconnu et mauvais mot de passe produisent deux messages distincts. */
    public static AuthError loginError(boolean accountExists, boolean passwordMatches) {
        if (!accountExists) return AuthError.USER_NOT_FOUND;
        if (!passwordMatches) return AuthError.BAD_PASSWORD;
        return null;
    }
}
