package com.sarahmizzi.fyp.connection;

import android.content.Context;

/**
 * Created by Sarah on 01-Feb-16.
 * Manages Kodi Hosts
 * Singleton that loads the list of registered hosts, keeps a {@link HostConnection} to the active host
 * and allows for creation and removal of hosts
 */
public class HostManager {
    final String TAG = HostManager.class.getSimpleName();
    private static volatile HostManager instance = null;

    Context context;

    protected HostManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static HostManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HostManager.class) {
                if (instance == null) {
                    instance = new HostManager(context);
                }
            }
        }
        return instance;
    }
}
