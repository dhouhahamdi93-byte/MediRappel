package com.example.medirappel;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Vérification d'interaction à partir des notices openFDA, avec résultats FR + AR. */
public class InteractionActivity extends SecureActivity {

    private EditText etA, etB;
    private ProgressBar progress;
    private Button check;
    private CardView card;
    private TextView tvResultFr, tvResultAr, tvDisclaimer;
    private TextView tvSourceLabel, tvExcerpt, tvTranslation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isSignedIn()) return;
        setContentView(R.layout.activity_interaction);
        InputUtils.disableHandwriting(getWindow().getDecorView());

        etA = findViewById(R.id.etDrugA);
        etB = findViewById(R.id.etDrugB);
        progress = findViewById(R.id.progress);
        check = findViewById(R.id.btnCheck);
        card = findViewById(R.id.cardResult);
        tvResultFr = findViewById(R.id.tvResultFr);
        tvResultAr = findViewById(R.id.tvResultAr);
        tvDisclaimer = findViewById(R.id.tvDisclaimer);
        tvSourceLabel = findViewById(R.id.tvSourceLabel);
        tvExcerpt = findViewById(R.id.tvExcerpt);
        tvTranslation = findViewById(R.id.tvTranslation);

        check.setOnClickListener(v -> runCheck());
    }

    private void runCheck() {
        String a = clean(etA);
        String b = clean(etB);
        etA.setError(null);
        etB.setError(null);

        if (a.isEmpty() || b.isEmpty()) {
            if (a.isEmpty()) etA.setError(getString(R.string.interaction_error_empty));
            if (b.isEmpty()) etB.setError(getString(R.string.interaction_error_empty));
            Toast.makeText(this, R.string.interaction_error_empty, Toast.LENGTH_SHORT).show();
            return; // aucune recherche si un champ est vide
        }

        setLoading(true);
        card.setVisibility(View.GONE);
        fetch(a, first -> fetch(b, second -> {
            setLoading(false);
            evaluate(a, b, first, second);
        }));
    }

    private String clean(EditText field) {
        String value = field.getText() == null ? "" : field.getText().toString().trim();
        return value.replaceAll("[^a-zA-Z0-9 -]", "").trim();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        check.setEnabled(!loading);
    }

    // ---------- Appel openFDA ----------
    private static final int STATUS_OK = 0;
    private static final int STATUS_NOT_FOUND = 1;
    private static final int STATUS_EMPTY = 2;
    private static final int STATUS_NETWORK = 3;
    private static final int STATUS_HTTP = 4;

    private static class FetchResult {
        final int status;
        final String text;
        final int code;

        FetchResult(int status, String text, int code) {
            this.status = status;
            this.text = text;
            this.code = code;
        }

        static FetchResult ok(String text) { return new FetchResult(STATUS_OK, text, 0); }
        static FetchResult notFound() { return new FetchResult(STATUS_NOT_FOUND, null, 0); }
        static FetchResult empty(int code) { return new FetchResult(STATUS_EMPTY, null, code); }
        static FetchResult network() { return new FetchResult(STATUS_NETWORK, null, 0); }
        static FetchResult http(int code) { return new FetchResult(STATUS_HTTP, null, code); }
    }

    private interface OnFetch { void done(FetchResult result); }

    private void fetch(String drug, OnFetch callback) {
        String query = "openfda.generic_name:\"" + drug + "\" OR openfda.brand_name:\"" + drug + "\"";
        FdaApi.Client.get().getLabel(query, 1).enqueue(new Callback<FdaApi.FdaResponse>() {
            @Override
            public void onResponse(Call<FdaApi.FdaResponse> call,
                                   Response<FdaApi.FdaResponse> response) {
                if (response.code() == 404) {
                    callback.done(FetchResult.notFound()); // openFDA : aucun résultat
                    return;
                }
                if (!response.isSuccessful()) {
                    callback.done(FetchResult.http(response.code()));
                    return;
                }
                FdaApi.FdaResponse body = response.body();
                if (body == null || body.results == null || body.results.isEmpty()) {
                    callback.done(FetchResult.empty(response.code()));
                    return;
                }
                callback.done(FetchResult.ok(body.results.get(0).interactionsText()));
            }

            @Override
            public void onFailure(Call<FdaApi.FdaResponse> call, Throwable t) {
                callback.done(FetchResult.network());
            }
        });
    }

    // ---------- Interprétation ----------
    private void evaluate(String a, String b, FetchResult first, FetchResult second) {
        boolean networkError = first.status == STATUS_NETWORK || second.status == STATUS_NETWORK;
        boolean httpError = first.status == STATUS_HTTP || second.status == STATUS_HTTP;
        boolean emptyResult = first.status == STATUS_EMPTY || second.status == STATUS_EMPTY;
        boolean missingDrug = first.status == STATUS_NOT_FOUND || second.status == STATUS_NOT_FOUND;

        if (networkError) {
            showTinted(InteractionOutcome.Tone.GRAY,
                    getString(R.string.result_error_network_fr),
                    getString(R.string.result_error_network_ar), "", null);
            return;
        }
        if (httpError) {
            int code = first.status == STATUS_HTTP ? first.code : second.code;
            showTinted(InteractionOutcome.Tone.ORANGE,
                    getString(R.string.result_error_http_fr, code),
                    getString(R.string.result_error_http_ar, code), "", null);
            return;
        }
        if (emptyResult) {
            showTinted(InteractionOutcome.Tone.ORANGE,
                    getString(R.string.result_error_empty_fr),
                    getString(R.string.result_error_empty_ar), "", null);
            return;
        }
        if (missingDrug) {
            String missing = first.status == STATUS_NOT_FOUND ? a : b;
            showTinted(InteractionOutcome.Tone.ORANGE,
                    getString(R.string.result_missing_fr, missing),
                    getString(R.string.result_missing_ar, missing), "", null);
            return;
        }

        String hit = snippet(first.text, b);
        if (hit == null) hit = snippet(second.text, a);
        InteractionOutcome.Tone tone = InteractionOutcome.tone(
                false, false, false, false, hit != null);

        if (hit == null) {
            // On ne dit PAS qu'il n'y a aucun risque : seulement qu'aucune interaction
            // n'est mentionnée dans les notices consultées. La carte passe au vert clair.
            showTinted(tone,
                    getString(R.string.result_no_mention_fr, a, b),
                    getString(R.string.result_no_mention_ar, a, b),
                    getString(R.string.result_no_risk_warning_fr) + "\n"
                            + getString(R.string.result_no_risk_warning_ar),
                    null);
        } else {
            String excerpt = hit.replaceAll("\\s+", " ").trim();
            showTinted(tone,
                    getString(R.string.result_found_fr, a, b),
                    getString(R.string.result_found_ar, a, b), "", excerpt);
        }
    }

    /** Applique la teinte décidée par la logique pure, sans toucher aux textes FR/AR. */
    private void showTinted(InteractionOutcome.Tone tone, String french, String arabic,
                            String note, String excerpt) {
        int[] palette = palette(tone);
        showBilingual(french, arabic, note, excerpt, palette[0], palette[1]);
    }

    private int[] palette(InteractionOutcome.Tone tone) {
        switch (tone) {
            case RED:
                return new int[]{R.color.error_bg, R.color.error_fg};
            case GREEN:
                return new int[]{R.color.ok_bg, R.color.ok_fg};
            case GRAY:
                return new int[]{R.color.network_bg, R.color.network_fg};
            case ORANGE:
            default:
                return new int[]{R.color.warn_bg, R.color.warn_fg};
        }
    }

    private String snippet(String text, String word) {
        if (text == null || word == null) return null;
        int index = text.toLowerCase().indexOf(word.toLowerCase());
        if (index < 0) return null;
        int start = Math.max(0, index - 120);
        int end = Math.min(text.length(), index + word.length() + 200);
        return (start > 0 ? "…" : "") + text.substring(start, end)
                + (end < text.length() ? "…" : "");
    }

    private void showBilingual(String french, String arabic, String note,
                               String excerpt, int bgColor, int fgColor) {
        int fg = ContextCompat.getColor(this, fgColor);
        card.setCardBackgroundColor(ContextCompat.getColor(this, bgColor));
        tvResultFr.setTextColor(fg);
        tvResultAr.setTextColor(fg);
        tvDisclaimer.setTextColor(fg);
        tvResultFr.setText(getString(R.string.result_label_fr) + " : " + french);
        tvResultAr.setText(getString(R.string.result_label_ar) + " : " + arabic);

        String disclaimer = note.isEmpty() ? "" : note + "\n";
        tvDisclaimer.setText(disclaimer + getString(R.string.result_disclaimer_fr)
                + "\n" + getString(R.string.result_disclaimer_ar));

        boolean hasExcerpt = excerpt != null && !excerpt.isEmpty();
        tvSourceLabel.setVisibility(hasExcerpt ? View.VISIBLE : View.GONE);
        tvExcerpt.setVisibility(hasExcerpt ? View.VISIBLE : View.GONE);
        tvTranslation.setVisibility(hasExcerpt ? View.VISIBLE : View.GONE);
        if (hasExcerpt) {
            tvSourceLabel.setText(getString(R.string.result_source_label));
            tvExcerpt.setText(excerpt);
            tvTranslation.setText(getString(R.string.result_label_ar) + " : "
                    + getString(R.string.result_translation_pending));
            translate(excerpt);
        }
        card.setVisibility(View.VISIBLE);
    }

    /** Traduction best effort : son échec ne remet pas en cause le résultat openFDA. */
    private void translate(String excerpt) {
        String query = excerpt.length() > 450 ? excerpt.substring(0, 450) : excerpt;
        MyMemoryApi.Client.get().translate(query, "en|ar")
                .enqueue(new Callback<MyMemoryApi.Resp>() {
                    @Override
                    public void onResponse(Call<MyMemoryApi.Resp> call,
                                           Response<MyMemoryApi.Resp> response) {
                        MyMemoryApi.Resp body = response.body();
                        if (response.isSuccessful() && body != null
                                && body.responseData != null
                                && body.responseData.translatedText != null
                                && !body.responseData.translatedText.isEmpty()) {
                            tvTranslation.setText(getString(R.string.result_label_ar) + " : "
                                    + body.responseData.translatedText);
                        } else {
                            tvTranslation.setText(getString(R.string.result_label_ar) + " : "
                                    + getString(R.string.result_translation_failed));
                        }
                    }

                    @Override
                    public void onFailure(Call<MyMemoryApi.Resp> call, Throwable t) {
                        tvTranslation.setText(getString(R.string.result_label_ar) + " : "
                                + getString(R.string.result_translation_failed));
                    }
                });
    }
}
