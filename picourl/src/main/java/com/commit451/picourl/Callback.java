package com.commit451.picourl;

/**
 * Callback!
 */
public interface Callback<T> {

    void onResponse(T response);

    void onFailure(Throwable t);
}
