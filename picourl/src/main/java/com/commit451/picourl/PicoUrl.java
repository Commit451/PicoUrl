package com.commit451.picourl;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Blah
 */
public class PicoUrl {

    private static TinyUrl sTinyUrl;

    public static TinyUrl instance() {
        if (sTinyUrl == null) {
            Retrofit restAdapter = new Retrofit.Builder()
                    .baseUrl(TinyUrl.BASE_URL)
                    .client(new OkHttpClient())
                    .build();
            sTinyUrl = restAdapter.create(TinyUrl.class);
        }
        return sTinyUrl;
    }

}
