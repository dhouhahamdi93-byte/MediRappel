package com.example.medirappel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/** Affiche la notification locale 45 secondes après l'enregistrement. */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "medirappel_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(ReminderScheduler.EXTRA_ID, 0);
        String name = intent.getStringExtra(ReminderScheduler.EXTRA_NAME);
        if (name == null || name.isEmpty()) {
            Medication medication = AppDatabase.get(context).medicationDao().getById(id);
            name = medication == null ? "" : medication.name;
        }
        if (name == null || name.isEmpty()) return;

        // Utilise la langue choisie, même si l'application est fermée.
        Context localized = LocaleHelper.wrap(context);
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        createChannel(manager, localized);
        PendingIntent open = PendingIntent.getActivity(context, id,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(localized, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(localized.getString(R.string.notif_title))
                .setContentText(localized.getString(R.string.notif_message, name))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(localized.getString(R.string.notif_message, name)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true);

        try {
            manager.notify(id, builder.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS refusée : aucun affichage, aucune donnée modifiée.
        }
        // Pas de reprogrammation : la règle est "45 s après Enregistrer", une seule fois.
    }

    private static void createChannel(NotificationManager manager, Context localized) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                localized.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(localized.getString(R.string.notif_channel_desc));
        manager.createNotificationChannel(channel);
    }
}
