package com.example.medirappel;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

/** Programme UNE notification exactement 45 secondes après l'enregistrement. */
public final class ReminderScheduler {

    public static final long DELAY_MS = 45_000L;
    public static final String EXTRA_ID = "reminder_id";
    public static final String EXTRA_NAME = "reminder_name";

    private ReminderScheduler() { }

    /** Android 12+ : SCHEDULE_EXACT_ALARM doit être autorisé dans les réglages système. */
    public static boolean canScheduleExact(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager manager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager != null && manager.canScheduleExactAlarms();
    }

    /** Calcul pur, sans dépendance Android : toujours enregistrement + 45 s. */
    static long triggerAtFrom(long nowMillis) {
        return nowMillis + DELAY_MS;
    }

    private static PendingIntent pending(@NonNull Context context, @NonNull Medication medication) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(EXTRA_ID, medication.id);
        intent.putExtra(EXTRA_NAME, medication.name);
        return PendingIntent.getBroadcast(context, medication.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent openApp(@NonNull Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Calcule et programme l'alarme ; retourne l'horodatage exact programmé. */
    public static long schedule(@NonNull Context context, @NonNull Medication medication) {
        long triggerAt = triggerAtFrom(System.currentTimeMillis());
        medication.triggerAt = triggerAt;
        arm(context, medication, triggerAt);
        return triggerAt;
    }

    /** Utilisé après redémarrage : ne réarme QUE les rappels encore dans le futur. */
    public static void rearm(@NonNull Context context, @NonNull Medication medication) {
        if (medication.triggerAt > System.currentTimeMillis()) {
            arm(context, medication, medication.triggerAt);
        }
    }

    private static void arm(@NonNull Context context, @NonNull Medication medication, long triggerAt) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        try {
            if (canScheduleExact(context)) {
                alarmManager.setAlarmClock(
                        new AlarmManager.AlarmClockInfo(triggerAt, openApp(context, medication.id)),
                        pending(context, medication));
                return;
            }

            // Secours Android 12+ : le rappel reste programmé (approximativement) avec le
            // même PendingIntent, donc aucun doublon. L'UI explique et propose d'activer
            // les alarmes exactes pour retrouver le délai strict de 45 s.
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pending(context, medication));
        } catch (SecurityException e) {
            try {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAt, pending(context, medication));
            } catch (SecurityException ignored) {
            }
        }
    }

    /** Supprime l'alarme : plus aucun doublon après suppression du rappel. */
    public static void cancel(@NonNull Context context, @NonNull Medication medication) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                alarmManager.cancel(pending(context, medication));
            } catch (SecurityException ignored) {
            }
        }
    }
}
