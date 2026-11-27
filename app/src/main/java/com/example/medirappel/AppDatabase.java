package com.example.medirappel;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base strictement locale. L'ancien schéma pouvant être incompatible avec les entités
 * actuelles, aucune migration manuelle n'est conservée : la base est recréée proprement.
 */
@Database(entities = {Medication.class, User.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract MedicationDao medicationDao();

    public abstract UserDao userDao();

    public static synchronized AppDatabase get(Context ctx) {
        if (instance == null) {
            instance = Room.databaseBuilder(ctx.getApplicationContext(),
                            AppDatabase.class, "dawai.db")
                    // Ancienne base incompatible : aucune donnée de production à préserver.
                    .fallbackToDestructiveMigration()
                    // Les requêtes historiques des rappels restent sur le thread appelant.
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
