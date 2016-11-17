package com.commit451.picourl;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import rx.observers.TestSubscriber;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class ShareUrlGeneratorTest {

    @Test
    public void shareUrlGeneration() throws Exception {
        String baseUrl = "http://jawnnypoo.github.io";
        PicoUrl picoUrl = PicoUrl.create(baseUrl, new OkHttpClient.Builder());

        Uri shareUrl = Uri.parse("http://jawnnypoo.github.io/?arg1=hi&arg2=there");
        final TestSubscriber<Uri> subscriber = new TestSubscriber<>();
        picoUrl.generate(shareUrl).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertCompleted();
        //Predetermined to be the generation tinyurl will do
        String shareUrlEnd = "gtffxxo";
        subscriber.assertValue(Uri.parse(baseUrl + "?tinyUrl=" + shareUrlEnd));
        //The parsed url should == the generated url in the end, otherwise this did not do its job
        Uri generatedUrl = subscriber.getOnNextEvents().get(0);
        final TestSubscriber<Uri> parseSubscriber = new TestSubscriber<>();
        picoUrl.parse(generatedUrl).subscribe(parseSubscriber);
        parseSubscriber.awaitTerminalEvent();
        parseSubscriber.assertCompleted();
        parseSubscriber.assertValue(shareUrl);
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