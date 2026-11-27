package com.example.medirappel;

import android.content.Context;

import java.util.regex.Pattern;

/** Validation des formulaires et traduction FR/AR des erreurs d'authentification locale. */
public final class AuthHelper {

    // Regex identique à android.util.Patterns.EMAIL_ADDRESS : la validation reste testable en JVM.
    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+");
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private AuthHelper() { }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    public static boolean isStrongPassword(String password) {
        return password != null && STRONG_PASSWORD.matcher(password).matches();
    }

    // Comparaison isolée pour être vérifiée par des tests JVM sans Activity.
    public static boolean passwordsMatch(String password, String confirm) {
        return password != null && password.equals(confirm);
    }

    /** Messages d'erreur locaux, traduits dans la langue courante de l'application. */
    public static String localError(Context context, AuthLogic.AuthError error) {
        if (error == AuthLogic.AuthError.EMAIL_ALREADY_IN_USE) {
            return context.getString(R.string.auth_error_email_in_use);
        }
        if (error == AuthLogic.AuthError.USER_NOT_FOUND) {
            return context.getString(R.string.auth_error_user_not_found);
        }
        if (error == AuthLogic.AuthError.BAD_PASSWORD) {
            return context.getString(R.string.auth_error_bad_password);
        }
        return context.getString(R.string.auth_error_storage);
    }
}
