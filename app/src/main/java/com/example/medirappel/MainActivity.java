package com.example.medirappel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/** Accueil protégé : ne montre que les rappels du compte connecté. */
public class MainActivity extends SecureActivity {

    private RecyclerView recycler;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isSignedIn()) return;
        setContentView(R.layout.activity_main);
        InputUtils.disableHandwriting(getWindow().getDecorView());

        recycler = findViewById(R.id.recycler);
        empty = findViewById(R.id.tvEmpty);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        String email = session().getEmail();
        ((TextView) findViewById(R.id.tvUser)).setText(
                getString(R.string.main_user_label) + " " + (email == null ? "" : email));

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddMedActivity.class)));

        Button interactions = findViewById(R.id.btnInteractions);
        interactions.setOnClickListener(v ->
                startActivity(new Intent(this, InteractionActivity.class)));

        Button settings = findViewById(R.id.btnSettings);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        askNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSignedIn()) {
            loadData();
            rearmFutureAlarms();
        }
    }

    /** Repasse en alarme exacte les rappels encore futurs après autorisation système. */
    private void rearmFutureAlarms() {
        if (!ReminderScheduler.canScheduleExact(this)) return;
        for (Medication medication : AppDatabase.get(this).medicationDao()
                .getAllForUser(currentUid())) {
            ReminderScheduler.rearm(this, medication);
        }
    }

    private void loadData() {
        List<Medication> list = AppDatabase.get(this).medicationDao()
                .getAllForUser(currentUid());
        empty.setVisibility(list.isEmpty() ? TextView.VISIBLE : TextView.GONE);
        recycler.setAdapter(new MedAdapter(list,
                medication -> {
                    Intent intent = new Intent(this, AddMedActivity.class);
                    intent.putExtra(AddMedActivity.EXTRA_EDIT_ID, medication.id);
                    startActivity(intent);
                },
                medication -> {
                    ReminderScheduler.cancel(this, medication);
                    AppDatabase.get(this).medicationDao().delete(medication);
                    loadData();
                    Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                }));
    }

    private void askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }
}
