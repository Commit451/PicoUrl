package com.commit451.picourl;

import android.net.Uri;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;

/**
 * Blah
 */
public class PicoUrl {

    private static final String QUERY_PARAM_TINY_URL = "tinyUrl";

    public static PicoUrl create(String baseUrl, OkHttpClient.Builder okhttpBuilder) {
        OkHttpClient client = okhttpBuilder.build();
        PicoUrl picoUrl = new PicoUrl();
        picoUrl.mBaseUrl = baseUrl;
        //Should these share the same okhttp client?
        picoUrl.mTinyUrl = TinyUrlFactory.create(client);
        picoUrl.mOkHttpClient = client;
        return picoUrl;
    }

    private TinyUrl mTinyUrl;
    private String mBaseUrl;
    private OkHttpClient mOkHttpClient;

    private PicoUrl() {

    }

    /**
     * Generate a shortened url, eg. http://yourbaseurl.com?tinyUrl=asdfhds
     * @param url the url to shorten.
     * @return an observable with the shorted url
     */
    public Observable<String> generate(final String url) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try {
                    String generatedUrl = generateInternal(url);
                    subscriber.onNext(generatedUrl);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    /**
     * Parse a shorted url
     * @param url the url that was shorted by the {@link #generate(String)} method
     * @return the original url that was passed to the {@link #generate(String)} method
     */
    public Observable<String> parse(final String url) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try {
                    String parsedUrl = parseInternal(url);
                    subscriber.onNext(parsedUrl);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    private String generateInternal(final String url) throws IOException {
        Response<String> tinyUrlResponse = mTinyUrl.generateLink(url).execute();
        String tinyUrl = tinyUrlResponse.body();
        //Get the unique string at the end of the tinyurl
        String tinyUrlPath = tinyUrl.split(".com/")[1];
        //Builds our url like so:
        //http://yourdomain.com&tinyurl=asdfwe
        return Uri.parse(mBaseUrl).buildUpon()
                .appendQueryParameter(QUERY_PARAM_TINY_URL, tinyUrlPath)
                .toString();
    }

    private String parseInternal(String url) throws Exception {
        String tinyUrlPath = Uri.parse(url).getQueryParameter(QUERY_PARAM_TINY_URL);
        if (tinyUrlPath == null) {
            throw new IllegalArgumentException("Passed url does not contain a tiny url param");
        }
        final String followUrl = TinyUrl.BASE_URL + tinyUrlPath;
        Request followRequest = new Request.Builder()
                .url(followUrl)
                .build();
        okhttp3.Response response = mOkHttpClient.newCall(followRequest).execute();

        String followedUrl = Util.backpedalRedirectsTillYouDie(response, mBaseUrl);
        if (followedUrl == null) {
            response.body().close();
            //Throw some more meaningful error
            throw new Exception("The url passed does not ever match your baseUrl");
        }
        //We have to do this now, since really we had our urls as params
        final String parsedUrl = Uri.decode(followedUrl);
        response.body().close();
        return parsedUrl;
    }
}
