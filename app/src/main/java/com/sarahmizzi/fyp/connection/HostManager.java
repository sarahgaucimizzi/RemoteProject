package com.sarahmizzi.fyp.connection;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.sarahmizzi.fyp.kodi.jsonrpc.api.provider.MediaContract;
import com.sarahmizzi.fyp.utils.Settings;

import java.util.ArrayList;

/**
 * Created by Sarah on 01-Feb-16.
 * Manages Kodi Hosts
 * Singleton that loads the list of registered hosts, keeps a {@link HostConnection} to the active host
 * and allows for creation and removal of hosts
 */
public class HostManager {
    final String TAG = HostManager.class.getSimpleName();
    private static volatile HostManager instance = null;

    //Arraylist that will hold all the hosts in the database
    private ArrayList<HostInfo> hosts = new ArrayList<>();
    private static HostInfo currentHostInfo = null;
    private HostConnection currentHostConnection = null;
    private HostConnectionObserver currentHostConnectionObserver = null;

    Context context;

    public HostManager(Context context) {
        this.context = context;
    }

    public void setCurrentHostInfo(HostInfo currentHostInfo) {
        this.currentHostInfo = currentHostInfo;
    }

    public HostInfo addHost(HostInfo hostInfo) {
        return addHost(hostInfo.getName(), hostInfo.getAddress(),
                hostInfo.getPort(),
                hostInfo.getUsername(), hostInfo.getPassword());
    }

    public HostInfo addHost(String name, String address, int httpPort,
                            String username, String password) {

        ContentValues values = new ContentValues();
        values.put(MediaContract.HostsColumns.NAME, name);
        values.put(MediaContract.HostsColumns.ADDRESS, address);
        values.put(MediaContract.HostsColumns.HTTP_PORT, httpPort);
        values.put(MediaContract.HostsColumns.USERNAME, username);
        values.put(MediaContract.HostsColumns.PASSWORD, password);

        Uri newUri = context.getContentResolver().insert(MediaContract.Hosts.CONTENT_URI, values);
        long newId = Long.valueOf(MediaContract.Hosts.getHostId(newUri));

        // Refresh the list and return the created host
        hosts = getHosts(true);
        HostInfo newHost = null;
        for (HostInfo host : hosts) {
            if (host.getId() == newId) {
                newHost = host;
                break;
            }
        }
        return newHost;
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

    public ArrayList<HostInfo> getHosts() {
        return getHosts(false);
    }

    public ArrayList<HostInfo> getHosts(boolean forcedReload) {
        if (forcedReload || (hosts.size() == 0)) {
            hosts.clear();

            Cursor cursor = context.getContentResolver()
                    .query(MediaContract.Hosts.CONTENT_URI,
                            MediaContract.Hosts.ALL_COLUMNS,
                            null, null, null);
            if (cursor == null) return hosts;

            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    int idx = 0;
                    int id = cursor.getInt(idx++);
                    long updated = cursor.getLong(idx++);
                    String name = cursor.getString(idx++);
                    String address = cursor.getString(idx++);
                    int httpPort = cursor.getInt(idx++);
                    String username = cursor.getString(idx++);
                    String password = cursor.getString(idx++);

                    hosts.add(new HostInfo(id, name, address, httpPort,
                            username, password));
                }
            }
            cursor.close();
        }
        return hosts;
    }

    public HostInfo getHostInfo() {
        if (currentHostInfo == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int currentHostId = prefs.getInt(Settings.KEY_PREF_CURRENT_HOST_ID, Settings.DEFAULT_PREF_CURRENT_HOST_ID);

            ArrayList<HostInfo> hosts = getHosts();

            // No host selected. Check if there are hosts configured and default to the first one
            if (currentHostId == -1) {
                if (hosts.size() > 0) {
                    currentHostInfo = hosts.get(0);
                    currentHostId = currentHostInfo.getId();
                    prefs.edit()
                            .putInt(Settings.KEY_PREF_CURRENT_HOST_ID, currentHostId)
                            .apply();
                }
            } else {
                for (HostInfo host : hosts) {
                    if (host.getId() == currentHostId) {
                        currentHostInfo = host;
                        break;
                    }
                }
            }
        }
        return currentHostInfo;
    }

    public HostConnection getConnection() {
        if (currentHostConnection == null) {
            //currentHostInfo = getHostInfo();

            if (currentHostInfo != null) {
                currentHostConnection = new HostConnection(currentHostInfo);
            }
        }
        return currentHostConnection;
    }


    public HostConnectionObserver getHostConnectionObserver() {
        if (currentHostConnectionObserver == null) {
            currentHostConnection = getConnection();
            if (currentHostConnection != null) {
                currentHostConnectionObserver = new HostConnectionObserver(currentHostConnection);
            }
        }
        return currentHostConnectionObserver;
    }
}
