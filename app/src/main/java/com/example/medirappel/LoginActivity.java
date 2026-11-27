package com.example.medirappel;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

/** Écran de connexion obligatoire : point d'entrée de l'application, sans serveur. */
public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible;
    private ProgressBar progress;
    private Button btnLogin;
    private boolean loginInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (new SessionManager(this).isLoggedIn()) {
            // Session locale déjà persistée : pas de reconnexion à chaque ouverture.
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        InputUtils.disableHandwriting(getWindow().getDecorView());
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        progress = findViewById(R.id.progressLogin);
        btnLogin = findViewById(R.id.btnLogin);

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> login());
        findViewById(R.id.tvGoRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void togglePasswordVisibility() {
        int start = etPassword.getSelectionStart();
        int end = etPassword.getSelectionEnd();
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivTogglePassword.setImageResource(R.drawable.ic_visibility);
            isPasswordVisible = false;
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivTogglePassword.setImageResource(R.drawable.ic_visibility);
            isPasswordVisible = true;
        }
        etPassword.setSelection(start, end);
    }

    private void login() {
        // Garde explicite : une deuxième pression pendant la vérification est ignorée.
        if (loginInProgress) return;

        String email = text(etEmail).trim();
        String password = text(etPassword);

        etEmail.setError(null);
        etPassword.setError(null);

        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.login_error_empty_email));
            etEmail.requestFocus();
            return;
        }
        if (!AuthHelper.isValidEmail(email)) {
            etEmail.setError(getString(R.string.auth_error_invalid_email));
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.login_error_empty_password));
            etPassword.requestFocus();
            return;
        }

        loginInProgress = true;
        setLoading(true);
        // Room et PBKDF2 tournent hors du thread principal ; callback sur le thread UI.
        LocalAuth.login(this, email, password.toCharArray(), new LocalAuth.Callback() {
            @Override
            public void onSuccess(User user) {
                loginInProgress = false;
                setLoading(false);
                new SessionManager(LoginActivity.this).save(user.id, user.email);
                goToMain();
            }

            @Override
            public void onError(AuthLogic.AuthError error) {
                loginInProgress = false;
                setLoading(false);
                Toast.makeText(LoginActivity.this, AuthHelper.localError(LoginActivity.this, error),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Nettoie la pile : le bouton Back ne ramène jamais à l'écran de connexion. */
    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        btnLogin.setEnabled(!loading);
    }

    private static String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}
