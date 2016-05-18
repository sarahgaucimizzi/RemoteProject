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

        // Check if last connected credentials work
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("HOST", MODE_PRIVATE);
        if (preferences.contains("ADDRESS")) {
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

    /*
     * Start connection to Kodi using credentials entered by the user over HTTP
     */
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

        // If username or password empty, set it to null
        if (TextUtils.isEmpty(kodiUsername))
            kodiUsername = null;
        if (TextUtils.isEmpty(kodiPassword))
            kodiPassword = null;

        // Ping host
        final HostInfo checkedHostInfo = new HostInfo(-1, kodiName, kodiAddress,
                kodiHttpPort, kodiUsername, kodiPassword);

        chainCallCheckHttpConnection(checkedHostInfo);
    }

    private void chainCallCheckHttpConnection(final HostInfo hostInfo) {
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
                Intent intent = new Intent(ConnectHostActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(int errorCode, String description) {
                hostConnectionError(errorCode, description);
            }
        }, handler);
    }

    private void hostConnectionError(int errorCode, String description) {
        Log.d(TAG, "An error occurred during connection test int. Message: " + description);
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
