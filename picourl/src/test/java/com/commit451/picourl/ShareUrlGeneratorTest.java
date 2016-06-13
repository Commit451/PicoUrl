package com.commit451.picourl;

import android.net.Uri;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class ShareUrlGeneratorTest {

    @Test
    public void shareUrlGeneration() throws Exception {
        OkHttpClient client = new OkHttpClient();
        String originalUrl = "http://jawnnypoo.github.io/";
        Response<ResponseBody> generatedLink = PicoUrl.instance().generateLink(originalUrl).execute();
        Assert.assertTrue(generatedLink.isSuccessful());
        String tinyUrl = generatedLink.body().string();

        Request followRequest = new Request.Builder()
                .url(tinyUrl)
                .build();


        okhttp3.Response followResponse = client.newCall(followRequest).execute();

        Assert.assertEquals(originalUrl, followResponse.request().url().toString());
    }

    @Test
    public void followRedirects() throws Exception {
        ShadowLog.stream = System.out;
        OkHttpClient okHttpClient = new OkHttpClient();
        Request followRequest = new Request.Builder()
                .url("http://tinyurl.com/juw3fq6")
                .build();
        okhttp3.Response response = okHttpClient.newCall(followRequest).execute();

        String url = Util.backpedalRedirectsTillYouDie(response, "github.com");
        ShadowLog.d("HAHA", "The url was resolved! " + url);
        //We have to do this now, since really we had our urls as params
        url = Uri.decode(url);
        ShadowLog.d("HAHA", "Fixed the url to be " + url);
    }
}