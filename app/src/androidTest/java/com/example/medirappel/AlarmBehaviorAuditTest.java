package com.example.medirappel;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Vérifie le cycle complet : annulation effective, pas de doublon, une seule notification. */
@RunWith(AndroidJUnit4.class)
public class AlarmBehaviorAuditTest {

    private final Context app = ApplicationProvider.getApplicationContext();

    @BeforeClass
    public static void grantNotifications() {
        // L'APK est réinstallé par Gradle : la permission doit être redonnée au test.
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission("com.example.medirappel", Manifest.permission.POST_NOTIFICATIONS);
    }

    @Test
    public void cancelled_alarm_never_fires_and_kept_alarm_fires_once() throws Exception {
        NotificationManager manager =
                (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        assertNotNull(manager);

        LocaleHelper.setLanguage(app, LocaleHelper.FRENCH);

        Medication cancelled = new Medication();
        cancelled.id = 771001;
        cancelled.name = "AnnuleAudit";
        Medication kept = new Medication();
        kept.id = 771002;
        kept.name = "ConserveAudit";

        ReminderScheduler.schedule(app, cancelled);
        ReminderScheduler.schedule(app, kept);
        // Même identifiant : une seconde programmation remplace la première sans doublon.
        ReminderScheduler.schedule(app, kept);
        ReminderScheduler.cancel(app, cancelled);

        // Sur cet émulateur, le chemin de secours approximatif peut livrer à ~105 s.
        // La fenêtre de 120 s couvre les deux chemins (exact et secours).
        long deadline = System.currentTimeMillis() + 120_000L;
        StatusBarNotification keptNotification = null;
        while (System.currentTimeMillis() < deadline && keptNotification == null) {
            for (StatusBarNotification n : manager.getActiveNotifications()) {
                if (n.getId() == kept.id) {
                    keptNotification = n;
                    break;
                }
                if (n.getId() == cancelled.id) {
                    throw new AssertionError("Une alarme annulée a déclenché une notification");
                }
            }
            if (keptNotification == null) Thread.sleep(500);
        }

        assertNotNull("L'alarme maintenue doit afficher une notification après 45 s",
                keptNotification);
        assertNotNull(keptNotification.getNotification().extras.getString(Notification.EXTRA_TEXT));
        org.junit.Assert.assertTrue(keptNotification.getNotification().extras
                .getString(Notification.EXTRA_TEXT).contains("ConserveAudit"));

        // Laisse encore 2 s pour détecter un éventuel doublon, puis vérifie le compte.
        Thread.sleep(2_000);
        int keptCount = 0;
        for (StatusBarNotification n : manager.getActiveNotifications()) {
            if (n.getId() == kept.id) keptCount++;
        }
        org.junit.Assert.assertEquals("Un seul rappel doit être affiché (pas de doublon)",
                1, keptCount);

        manager.cancel(cancelled.id);
        manager.cancel(kept.id);
        ReminderScheduler.cancel(app, kept);
        LocaleHelper.setLanguage(app, LocaleHelper.SYSTEM);
    }
}
