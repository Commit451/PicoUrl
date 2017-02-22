package com.commit451.picourl;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Shorten your urls, powered by http://tinyurl.com/. Use the {@link Builder}
 * to create an instance.
 */
public class PicoUrl {

    /**
     * Build your {@link PicoUrl} instance
     */
    public static class Builder {
        private String baseUrl;
        private String param;
        private OkHttpClient client;

        /**
         * Create a new Builder
         */
        public Builder() {
        }

        /**
         * Set the base url to use.
         * @param baseUrl the base url
         * @return builder
         */
        public Builder baseUrl(@NonNull String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Set the {@link OkHttpClient} PicoUrl will use. Make sure to include any configuration
         * or authentication you might need
         * @param client the client
         * @return builder
         */
        public Builder client(@Nullable OkHttpClient client) {
            this.client = client;
            return this;
        }

        /**
         * Set the query param that will show up in the shortened url. By default, "tinyUrl"
         * @param param the key that will be used for the shortened url
         * @return builder
         */
        public Builder tinyQueryParam(@Nullable String param) {
            this.param = param;
            return this;
        }

        /**
         * Build the {@link PicoUrl} instance
         * @return the built instance
         */
        @NonNull
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
    public Single<Uri> generate(@NonNull final Uri url) {
        return Single.defer(new Callable<SingleSource<? extends Uri>>() {
            @Override
            public SingleSource<? extends Uri> call() throws Exception {
                Uri generatedUrl = generateInternal(url);
                return Single.just(generatedUrl);
            }
        });
    }

    /**
     * Parse a shorted url
     * @param url the url that was shorted by the {@link #generate(Uri)} method
     * @return the original url that was passed to the {@link #generate(Uri)} method
     */
    public Single<Uri> parse(@NonNull final Uri url) {
        return Single.defer(new Callable<SingleSource<? extends Uri>>() {
            @Override
            public SingleSource<? extends Uri> call() throws Exception {
                return Single.just(parseInternal(url));
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
            throw new RedirectNotFoundException();
        }
        //We have to do this now, since really we had our urls as params
        final String parsedUrl = Uri.decode(followedUrl);
        response.body().close();
        return Uri.parse(parsedUrl);
    }
}
