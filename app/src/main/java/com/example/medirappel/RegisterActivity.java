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

/** Création de compte 100 % locale avec validation locale des champs. */
public class RegisterActivity extends BaseActivity {

    private EditText etName, etEmail, etPassword, etConfirm;
    private ImageView ivTogglePassword, ivToggleConfirm;
    private boolean isPasswordVisible, isConfirmVisible;
    private ProgressBar progress;
    private Button btnRegister;
    private boolean registerInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        InputUtils.disableHandwriting(getWindow().getDecorView());

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirm = findViewById(R.id.ivToggleConfirm);
        progress = findViewById(R.id.progressRegister);
        btnRegister = findViewById(R.id.btnRegister);

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        ivToggleConfirm.setOnClickListener(v -> toggleConfirmVisibility());
        btnRegister.setOnClickListener(v -> register());
        findViewById(R.id.tvGoLogin).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
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

    private void toggleConfirmVisibility() {
        int start = etConfirm.getSelectionStart();
        int end = etConfirm.getSelectionEnd();
        if (isConfirmVisible) {
            etConfirm.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivToggleConfirm.setImageResource(R.drawable.ic_visibility);
            isConfirmVisible = false;
        } else {
            etConfirm.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivToggleConfirm.setImageResource(R.drawable.ic_visibility);
            isConfirmVisible = true;
        }
        etConfirm.setSelection(start, end);
    }

    private void register() {
        // Garde anti-clics multiples pendant la création de compte.
        if (registerInProgress) return;

        String name = text(etName).trim();
        String email = text(etEmail).trim();
        String password = text(etPassword);
        String confirm = text(etConfirm);

        clearErrors();
        if (name.isEmpty()) {
            etName.setError(getString(R.string.register_error_empty));
            etName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.register_error_empty_email));
            etEmail.requestFocus();
            return;
        }
        if (!AuthHelper.isValidEmail(email)) {
            etEmail.setError(getString(R.string.auth_error_invalid_email));
            etEmail.requestFocus();
            return;
        }
        if (!AuthHelper.isStrongPassword(password)) {
            etPassword.setError(getString(R.string.auth_error_weak_password));
            etPassword.requestFocus();
            return;
        }
        // La confirmation utilise la même méthode pure que les tests JVM.
        if (!AuthHelper.passwordsMatch(password, confirm)) {
            etConfirm.setError(getString(R.string.register_error_mismatch));
            etConfirm.requestFocus();
            return;
        }

        registerInProgress = true;
        setLoading(true);
        // Room et PBKDF2 tournent hors du thread principal ; callback sur le thread UI.
        LocalAuth.register(this, email, password.toCharArray(), name, new LocalAuth.Callback() {
            @Override
            public void onSuccess(User user) {
                registerInProgress = false;
                setLoading(false);
                new SessionManager(RegisterActivity.this).save(user.id, user.email);
                Toast.makeText(RegisterActivity.this, R.string.register_success,
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(AuthLogic.AuthError error) {
                registerInProgress = false;
                setLoading(false);
                Toast.makeText(RegisterActivity.this, AuthHelper.localError(RegisterActivity.this, error),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearErrors() {
        etName.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        etConfirm.setError(null);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        btnRegister.setEnabled(!loading);
    }

    private static String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}
