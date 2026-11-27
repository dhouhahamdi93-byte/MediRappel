package com.example.medirappel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Après redémarrage, ne conserve que les alarmes dont le délai n'est pas encore écoulé. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        for (Medication medication : AppDatabase.get(context).medicationDao().getAll()) {
            ReminderScheduler.rearm(context, medication);
        }
    }
}
