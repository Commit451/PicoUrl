package com.commit451.picourl;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;

/**
 * Shorten your urls, powered by http://tinyurl.com/. Use the {@link Builder} to create an instance
 */
public class PicoUrl {

    /**
     * Build your {@link PicoUrl} instance
     */
    public static class Builder {
        String baseUrl;
        String param;
        OkHttpClient client;

        public Builder() {
        }

        /**
         * Set the base url to use.
         * @param baseUrl the base url
         * @return builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Set the {@link OkHttpClient} PicoUrl will use. Make sure to include any configuration
         * or authentication you might need
         * @param client the client
         * @return builder
         */
        public Builder client(OkHttpClient client) {
            this.client = client;
            return this;
        }

        /**
         * Set the query param that will show up in the shortened url. By default, "tinyUrl"
         * @param param the key that will be used for the shortened url
         * @return builder
         */
        public Builder tinyQueryParam(String param) {
            this.param = param;
            return this;
        }

        /**
         * Build the {@link PicoUrl} instance
         * @return the built instance
         */
        public PicoUrl build() {
            if (baseUrl == null) {
                throw new IllegalStateException("You need to specify a base url");
            }
            if (client == null) {
                client = new OkHttpClient();
            }
            if (param == null) {
                param = DEFAULT_QUERY_PARAM_TINY_URL;
            }
            return new PicoUrl(baseUrl, param, client);
        }
    }

    private static final String DEFAULT_QUERY_PARAM_TINY_URL = "tinyUrl";

    private TinyUrl tinyUrl;
    private String baseUrl;
    private String param;
    private OkHttpClient client;

    private PicoUrl(String baseUrl, String param, OkHttpClient client) {
        this.baseUrl = baseUrl;
        this.client = client;
        this.param = param;
        tinyUrl = TinyUrlFactory.create(client);
    }

    /**
     * Generate a shortened url, eg. http://yourbaseurl.com?tinyUrl=asdfhds
     * @param url the url to shorten.
     * @return an observable with the shorted url
     */
    public Observable<Uri> generate(@NonNull final Uri url) {
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
    public Observable<Uri> parse(@NonNull final Uri url) {
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
        Response<ResponseBody> tinyUrlResponse = tinyUrl.generateLink(url.toString()).execute();
        String tinyUrl = tinyUrlResponse.body().string();
        //Get the unique string at the end of the tinyurl
        String tinyUrlPath = tinyUrl.split(".com/")[1];
        //Builds our url like so:
        //http://yourdomain.com&tinyurl=asdfwe
        return Uri.parse(baseUrl).buildUpon()
                .appendQueryParameter(param, tinyUrlPath)
                .build();
    }

    private Uri parseInternal(Uri url) throws Exception {
        String tinyUrlPath = url.getQueryParameter(param);
        if (tinyUrlPath == null) {
            throw new IllegalArgumentException("Passed url does not contain a tiny url param");
        }
        final String followUrl = TinyUrl.BASE_URL + tinyUrlPath;
        Request followRequest = new Request.Builder()
                .url(followUrl)
                .build();
        okhttp3.Response response = client.newCall(followRequest).execute();

        String followedUrl = Util.backpedalRedirectsTillYouDie(response, baseUrl);
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
