package com.example.medirappel;

/** Validation pure du nom de médicament, réutilisable par l'UI et les tests JVM. */
public final class MedicationNameValidator {

    private MedicationNameValidator() { }

    /** Un nom nul ou composé seulement d'espaces devient une chaîne vide. */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.trim();
    }

    /** Vrai uniquement si le nom contient au moins un caractère après trim. */
    public static boolean isValid(String raw) {
        return !normalize(raw).isEmpty();
    }
}
