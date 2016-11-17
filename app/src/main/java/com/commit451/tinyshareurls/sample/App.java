package com.commit451.tinyshareurls.sample;

import android.app.Application;

import com.commit451.picourl.PicoUrl;

/**
 * Singleton app
 */
public class App extends Application {

    private static App sInstance;

    public static App instance() {
        return sInstance;
    }

    private PicoUrl mPicoUrl;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mPicoUrl = new PicoUrl.Builder()
                .baseUrl("http://" + getString(R.string.share_base_url))
                .build();
    }

    /**
     * Get a shared instance of {@link PicoUrl}. Feel free to use other ways to share instances,
     * such as using Dagger
     * @return the shared instance
     */
    public PicoUrl getPicoUrl() {
        return mPicoUrl;
    }
}
