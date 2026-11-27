package com.example.medirappel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests JVM locaux : logique pure sans framework Android. */
public class ReminderLogicTest {

    @Test
    public void trigger_at_is_always_now_plus_forty_five_seconds() {
        assertEquals(46_000L, ReminderScheduler.triggerAtFrom(1_000L));
        assertEquals(45_000L, ReminderScheduler.triggerAtFrom(0L));
        assertEquals(1_045_000L, ReminderScheduler.triggerAtFrom(1_000_000L));
    }

    @Test
    public void reminder_delay_constant_is_forty_five_seconds() {
        assertEquals(45_000L, ReminderScheduler.DELAY_MS);
    }

    @Test
    public void trigger_at_is_monotonic_with_constant_offset() {
        long first = ReminderScheduler.triggerAtFrom(1_700_000_000_000L);
        long second = ReminderScheduler.triggerAtFrom(1_700_000_000_001L);
        assertEquals(1L, second - first);
        assertEquals(ReminderScheduler.DELAY_MS,
                first - 1_700_000_000_000L);
    }

    @Test
    public void password_rules_require_eight_chars_letter_and_digit() {
        assertTrue(AuthHelper.isStrongPassword("MediRappel2026"));
        assertFalse(AuthHelper.isStrongPassword("MediRappel")); // pas de chiffre
        assertFalse(AuthHelper.isStrongPassword("12345678"));   // pas de lettre
        assertFalse(AuthHelper.isStrongPassword("Med1"));       // trop court
        assertFalse(AuthHelper.isStrongPassword(null));
    }
}
