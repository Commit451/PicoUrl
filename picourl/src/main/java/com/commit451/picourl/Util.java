package com.commit451.picourl;

import android.support.annotation.Nullable;

/**
 * Utility methods
 */
class Util {

    /**
     * Keep following the previous responses until you find the desired url
     * @param response the response
     * @return the desired url, or null if not found
     */
    @Nullable
    static String backpedalRedirectsTillYouDie(okhttp3.Response response, String expectedBaseUrl) {
        while(response != null) {
            String url = response.request().url().toString();
            if (url.contains(expectedBaseUrl)) {
                return url;
            }
            response = response.priorResponse();
        }
        return null;
    }
}
