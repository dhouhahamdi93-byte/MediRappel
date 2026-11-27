package com.example.medirappel;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Un rappel appartient à un compte local (ownerId = identifiant du compte). */
@Entity
public class Medication {
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Identifiant du compte local : filtre obligatoire dans toutes les requêtes. */
    public String ownerId = "";

    public String name;
    public String dose;          // description facultative (ancien champ conservé)

    /** Champs conservés pour compatibilité avec l'ancien schéma ; plus utilisés pour l'alarme. */
    public int hour;
    public int minute;

    /** Horodatage exact (ms) de la notification, fixé à enregistrement + 45 s. */
    public long triggerAt;
}
