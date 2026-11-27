package com.example.medirappel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/** Langue, permission de notification et déconnexion. */
public class SettingsActivity extends SecureActivity {

    private static final int REQUEST_NOTIFICATIONS = 102;

    private TextView tvPermissionState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isSignedIn()) return;
        setContentView(R.layout.activity_settings);
        InputUtils.disableHandwriting(getWindow().getDecorView());

        String email = session().getEmail();
        ((TextView) findViewById(R.id.tvEmail)).setText(
                getString(R.string.settings_logged_as) + " "
                        + (email == null ? "" : email));

        tvPermissionState = findViewById(R.id.tvPermissionState);
        updatePermissionState();

        RadioGroup group = findViewById(R.id.rgLanguage);
        String language = LocaleHelper.getLanguage(this);
        if (LocaleHelper.ARABIC.equals(language)) {
            ((RadioButton) findViewById(R.id.rbAr)).setChecked(true);
        } else if (LocaleHelper.FRENCH.equals(language)) {
            ((RadioButton) findViewById(R.id.rbFr)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rbSystem)).setChecked(true);
        }
        group.setOnCheckedChangeListener((g, checkedId) -> {
            if (checkedId == R.id.rbFr) {
                LocaleHelper.setLanguage(this, LocaleHelper.FRENCH);
            } else if (checkedId == R.id.rbAr) {
                LocaleHelper.setLanguage(this, LocaleHelper.ARABIC);
            } else {
                LocaleHelper.setLanguage(this, LocaleHelper.SYSTEM);
            }
            Toast.makeText(this, R.string.settings_language_saved, Toast.LENGTH_SHORT).show();
            recreate(); // recharge immédiatement les ressources dans la langue choisie
        });

        findViewById(R.id.btnPermission).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATIONS);
            } else {
                updatePermissionState();
            }
        });

        Button logout = findViewById(R.id.btnLogout);
        logout.setOnClickListener(v -> {
            session().logout();
            Toast.makeText(this, R.string.main_logout_success, Toast.LENGTH_SHORT).show();
            goToLogin();
        });
    }

    private void updatePermissionState() {
        boolean granted = Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        tvPermissionState.setText(granted ? R.string.settings_notification_state_on
                : R.string.settings_notification_state_off);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) updatePermissionState();
    }
}
