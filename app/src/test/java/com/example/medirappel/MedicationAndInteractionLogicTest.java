package com.example.medirappel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests JVM du nom de médicament et de la couleur du résultat d'interaction. */
public class MedicationAndInteractionLogicTest {

    @Test
    public void medication_name_is_trimmed_before_use() {
        assertEquals("Paracétamol", MedicationNameValidator.normalize("  Paracétamol  "));
        assertEquals("Doliprane", MedicationNameValidator.normalize("\tDoliprane\n"));
    }

    @Test
    public void non_blank_medication_names_are_valid() {
        assertTrue(MedicationNameValidator.isValid("Paracétamol"));
        assertTrue(MedicationNameValidator.isValid("  Doliprane  "));
    }

    @Test
    public void null_or_blank_medication_names_are_rejected() {
        assertFalse(MedicationNameValidator.isValid(null));
        assertFalse(MedicationNameValidator.isValid(""));
        assertFalse(MedicationNameValidator.isValid("   \t\n  "));
        assertEquals("", MedicationNameValidator.normalize(null));
        assertEquals("", MedicationNameValidator.normalize("   "));
    }

    @Test
    public void network_error_takes_priority_over_every_other_state() {
        assertEquals(InteractionOutcome.Tone.GRAY,
                InteractionOutcome.tone(true, true, true, true, true));
        assertEquals(InteractionOutcome.Tone.GRAY,
                InteractionOutcome.tone(true, false, false, false, false));
    }

    @Test
    public void http_empty_and_missing_drug_results_are_orange() {
        assertEquals(InteractionOutcome.Tone.ORANGE,
                InteractionOutcome.tone(false, true, false, false, false));
        assertEquals(InteractionOutcome.Tone.ORANGE,
                InteractionOutcome.tone(false, false, true, false, false));
        assertEquals(InteractionOutcome.Tone.ORANGE,
                InteractionOutcome.tone(false, false, false, true, false));
    }

    @Test
    public void successful_lookup_is_red_when_found_and_green_when_not_mentioned() {
        assertEquals(InteractionOutcome.Tone.RED,
                InteractionOutcome.tone(false, false, false, false, true));
        assertEquals(InteractionOutcome.Tone.GREEN,
                InteractionOutcome.tone(false, false, false, false, false));
    }
}
