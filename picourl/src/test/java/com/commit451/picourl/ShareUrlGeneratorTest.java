package com.commit451.picourl;

import android.net.Uri;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class ShareUrlGeneratorTest {

    @Test
    public void shareUrlGeneration() throws Exception {
        String baseUrl = "http://jawnnypoo.github.io";
        PicoUrl picoUrl = new PicoUrl.Builder()
                .baseUrl(baseUrl)
                .build();

        Uri shareUrl = Uri.parse("http://jawnnypoo.github.io/?arg1=hi&arg2=there");
        Uri testUri = picoUrl.generate(shareUrl).blockingGet();
        //Predetermined to be the generation tinyurl will do
        String shareUrlEnd = "gtffxxo";
        Uri actualUri = Uri.parse(baseUrl + "?tinyUrl=" + shareUrlEnd);
        Assert.assertEquals(actualUri, testUri);
        //The parsed url should == the generated url in the end, otherwise this did not do its job
        Uri parsedUri = picoUrl.parse(testUri).blockingGet();
        Assert.assertEquals(shareUrl, parsedUri);
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