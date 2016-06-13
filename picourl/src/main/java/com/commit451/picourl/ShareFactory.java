package com.commit451.picourl;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Generates share links using tinyurl, then changes the url back to ours so that we
 * can intercept the deep links, then change them back to tiny urls, then follow them
 */
public class ShareFactory {

    private String mBaseUrl;
    private OkHttpClient mOkHttpClient;

    public ShareFactory(String baseUrl, OkHttpClient.Builder okhttpBuilder) {
        mBaseUrl = baseUrl;
        mOkHttpClient = okhttpBuilder.build();
    }

    public void generateShareUrl(Context context, String data, final Callback<String> urlCallback) {
        PicoUrl.instance().generateLink(data).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                //Good, we got the tinyurl. Now we need to make it one of ours
                if (!response.isSuccessful()) {
                    urlCallback.onFailure(new Exception("Not successful"));
                    return;
                }
                try {
                    String tinyUrl = response.body().string();
                    String tinyUrlPath = tinyUrl.split(".com/")[1];
                    String ourUrl = Uri.parse(mBaseUrl).buildUpon()
                            .appendQueryParameter("tinyUrl", tinyUrlPath)
                            .toString();
                    urlCallback.onResponse(ourUrl);
                } catch (IOException e) {
                    urlCallback.onFailure(e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                urlCallback.onFailure(t);
            }
        });
    }

    public void parseShareUrl(String url, final Callback<String> callback) {
        String tinyUrlPath = Uri.parse(url).getQueryParameter("tinyUrl");
        final String followUrl = "http://tinyurl.com/" + tinyUrlPath;
        Request followRequest = new Request.Builder()
                .url(followUrl)
                .build();
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        //yuck
        mOkHttpClient.newCall(followRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, final IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(new Exception("Not successful"));
                        }
                    });
                    return;
                }
                String followedUrl = Util.backpedalRedirectsTillYouDie(response, mBaseUrl);
                if (followedUrl == null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(new Exception("yeah... no redirect worked"));
                        }
                    });
                    return;
                }
                //We have to do this now, since really we had our urls as params
                final String finalUrl = Uri.decode(followedUrl);
                response.body().close();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(finalUrl);
                    }
                });
            }
        });
    }

}
