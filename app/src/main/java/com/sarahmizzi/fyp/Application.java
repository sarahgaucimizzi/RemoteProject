package com.sarahmizzi.fyp;

import com.firebase.client.Firebase;

/**
 * Created by Sarah on 12-Feb-16.
 */
public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialise Firebase
        Firebase.setAndroidContext(this);
    }
}
