package com.commit451.picourl;

import android.support.annotation.Nullable;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Creates a usable {@link TinyUrl} instance
 */
class TinyUrlFactory {

    static TinyUrl create(@Nullable OkHttpClient okHttpClient) {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient();
        }

        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl(TinyUrl.BASE_URL)
                .client(okHttpClient)
                .build();
        return restAdapter.create(TinyUrl.class);
    }
}
