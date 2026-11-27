package com.example.medirappel;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Traduction facultative de l'extrait anglais vers l'arabe. */
public interface MyMemoryApi {

    @GET("get")
    Call<Resp> translate(@Query("q") String text, @Query("langpair") String langpair);

    class Resp {
        public int responseStatus;
        public ResponseData responseData;
    }

    class ResponseData {
        public String translatedText;
    }

    class Client {
        private static MyMemoryApi api;

        public static synchronized MyMemoryApi get() {
            if (api == null) {
                api = new Retrofit.Builder()
                        .baseUrl("https://api.mymemory.translated.net/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(MyMemoryApi.class);
            }
            return api;
        }
    }
}
