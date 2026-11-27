package com.example.medirappel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.widget.EditText;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Validation de l'écran de connexion : tout est local, sans serveur ni configuration. */
@RunWith(AndroidJUnit4.class)
public class LoginValidationAuditTest {

    @Test
    public void empty_email_shows_a_dedicated_error() {
        ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class);
        scenario.onActivity(activity -> {
            setText(activity, R.id.etEmail, "");
            setText(activity, R.id.etPassword, "MediRappel2026!");
            activity.findViewById(R.id.btnLogin).performClick();

            CharSequence error = errorOf(activity, R.id.etEmail);
            assertNotNull("Un email vide doit afficher une erreur", error);
            assertEquals(activity.getString(R.string.login_error_empty_email), error.toString());
        });
        scenario.close();
    }

    @Test
    public void invalid_email_format_is_rejected_locally() {
        ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class);
        scenario.onActivity(activity -> {
            setText(activity, R.id.etEmail, "email-invalide");
            setText(activity, R.id.etPassword, "MediRappel2026!");
            activity.findViewById(R.id.btnLogin).performClick();

            CharSequence error = errorOf(activity, R.id.etEmail);
            assertNotNull("Un email mal formé doit afficher une erreur", error);
            assertEquals(activity.getString(R.string.auth_error_invalid_email), error.toString());
        });
        scenario.close();
    }

    @Test
    public void empty_password_shows_a_dedicated_error() {
        ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class);
        scenario.onActivity(activity -> {
            setText(activity, R.id.etEmail, "utilisateur@example.com");
            setText(activity, R.id.etPassword, "");
            activity.findViewById(R.id.btnLogin).performClick();

            CharSequence error = errorOf(activity, R.id.etPassword);
            assertNotNull("Un mot de passe vide doit afficher une erreur", error);
            assertEquals(activity.getString(R.string.login_error_empty_password), error.toString());
        });
        scenario.close();
    }

    @Test
    public void protected_activity_closes_when_no_local_session_exists() throws Exception {
        // SecureActivity termine l'activité dans onCreate() : onActivity() n'est donc
        // pas utilisable ici. On vérifie l'état de fin directement.
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        for (int i = 0; i < 20 && scenario.getState() != Lifecycle.State.DESTROYED; i++) {
            Thread.sleep(100);
        }
        assertEquals("Un écran protégé ne doit pas rester ouvert sans session locale",
                Lifecycle.State.DESTROYED, scenario.getState());
        scenario.close();
    }

    private static void setText(LoginActivity activity, int id, String value) {
        ((EditText) activity.findViewById(id)).setText(value);
    }

    private static CharSequence errorOf(LoginActivity activity, int id) {
        return ((EditText) activity.findViewById(id)).getError();
    }
}
