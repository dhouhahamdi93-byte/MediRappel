package com.example.medirappel;

/** Décision de présentation d'un résultat d'interaction, sans dépendance Android. */
public final class InteractionOutcome {

    /** Teinte de la carte : rouge = trouvée, vert = aucune, gris = réseau, orange = autre erreur. */
    public enum Tone { RED, GREEN, GRAY, ORANGE }

    private InteractionOutcome() { }

    /**
     * Les erreurs priment sur le résultat : un échec réseau reste gris, une autre erreur
     * reste orange, et seuls les deux appels réussis peuvent donner rouge ou vert.
     */
    public static Tone tone(boolean networkError, boolean httpError,
                            boolean emptyResult, boolean missingDrug,
                            boolean interactionFound) {
        if (networkError) return Tone.GRAY;
        if (httpError || emptyResult || missingDrug) return Tone.ORANGE;
        return interactionFound ? Tone.RED : Tone.GREEN;
    }
}
