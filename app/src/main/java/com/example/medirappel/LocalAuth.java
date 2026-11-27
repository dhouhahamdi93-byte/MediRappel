package com.example.medirappel;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Authentification 100 % locale : Room + PBKDF2, callbacks toujours sur le thread UI. */
public final class LocalAuth {

    private static final String TAG = "LocalAuth";

    /** Tous les accès Room et tous les hashes sont exécutés hors du thread principal. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler UI = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(User user);
        void onError(AuthLogic.AuthError error);
    }

    private LocalAuth() { }

    /** Crée un compte local ; un email déjà utilisé est refusé. */
    public static void register(Context context, String email, char[] password,
                                String displayName, Callback callback) {
        Context app = context.getApplicationContext();
        String normalized = AuthLogic.normalizeEmail(email);
        EXECUTOR.execute(() -> {
            try {
                byte[] salt = PasswordHasher.newSalt();
                String hash = PasswordHasher.hash(password, salt);
                UserDao dao = AppDatabase.get(app).userDao();
                if (AuthLogic.registerError(dao.findByEmail(normalized) != null) != null) {
                    deliver(callback, null, AuthLogic.AuthError.EMAIL_ALREADY_IN_USE);
                    return;
                }
                User user = new User();
                user.email = normalized;
                user.salt = PasswordHasher.encode(salt);
                user.passwordHash = hash;
                user.createdAt = System.currentTimeMillis();
                user.displayName = displayName;
                user.id = dao.insert(user);
                deliver(callback, user, null);
            } catch (SQLiteConstraintException e) {
                // Concurrence ou index unique : le diagnostic reste « email déjà utilisé ».
                Log.e(TAG, "register failed: email already in use", e);
                deliver(callback, null, AuthLogic.AuthError.EMAIL_ALREADY_IN_USE);
            } catch (Exception e) {
                // Exception journalisée sans le mot de passe, jamais affichée telle quelle.
                Log.e(TAG, "register failed", e);
                deliver(callback, null, AuthLogic.AuthError.STORAGE_ERROR);
            }
        });
    }

    /** Connecte un compte local ; email inconnu et mauvais mot de passe sont distincts. */
    public static void login(Context context, String email, char[] password, Callback callback) {
        Context app = context.getApplicationContext();
        String normalized = AuthLogic.normalizeEmail(email);
        EXECUTOR.execute(() -> {
            try {
                User user = AppDatabase.get(app).userDao().findByEmail(normalized);
                boolean exists = user != null;
                boolean matches = exists && PasswordHasher.verify(
                        password, PasswordHasher.decode(user.salt), user.passwordHash);
                deliver(callback, user, AuthLogic.loginError(exists, matches));
            } catch (Exception e) {
                Log.e(TAG, "login failed", e);
                deliver(callback, null, AuthLogic.AuthError.STORAGE_ERROR);
            }
        });
    }

    private static void deliver(Callback callback, User user, AuthLogic.AuthError error) {
        if (callback == null) return;
        UI.post(() -> {
            if (error == null) callback.onSuccess(user);
            else callback.onError(error);
        });
    }
}
