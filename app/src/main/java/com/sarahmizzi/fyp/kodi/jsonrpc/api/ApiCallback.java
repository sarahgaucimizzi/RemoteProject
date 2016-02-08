package com.sarahmizzi.fyp.kodi.jsonrpc.api;

/**
 * Created by Sarah on 05-Feb-16.
 */

public interface ApiCallback<T> {
    public abstract void onSuccess(T result);
    public abstract void onError(int errorCode, String description);
}

