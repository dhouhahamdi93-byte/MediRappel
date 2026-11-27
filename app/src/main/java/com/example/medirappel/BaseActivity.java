package com.example.medirappel;

import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

/** Activité de base : applique la langue choisie avant le chargement des ressources. */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void disableHandwriting(EditText... editTexts) {
        for (EditText editText : editTexts) {
            InputUtils.disableHandwriting(editText);
        }
    }
}
