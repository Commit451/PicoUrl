package com.commit451.picourl;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;

/**
 * Blah
 */
public class PicoUrl {

    private static final String QUERY_PARAM_TINY_URL = "tinyUrl";

    public static PicoUrl create(String baseUrl) {
        return create(baseUrl, null);
    }
    public static PicoUrl create(String baseUrl, @Nullable OkHttpClient.Builder okhttpBuilder) {
        if (okhttpBuilder == null) {
            okhttpBuilder = new OkHttpClient.Builder();
        }
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
    public Observable<Uri> generate(final Uri url) {
        return Observable.create(new Observable.OnSubscribe<Uri>() {
            @Override
            public void call(Subscriber<? super Uri> subscriber) {
                try {
                    Uri generatedUrl = generateInternal(url);
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
     * @param url the url that was shorted by the {@link #generate(Uri)} method
     * @return the original url that was passed to the {@link #generate(Uri)} method
     */
    public Observable<Uri> parse(final Uri url) {
        return Observable.create(new Observable.OnSubscribe<Uri>() {
            @Override
            public void call(Subscriber<? super Uri> subscriber) {
                try {
                    Uri parsedUrl = parseInternal(url);
                    subscriber.onNext(parsedUrl);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    private Uri generateInternal(final Uri url) throws IOException {
        Response<ResponseBody> tinyUrlResponse = mTinyUrl.generateLink(url.toString()).execute();
        String tinyUrl = tinyUrlResponse.body().string();
        //Get the unique string at the end of the tinyurl
        String tinyUrlPath = tinyUrl.split(".com/")[1];
        //Builds our url like so:
        //http://yourdomain.com&tinyurl=asdfwe
        return Uri.parse(mBaseUrl).buildUpon()
                .appendQueryParameter(QUERY_PARAM_TINY_URL, tinyUrlPath)
                .build();
    }

    private Uri parseInternal(Uri url) throws Exception {
        String tinyUrlPath = url.getQueryParameter(QUERY_PARAM_TINY_URL);
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
        return Uri.parse(parsedUrl);
    }
}
