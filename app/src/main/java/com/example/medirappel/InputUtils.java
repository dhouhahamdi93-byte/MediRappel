package com.example.medirappel;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/** Désactive l'écriture manuscrite au stylet sur tous les champs de texte de l'écran. */
public final class InputUtils {

    private InputUtils() { }

    /** Parcourt récursivement la hiérarchie et désactive l'écriture manuscrite des EditText. */
    public static void disableHandwriting(View root) {
        if (root == null) return;
        if (root instanceof EditText && Build.VERSION.SDK_INT >= 34) {
            root.setAutoHandwritingEnabled(false);
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                disableHandwriting(group.getChildAt(i));
            }
        }
    }
}
