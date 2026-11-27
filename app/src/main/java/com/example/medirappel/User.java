package com.example.medirappel;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Compte local : l'email est unique et toujours stocké en minuscules. */
@Entity(tableName = "User", indices = {@Index(value = "email", unique = true)})
public class User {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "email")
    public String email = "";

    /** Hash PBKDF2 en Base64 ; aucun mot de passe en clair n'est conservé. */
    @NonNull
    public String passwordHash = "";

    /** Sel aléatoire en Base64, propre à chaque compte. */
    @NonNull
    public String salt = "";

    public long createdAt;

    /** Nom saisi à l'inscription, conservé comme simple libellé local. */
    public String displayName;
}
