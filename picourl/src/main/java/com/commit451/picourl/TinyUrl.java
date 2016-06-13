package com.commit451.picourl;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Url so tiny!
 */
public interface TinyUrl {

    String BASE_URL = "http://tinyurl.com/";

    @GET("api-create.php")
    Call<ResponseBody> generateLink(@Query("url") String url);
}
