package com.example.medirappel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.service.notification.StatusBarNotification;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/** Audit fonctionnel sur appareil, avec des comptes et rappels strictement locaux. */
@RunWith(AndroidJUnit4.class)
public class AuditInstrumentedTest {

    private final Context app = ApplicationProvider.getApplicationContext();

    @BeforeClass
    public static void grantNotifications() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission("com.example.medirappel", Manifest.permission.POST_NOTIFICATIONS);
    }

    @Test
    public void reminders_are_isolated_per_owner_and_crud_works() {
        MedicationDao dao = AppDatabase.get(app).medicationDao();
        String ownerA = "audit-user-A-" + System.nanoTime();
        String ownerB = "audit-user-B-" + System.nanoTime();

        Medication a = new Medication();
        a.ownerId = ownerA;
        a.name = "MedicamentA";
        a.dose = "1 comprime";
        int idA = (int) dao.insert(a);
        a.id = idA; // Room ne renseigne pas toujours la clé sur l'entité à l'insertion

        Medication b = new Medication();
        b.ownerId = ownerB;
        b.name = "MedicamentB";
        dao.insert(b);

        List<Medication> listA = dao.getAllForUser(ownerA);
        List<Medication> listB = dao.getAllForUser(ownerB);
        assertEquals(1, listA.size());
        assertEquals(1, listB.size());
        assertEquals("MedicamentA", listA.get(0).name);
        assertEquals("MedicamentB", listB.get(0).name);

        a.name = "MedicamentA-Modifie";
        dao.update(a);
        assertEquals("MedicamentA-Modifie", dao.getByIdForUser(idA, ownerA).name);
        assertEquals(null, dao.getByIdForUser(idA, ownerB));

        dao.delete(a);
        assertTrue(dao.getAllForUser(ownerA).isEmpty());
        assertEquals(1, dao.getAllForUser(ownerB).size());

        dao.delete(b);
    }

    @Test
    public void notification_is_displayed_with_localized_text_and_drug_name() {
        NotificationManager manager =
                (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        assertNotNull(manager);

        LocaleHelper.setLanguage(app, LocaleHelper.FRENCH);
        fireReminder(990001, "ParacetamolAudit");
        StatusBarNotification fr = findNotification(manager, 990001);
        assertNotNull(fr);
        assertEquals(LocaleHelper.wrap(app).getString(R.string.notif_title),
                fr.getNotification().extras.getString(Notification.EXTRA_TITLE));
        assertTrue(fr.getNotification().extras.getString(Notification.EXTRA_TEXT)
                .contains("ParacetamolAudit"));
        manager.cancel(990001);

        LocaleHelper.setLanguage(app, LocaleHelper.ARABIC);
        fireReminder(990002, "IbuprofeneAudit");
        StatusBarNotification ar = findNotification(manager, 990002);
        assertNotNull(ar);
        assertEquals(LocaleHelper.wrap(app).getString(R.string.notif_title),
                ar.getNotification().extras.getString(Notification.EXTRA_TITLE));
        assertTrue(ar.getNotification().extras.getString(Notification.EXTRA_TEXT)
                .contains("IbuprofeneAudit"));
        manager.cancel(990002);

        LocaleHelper.setLanguage(app, LocaleHelper.SYSTEM);
    }

    private void fireReminder(int id, String name) {
        Intent intent = new Intent(app, ReminderReceiver.class);
        intent.setComponent(new ComponentName(app, ReminderReceiver.class));
        intent.putExtra(ReminderScheduler.EXTRA_ID, id);
        intent.putExtra(ReminderScheduler.EXTRA_NAME, name);
        app.sendBroadcast(intent);
        // Le BroadcastReceiver est synchrône sur le thread principal : on attend le post.
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private StatusBarNotification findNotification(NotificationManager manager, int id) {
        for (StatusBarNotification notification : manager.getActiveNotifications()) {
            if (notification.getId() == id) return notification;
        }
        return null;
    }

    @Test
    public void language_selection_persists_and_sets_rtl_for_arabic() {
        LocaleHelper.setLanguage(app, LocaleHelper.ARABIC);
        assertEquals(LocaleHelper.ARABIC, LocaleHelper.getLanguage(app));
        Context arabic = LocaleHelper.wrap(app);
        assertEquals(View.LAYOUT_DIRECTION_RTL,
                arabic.getResources().getConfiguration().getLayoutDirection());
        assertFalse(arabic.getString(R.string.login_button)
                .equals(app.getString(R.string.login_button)));

        LocaleHelper.setLanguage(app, LocaleHelper.FRENCH);
        assertEquals(LocaleHelper.FRENCH, LocaleHelper.getLanguage(app));
        assertEquals(View.LAYOUT_DIRECTION_LTR,
                LocaleHelper.wrap(app).getResources().getConfiguration().getLayoutDirection());
        LocaleHelper.setLanguage(app, LocaleHelper.SYSTEM);
    }

    @Test
    public void openfda_label_endpoint_returns_interaction_data() throws Exception {
        FdaApi.FdaResponse response = FdaApi.Client.get()
                .getLabel("openfda.generic_name:\"warfarin\"", 1).execute().body();
        assertNotNull(response);
        assertNotNull(response.results);
        assertFalse(response.results.isEmpty());
        assertNotNull(response.results.get(0).interactionsText());
        assertFalse(response.results.get(0).interactionsText().isEmpty());
    }

    @Test
    public void translation_endpoint_returns_arabic_text() throws Exception {
        MyMemoryApi.Resp response = MyMemoryApi.Client.get()
                .translate("drugs that increase bleeding risk", "en|ar").execute().body();
        assertNotNull(response);
        assertNotNull(response.responseData);
        assertNotNull(response.responseData.translatedText);
        assertFalse(response.responseData.translatedText.isEmpty());
    }
}
