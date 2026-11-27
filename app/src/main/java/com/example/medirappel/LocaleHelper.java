package com.example.medirappel;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;

import java.util.Locale;

/** Gère la langue choisie dans l'application (fr, ar ou langue du système). */
public final class LocaleHelper {

    public static final String PREFS_NAME = "medirappel_settings";
    public static final String KEY_LANGUAGE = "language";
    public static final String FRENCH = "fr";
    public static final String ARABIC = "ar";
    public static final String SYSTEM = "";

    private LocaleHelper() { }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** "" = langue du système, sinon "fr" ou "ar". */
    public static String getLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, SYSTEM);
    }

    public static void setLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_LANGUAGE, language).apply();
    }

    public static Locale locale(Context context) {
        String language = getLanguage(context);
        if (FRENCH.equals(language)) return Locale.FRENCH;
        if (ARABIC.equals(language)) return new Locale(ARABIC);
        return Locale.getDefault();
    }

    /** À appeler depuis attachBaseContext() de chaque activité. */
    @SuppressWarnings("deprecation")
    public static Context wrap(Context context) {
        Locale locale = locale(context);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        // setLocales() met aussi à jour la direction de mise en page (RTL pour l'arabe).
        config.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(config);
    }

    /** Pour les BroadcastReceiver qui n'ont pas d'activité. */
    public static android.content.res.Resources resources(Context context) {
        return wrap(context).getResources();
    }
}
