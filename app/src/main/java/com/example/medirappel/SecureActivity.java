package com.example.medirappel;

import android.content.Intent;
import android.os.Bundle;

/** Empêche tout accès direct aux écrans protégés sans session locale active. */
public abstract class SecureActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isSignedIn()) {
            goToLogin();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isSignedIn()) goToLogin();
    }

    protected boolean isSignedIn() {
        return session().isLoggedIn();
    }

    /** Identifiant du compte local : utilisé comme ownerId de tous les rappels. */
    protected String currentUid() {
        return Long.toString(session().getUserId());
    }

    protected SessionManager session() {
        return new SessionManager(this);
    }

    protected void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
