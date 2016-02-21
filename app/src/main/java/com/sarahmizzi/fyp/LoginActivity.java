package com.sarahmizzi.fyp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.sarahmizzi.fyp.classes.User;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    final String TAG = LoginActivity.class.getSimpleName();
    Firebase mFirebaseRef;

    EditText username;
    EditText age;
    RadioButton femaleButton;
    RadioButton maleButton;
    EditText password;
    Button registerButton;
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFirebaseRef = new Firebase("https://sweltering-torch-8619.firebaseio.com/android/users");

        username = (EditText) findViewById(R.id.username);
        age = (EditText) findViewById(R.id.age);
        femaleButton = (RadioButton) findViewById(R.id.female);
        maleButton = (RadioButton) findViewById(R.id.male);
        password = (EditText) findViewById(R.id.passwordlogin);
        registerButton = (Button) findViewById(R.id.registerButton);
        loginButton = (Button) findViewById(R.id.loginButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
    }

    public void registerUser(){
        mFirebaseRef.createUser(username.getText().toString(), password.getText().toString(), new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                Log.d(TAG, "Successfully created user account with uid: " + result.get("uid"));
                User user;
                String ageValue = age.getText().toString();
                if(femaleButton.isChecked()){
                    user = new User(username.getText().toString(), Integer.parseInt(ageValue), "female");
                }
                else{
                    user = new User(username.getText().toString(), Integer.parseInt(ageValue), "male");
                }
                Firebase userRef = mFirebaseRef.child(result.get("uid").toString());
                if(user != null) {
                    userRef.setValue(user);
                }
                loginUser();
            }
            @Override
            public void onError(FirebaseError firebaseError) {
                // there was an error
                Log.d(TAG, "ERROR: " + firebaseError.getMessage());
            }
        });
    }

    public void loginUser(){
        mFirebaseRef.authWithPassword(username.getText().toString(), password.getText().toString(), new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());
                SharedPreferences preferences = getApplicationContext().getSharedPreferences("USER", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("UID", authData.getUid());
                editor.commit();
                Intent intent = new Intent(LoginActivity.this, ConnectHostActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                // there was an error
                Log.d(TAG, "ERROR: " + firebaseError.getMessage());
            }
        });
    }

}
