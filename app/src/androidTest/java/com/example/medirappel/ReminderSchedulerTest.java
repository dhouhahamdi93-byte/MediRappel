package com.example.medirappel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Vérifie sur appareil/émulateur la règle permanente : enregistrement + 45 s. */
@RunWith(AndroidJUnit4.class)
public class ReminderSchedulerTest {

    @Test
    public void schedule_setsAlarmExactlyFortyFiveSecondsLater() {
        Context context = ApplicationProvider.getApplicationContext();
        Medication medication = new Medication();
        medication.id = 987654;
        medication.name = "Test 45s";

        long before = System.currentTimeMillis();
        long triggerAt = ReminderScheduler.schedule(context, medication);
        long after = System.currentTimeMillis();

        assertTrue(triggerAt >= before + ReminderScheduler.DELAY_MS);
        assertTrue(triggerAt <= after + ReminderScheduler.DELAY_MS + 1000L);
        assertEquals(triggerAt, medication.triggerAt);

        ReminderScheduler.cancel(context, medication);
    }
}
