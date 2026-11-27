package com.example.medirappel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Client openFDA pour les notices (champ drug_interactions). */
public interface FdaApi {

    @GET("drug/label.json")
    Call<FdaResponse> getLabel(@Query("search") String search, @Query("limit") int limit);

    class FdaResponse {
        public List<Result> results;
    }

    class Result {
        @SerializedName("drug_interactions")
        public JsonElement drugInteractions;

        /** openFDA renvoie tantôt une liste, tantôt une chaîne : on gère les deux. */
        public String interactionsText() {
            if (drugInteractions == null || drugInteractions.isJsonNull()) return "";
            if (drugInteractions.isJsonPrimitive()) return drugInteractions.getAsString();
            if (drugInteractions.isJsonArray()) {
                JsonArray array = drugInteractions.getAsJsonArray();
                StringBuilder builder = new StringBuilder();
                for (JsonElement element : array) {
                    if (element == null || element.isJsonNull()) continue;
                    if (builder.length() > 0) builder.append(' ');
                    builder.append(element.getAsString());
                }
                return builder.toString();
            }
            return "";
        }
    }

    class Client {
        private static FdaApi api;

        public static synchronized FdaApi get() {
            if (api == null) {
                api = new Retrofit.Builder()
                        .baseUrl("https://api.fda.gov/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(FdaApi.class);
            }
            return api;
        }
    }
}
