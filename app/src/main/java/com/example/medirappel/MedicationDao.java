package com.example.medirappel;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MedicationDao {

    @Insert
    long insert(Medication m);

    @Update
    void update(Medication m);

    @Delete
    void delete(Medication m);

    /** Toutes les requêtes d'écran sont filtrées par propriétaire. */
    @Query("SELECT * FROM Medication WHERE ownerId = :ownerId ORDER BY triggerAt DESC, id DESC")
    List<Medication> getAllForUser(String ownerId);

    @Query("SELECT * FROM Medication WHERE id = :id AND ownerId = :ownerId")
    Medication getByIdForUser(int id, String ownerId);

    /** Utilisé uniquement par le receiver système (alarme déjà programmée). */
    @Query("SELECT * FROM Medication WHERE id = :id")
    Medication getById(int id);

    @Query("SELECT * FROM Medication")
    List<Medication> getAll();
}
