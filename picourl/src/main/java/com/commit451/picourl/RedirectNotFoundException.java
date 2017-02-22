package com.commit451.picourl;

/**
 * The redirect was never found
 */
public class RedirectNotFoundException extends Exception {

    @Override
    public String getMessage() {
        return "The redirect was never found";
    }
}
