package com.example.medirappel;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Insert
    long insert(User user);

    /** Recherche normalisée : l'appelant fournit toujours un email en minuscules. */
    @Query("SELECT * FROM User WHERE email = :email LIMIT 1")
    User findByEmail(String email);
}
