package com.example.medirappel;

import android.content.Context;
import android.content.SharedPreferences;

/** Session locale persistée : identifiant du compte et email, effacés au logout. */
public final class SessionManager {

    private static final String PREFERENCES = "medirappel_session";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    /** Enregistre la session après inscription ou connexion réussie. */
    public void save(long userId, String email) {
        preferences.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public boolean isLoggedIn() {
        return getUserId() != 0L && getEmail() != null;
    }

    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, 0L);
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, null);
    }

    /** Supprime toute trace de la session courante. */
    public void logout() {
        preferences.edit().clear().apply();
    }
}
