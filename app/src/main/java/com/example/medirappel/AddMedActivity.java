package com.example.medirappel;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Création et modification d'un rappel ; l'alarme est toujours à maintenant + 45 s. */
public class AddMedActivity extends SecureActivity {

    public static final String EXTRA_EDIT_ID = "edit_medication_id";

    private TextInputLayout tilName;
    private TextInputEditText etName, etDescription;
    private Medication editing;

    /** Rappel à reprogrammer exactement au retour des réglages Android. */
    private Medication pendingExactAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isSignedIn()) return;
        setContentView(R.layout.activity_add);
        InputUtils.disableHandwriting(getWindow().getDecorView());

        tilName = findViewById(R.id.tilName);
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        TextView title = findViewById(R.id.tvTitle);
        Button save = findViewById(R.id.btnSave);

        int editId = getIntent().getIntExtra(EXTRA_EDIT_ID, 0);
        if (editId != 0) {
            editing = AppDatabase.get(this).medicationDao().getByIdForUser(editId, currentUid());
            if (editing == null) {
                Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            title.setText(R.string.add_title_edit);
            save.setText(R.string.add_update);
            etName.setText(editing.name);
            etDescription.setText(editing.dose);
        }

        save.setOnClickListener(v -> save());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // L'utilisateur revient des réglages : on remplace l'alarme de secours par
        // une alarme exacte sans déplacer l'horodatage enregistré (enregistrement + 45 s).
        if (pendingExactAlarm != null && ReminderScheduler.canScheduleExact(this)) {
            Medication medication = pendingExactAlarm;
            pendingExactAlarm = null;
            ReminderScheduler.rearm(this, medication);
            Toast.makeText(this, R.string.alarm_exact_enabled, Toast.LENGTH_SHORT).show();
        }
    }

    private void save() {
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        String description = etDescription.getText() == null
                ? "" : etDescription.getText().toString().trim();

        tilName.setError(null);
        if (name.isEmpty()) {
            tilName.setError(getString(R.string.add_error_name));
            return;
        }
        // On enregistre toujours le rappel. Sans droit d'alarme exacte, le secours
        // d'AlarmManager reste approximatif et l'utilisateur est guidé vers les réglages.
        boolean exactAllowed = ReminderScheduler.canScheduleExact(this);

        Medication medication = editing;
        boolean isNew = medication == null;
        if (isNew) {
            medication = new Medication();
            medication.ownerId = currentUid();
        }
        medication.name = name;
        medication.dose = description;

        MedicationDao dao = AppDatabase.get(this).medicationDao();
        if (isNew) {
            medication.id = (int) dao.insert(medication);
        }

        // Une modification annule explicitement l'ancienne alarme, puis la nouvelle
        // est programmée avec le même requestCode : jamais de doublon.
        if (!isNew) ReminderScheduler.cancel(this, medication);
        long triggerAt = ReminderScheduler.schedule(this, medication);
        medication.triggerAt = triggerAt;
        dao.update(medication);

        if (exactAllowed) {
            pendingExactAlarm = null;
            Toast.makeText(this, isNew ? R.string.add_saved : R.string.add_updated,
                    Toast.LENGTH_LONG).show();
        } else {
            pendingExactAlarm = medication;
            // Un seul message : l'explication du secours ne doit pas être masquée
            // par le message « enregistré » suivant.
            Toast.makeText(this, R.string.alarm_fallback_scheduled,
                    Toast.LENGTH_LONG).show();
            requestExactAlarmPermission();
        }
        finish();
    }

    /** Android 12+ : ouvre l'écran système SCHEDULE_EXACT_ALARM sans jamais planter. */
    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception ignored) {
            // Fabricant sans écran dédié : le message de secours reste affiché.
        }
    }
}
