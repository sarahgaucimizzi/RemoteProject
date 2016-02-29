package com.sarahmizzi.fyp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.sarahmizzi.fyp.connection.HostConnection;
import com.sarahmizzi.fyp.connection.HostInfo;
import com.sarahmizzi.fyp.connection.HostManager;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiCallback;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiException;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.JSONRPC;

public class ConnectHostActivity extends AppCompatActivity {
    final String TAG = ConnectHostActivity.class.getSimpleName();
    final Handler handler = new Handler();
    EditText mNameEditText;
    EditText mAddressEditText;
    EditText mPortEditText;
    EditText mUsernameEditText;
    EditText mPasswordEditText;
    Button mConnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Firebase ref = new Firebase("https://sweltering-torch-8619.firebaseio.com");

        // If logged in connect
        AuthData authData = ref.getAuth();
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("HOST", MODE_PRIVATE);
        if(authData != null && preferences.contains("ADDRESS")){
            HostInfo hostInfo = new HostInfo(-1, preferences.getString("NAME", "TV"), preferences.getString("ADDRESS", "error"),
                    preferences.getInt("PORT", 8080), preferences.getString("USERNAME", "kodi"), preferences.getString("PASSWORD", "kodi"));

            chainCallCheckHttpConnection(hostInfo);
        }

        setContentView(R.layout.activity_connect_host);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNameEditText = (EditText) findViewById(R.id.name);
        mAddressEditText = (EditText) findViewById(R.id.address);
        mPortEditText = (EditText) findViewById(R.id.port);
        mUsernameEditText = (EditText) findViewById(R.id.username);
        mPasswordEditText = (EditText) findViewById(R.id.password);
        mConnectButton = (Button) findViewById(R.id.connectbutton);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToHost();
            }
        });
    }

    public void connectToHost() {
        String kodiName = mNameEditText.getText().toString();
        String kodiAddress = mAddressEditText.getText().toString();
        String kodiUsername = mUsernameEditText.getText().toString();
        String kodiPassword = mPasswordEditText.getText().toString();

        int kodiHttpPort;
        String temp = mPortEditText.getText().toString();
        try {
            kodiHttpPort = TextUtils.isEmpty(temp) ? HostInfo.DEFAULT_HTTP_PORT : Integer.valueOf(temp);
        } catch (NumberFormatException exc) {
            kodiHttpPort = -1;
        }

        /*
        temp = xbmcTcpPortEditText.getText().toString();
        int xbmcTcpPort;
        try {
            xbmcTcpPort = TextUtils.isEmpty(aux) ? HostInfo.DEFAULT_TCP_PORT : Integer.valueOf(aux);
        } catch (NumberFormatException exc) {
            xbmcTcpPort = -1;
        }
        */

        int kodiProtocol = HostConnection.PROTOCOL_HTTP;

        /*
        String macAddress = xbmcMacAddressEditText.getText().toString();
        aux = xbmcWolPortEditText.getText().toString();
        */

        int kodiWolPort = HostInfo.DEFAULT_WOL_PORT;

        /*
        try {
            xbmcWolPort = TextUtils.isEmpty(aux) ? HostInfo.DEFAULT_WOL_PORT : Integer.valueOf(aux);
        } catch (NumberFormatException exc) {
            // Ignoring this exception and keeping WoL port at the default value
        }
        */

        /*
        boolean xbmcUseEventServer = xbmcUseEventServerCheckbox.isChecked();
        aux = xbmcEventServerPortEditText.getText().toString();
        int xbmcEventServerPort;
        try {
            xbmcEventServerPort = TextUtils.isEmpty(aux) ? HostInfo.DEFAULT_EVENT_SERVER_PORT : Integer.valueOf(aux);
        } catch (NumberFormatException exc) {
            xbmcEventServerPort = -1;
        }
        */

        int kodiEventServerPort = HostInfo.DEFAULT_EVENT_SERVER_PORT;

        // Check name and address and port number
        if (TextUtils.isEmpty(kodiName)) {
            Toast.makeText(ConnectHostActivity.this, "Enter a name", Toast.LENGTH_SHORT).show();
            mNameEditText.requestFocus();
            return;
        } else if (TextUtils.isEmpty(kodiAddress)) {
            Toast.makeText(ConnectHostActivity.this, "Enter an address", Toast.LENGTH_SHORT).show();
            mAddressEditText.requestFocus();
            return;
        } else if (kodiHttpPort <= 0) {
            Toast.makeText(ConnectHostActivity.this, "Enter port number", Toast.LENGTH_SHORT).show();
            mPortEditText.requestFocus();
            return;
        }
        /*
        else if (xbmcTcpPort <= 0) {
            Toast.makeText(getActivity(), R.string.wizard_invalid_tcp_port_specified, Toast.LENGTH_SHORT).show();
            xbmcTcpPortEditText.requestFocus();
            return;
        } else if (xbmcEventServerPort <= 0) {
            Toast.makeText(getActivity(), R.string.wizard_invalid_tcp_port_specified, Toast.LENGTH_SHORT).show();
            xbmcEventServerPortEditText.requestFocus();
            return;
        }
        */

        // If username or password empty, set it to null
        if (TextUtils.isEmpty(kodiUsername))
            kodiUsername = null;
        if (TextUtils.isEmpty(kodiPassword))
            kodiPassword = null;

        // Ok, let's try to ping the host
        final HostInfo checkedHostInfo = new HostInfo(-1, kodiName, kodiAddress,
                kodiHttpPort, kodiUsername, kodiPassword);
        //checkedHostInfo.setMacAddress(macAddress);
        //checkedHostInfo.setWolPort(xbmcWolPort);

        /*
        progressDialog.setTitle(String.format(getResources().getString(R.string.wizard_connecting_to_xbmc_title), xbmcName));
        progressDialog.setMessage(getResources().getString(R.string.wizard_connecting_to_xbmc_message));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // Let's ping the host through HTTP
                chainCallCheckHttpConnection(checkedHostInfo);
            }
        });
        progressDialog.show();
        */

        chainCallCheckHttpConnection(checkedHostInfo);
    }

    private void chainCallCheckHttpConnection(final HostInfo hostInfo) {
        // Let's ping the host through HTTP
        final HostConnection hostConnection = new HostConnection(hostInfo);
        final JSONRPC.Ping httpPing = new JSONRPC.Ping();
        httpPing.execute(hostConnection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Successfully connected to new host through HTTP.");
                HostManager hostManager = new HostManager(ConnectHostActivity.this.getApplicationContext());
                hostManager.setCurrentHostInfo(hostInfo);
                SharedPreferences preferences = getApplicationContext().getSharedPreferences("HOST", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("NAME", hostInfo.getName());
                editor.putString("ADDRESS", hostInfo.getAddress());
                editor.putInt("PORT", hostInfo.getPort());
                editor.putString("USERNAME", hostInfo.getUsername());
                editor.putString("PASSWORD", hostInfo.getPassword());
                editor.commit();
                Intent intent = new Intent(ConnectHostActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(int errorCode, String description) {
                // Couldn't connect through HTTP, abort, and initialize checkedHostInfo
                hostConnectionError(errorCode, description);
            }
        }, handler);
    }

    private void hostConnectionError(int errorCode, String description) {
        //if (!isAdded()) return;

        Log.d(TAG, "An error occurred during connection testint. Message: " + description);
        switch (errorCode) {
            case ApiException.HTTP_RESPONSE_CODE_UNAUTHORIZED:
                String username = mUsernameEditText.getText().toString(),
                        password = mPasswordEditText.getText().toString();
                String messageResourceId;
                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                    messageResourceId = "Enter credentials";
                } else {
                    messageResourceId = "Enter correct credentials";
                }
                Toast.makeText(ConnectHostActivity.this, messageResourceId, Toast.LENGTH_SHORT).show();
                mUsernameEditText.requestFocus();
                break;
            default:
                Toast.makeText(ConnectHostActivity.this,
                        "Connection error",
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
