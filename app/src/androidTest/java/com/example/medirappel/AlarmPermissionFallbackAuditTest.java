package com.example.medirappel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Teste les DEUX chemins de programmation d'un rappel sur Android 12+ :
 *  - droit accordé : setAlarmClock (chemin exact) ;
 *  - droit refusé : setAndAllowWhileIdle (secours), puis reprise en exact après autorisation.
 *
 * Le refus doit être appliqué PAR LE SCRIPT avant l'instrumentation : révoquer
 * SCHEDULE_EXACT_ALARM pendant que l'application tourne fait tuer son processus
 * par Android (comportement système observé et documenté).
 * Exécution du secours :
 *   adb shell am instrument -w -e class com.example.medirappel.AlarmPermissionFallbackAuditTest \
 *     com.example.medirappel.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4.class)
public class AlarmPermissionFallbackAuditTest {

    private static final String TAG = "AuditFallback";
    private final Context app = ApplicationProvider.getApplicationContext();

    @BeforeClass
    public static void grantNotifications() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission("com.example.medirappel", Manifest.permission.POST_NOTIFICATIONS);
    }

    private String shell(String command) throws Exception {
        ParcelFileDescriptor pfd = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand(command);
        StringBuilder out = new StringBuilder();
        try (InputStream in = new FileInputStream(pfd.getFileDescriptor())) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.append(new String(buffer, 0, read));
            }
        }
        return out.toString();
    }

    @Test
    public void exact_alarm_denied_still_delivers_reminder_and_rearm_is_safe() throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;

        String pkg = app.getPackageName();
        NotificationManager nm =
                (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        assertNotNull(nm);

        boolean exactAllowed = ReminderScheduler.canScheduleExact(app);
        String appops = shell("appops get " + pkg + " SCHEDULE_EXACT_ALARM").trim();
        Log.d(TAG, "sdk=" + Build.VERSION.SDK_INT + " pkg=" + pkg
                + " canScheduleExactAlarms=" + exactAllowed + " appops=[" + appops + "]");

        NotificationChannel channel = nm.getNotificationChannel(ReminderReceiver.CHANNEL_ID);
        Log.d(TAG, "channelBefore importance="
                + (channel == null ? "none" : channel.getImportance()));

        LocaleHelper.setLanguage(app, LocaleHelper.FRENCH);
        Medication medication = new Medication();
        medication.id = 772001;
        medication.name = "SecoursAudit";

        try {
            long before = System.currentTimeMillis();
            long triggerAt = ReminderScheduler.schedule(app, medication);
            long after = System.currentTimeMillis();
            assertEquals(triggerAt, medication.triggerAt);
            assertTrue("triggerAt doit être dans la fenêtre +45 s",
                    triggerAt >= before + ReminderScheduler.DELAY_MS
                            && triggerAt <= after + ReminderScheduler.DELAY_MS);
            Log.d(TAG, "schedule before=" + before + " after=" + after
                    + " triggerAt=" + triggerAt + " delay=" + (triggerAt - before));

            String alarms = shell("dumpsys alarm");
            for (String line : alarms.split("\n")) {
                if (line.contains(pkg) || line.contains(Long.toString(triggerAt))) {
                    Log.d(TAG, "ALARM " + line.trim());
                }
            }
            assertTrue("L'horodatage doit être enregistré dans AlarmManager",
                    alarms.contains(Long.toString(triggerAt)));
            assertTrue("Le PendingIntent du récepteur doit être enregistré",
                    alarms.contains(pkg + "/.ReminderReceiver"));

            if (exactAllowed) {
                // Suite normale : le droit est accordé, on vérifie le chemin exact.
                // Le chemin de secours est vérifié séparément avec appops=deny.
                assertTrue("setAlarmClock doit être utilisé quand le droit est accordé",
                        alarms.contains("Alarm clock:"));
                Log.d(TAG, "MODE=exact : setAlarmClock confirmé (secours testé à part)");
                return;
            }

            // Chemin de secours : setAndAllowWhileIdle est INEXACT, le système
            // regroupe les alarmes (délai réel mesuré ~105 s). Fenêtre de 120 s
            // fondée sur cette mesure, sans modifier le comportement applicatif.
            long deadline = System.currentTimeMillis() + 120_000L;
            StatusBarNotification notification = null;
            long observed = -1;
            while (System.currentTimeMillis() < deadline && notification == null) {
                for (StatusBarNotification n : nm.getActiveNotifications()) {
                    if (n.getId() == medication.id) {
                        notification = n;
                        observed = System.currentTimeMillis();
                        break;
                    }
                }
                if (notification == null) Thread.sleep(500);
            }
            Log.d(TAG, "waitEnd now=" + System.currentTimeMillis()
                    + " observed=" + observed
                    + " realDelayMs=" + (observed < 0 ? -1 : observed - before)
                    + " activeNotifications=" + nm.getActiveNotifications().length);

            assertNotNull("Le secours approximatif doit afficher la notification", notification);
            String text = notification.getNotification().extras.getString(Notification.EXTRA_TEXT);
            assertNotNull(text);
            assertTrue(text.contains("SecoursAudit"));
            // Seul ReminderReceiver publie cette notification : sa présence prouve son invocation.
            Log.d(TAG, "NOTIFICATION posted id=" + notification.getId() + " text=[" + text + "]");

            NotificationChannel channelAfter = nm.getNotificationChannel(ReminderReceiver.CHANNEL_ID);
            Log.d(TAG, "channelAfter importance="
                    + (channelAfter == null ? "none" : channelAfter.getImportance()));
            assertNotNull("Le canal doit exister après la notification", channelAfter);
            assertTrue("Le canal doit être actif",
                    channelAfter.getImportance() != NotificationManager.IMPORTANCE_NONE);
            for (String line : shell("dumpsys alarm").split("\n")) {
                if (line.contains(pkg) || line.contains("ReminderReceiver")
                        || line.contains("alarm_fired")) {
                    Log.d(TAG, "ALARM_HISTORY " + line.trim());
                }
            }

            // Octroi à chaud : autoriser n'est pas une révocation, le processus survit.
            shell("appops set " + pkg + " SCHEDULE_EXACT_ALARM allow");
            assertTrue("Le droit doit être restauré après autorisation",
                    ReminderScheduler.canScheduleExact(app));
            medication.triggerAt = System.currentTimeMillis() + ReminderScheduler.DELAY_MS;
            ReminderScheduler.rearm(app, medication); // repasse en setAlarmClock sans exception
            String afterGrant = shell("dumpsys alarm");
            assertTrue("La reprogrammation doit repasser en alarme exacte",
                    afterGrant.contains("Alarm clock:") && afterGrant.contains(pkg + "/.ReminderReceiver"));
            Log.d(TAG, "REARM after grant OK canScheduleExact="
                    + ReminderScheduler.canScheduleExact(app));
        } finally {
            shell("appops set " + pkg + " SCHEDULE_EXACT_ALARM allow");
            ReminderScheduler.cancel(app, medication);
            nm.cancel(medication.id);
            LocaleHelper.setLanguage(app, LocaleHelper.SYSTEM);
        }
    }
}
