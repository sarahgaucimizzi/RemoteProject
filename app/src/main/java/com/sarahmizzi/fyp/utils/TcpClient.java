package com.sarahmizzi.fyp.utils;

import android.util.Log;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;

/**
 * Created by Sarah on 27-Feb-16.
 * Start client and connect to server on port 81. Add listener for any message responses from server.
 */
public class TcpClient {
    final String TAG = TcpClient.class.getSimpleName();
    final Client client;

    public TcpClient(String address) {
        client = new Client();
        Kryo kryo = client.getKryo();
        kryo.register(TcpRequest.class);
        kryo.register(TcpResponse.class);

        client.start();
        try {
            client.connect(5000, address, 81);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof TcpResponse) {
                    TcpResponse response = (TcpResponse) object;
                    System.out.println(response.message);
                }
            }
        });
    }

    public Client getClient() {
        return client;
    }
}
