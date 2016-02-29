package com.sarahmizzi.fyp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.firebase.client.Firebase;
import com.sarahmizzi.fyp.classes.Transaction;
import com.sarahmizzi.fyp.connection.HostManager;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiCallback;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiMethod;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.GUI;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.Input;
import com.sarahmizzi.fyp.utils.RepeatListener;
import com.sarahmizzi.fyp.utils.TcpRequest;
import com.sarahmizzi.fyp.utils.TcpClient;
import com.sarahmizzi.fyp.utils.UIUtils;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    final String TAG = MainActivity.class.getSimpleName();

    private HostManager hostManager = null;
    private Handler callbackHandler = new Handler();
    private View.OnTouchListener feedbackTouchListener;

    private Animation buttonInAnim;
    private Animation buttonOutAnim;

    SharedPreferences mUserPreferences;
    SharedPreferences mHostPreferences;
    TcpClient tcpClient;
    Firebase mFirebaseRef;
    String uID;

    ImageButton upButton;
    ImageButton downButton;
    ImageButton leftButton;
    ImageButton rightButton;
    ImageButton backButton;
    ImageButton contextButton;
    ImageButton okButton;
    ImageButton homeButton;
    ImageButton infoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        TextView email = (TextView) navigationView.getHeaderView(0).findViewById(R.id.emailTextView);

        mHostPreferences = getApplicationContext().getSharedPreferences("HOST", MODE_PRIVATE);
        tcpClient = new TcpClient(mHostPreferences.getString("ADDRESS", "error"));

        mFirebaseRef = new Firebase("https://sweltering-torch-8619.firebaseio.com/android/transactions");
        mUserPreferences = getApplicationContext().getSharedPreferences("USER", MODE_PRIVATE);
        uID = mUserPreferences.getString("UID", "error");
        email.setText(mUserPreferences.getString("EMAIL", ""));

        hostManager = new HostManager(getApplicationContext());
        feedbackTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonInAnim.setFillAfter(true);
                        v.startAnimation(buttonInAnim);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.startAnimation(buttonOutAnim);
                        break;
                }
                return false;
            }
        };

        buttonInAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_in);
        buttonOutAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_out);

        upButton = (ImageButton) findViewById(R.id.up_arrow_button);
        downButton = (ImageButton) findViewById(R.id.down_arrow_button);
        leftButton = (ImageButton) findViewById(R.id.left_arrow_button);
        rightButton = (ImageButton) findViewById(R.id.right_arrow_button);

        backButton = (ImageButton) findViewById(R.id.back_button);
        contextButton = (ImageButton) findViewById(R.id.context_button);
        okButton = (ImageButton) findViewById(R.id.ok_button);
        homeButton = (ImageButton) findViewById(R.id.home_button);
        infoButton = (ImageButton) findViewById(R.id.info_button);

        setupRepeatButton(upButton, new Input.Up(), "UP");
        setupRepeatButton(downButton, new Input.Down(), "DOWN");
        setupRepeatButton(leftButton, new Input.Left(), "LEFT");
        setupRepeatButton(rightButton, new Input.Right(), "RIGHT");

        setupDefaultButton(backButton, new Input.Back(), null, "BACK");
        setupDefaultButton(contextButton, new Input.ExecuteAction(Input.ExecuteAction.CONTEXTMENU), null, "CONTEXT");
        setupDefaultButton(okButton, new Input.Select(), null, "OK");

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Transaction transaction = new Transaction("HOME");
                Firebase transactionRef = mFirebaseRef.child(uID);
                transactionRef.push().setValue(transaction);
                GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.HOME);
                action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
            }
        });

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Transaction transaction = new Transaction("INFO");
                Firebase transactionRef = mFirebaseRef.child(uID);
                transactionRef.push().setValue(transaction);
                GUI.ShowNotification notification = new GUI.ShowNotification("Hello", "Woo it works!");
                notification.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);*/

                TcpRequest request = new TcpRequest();
                request.methodName = "CONNECT";
                tcpClient.getClient().sendTCP(request);
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_reconnect) {
            mHostPreferences.edit().clear().commit();
            Intent intent = new Intent(MainActivity.this, ConnectHostActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_manage) {
            // Show dialog on TV or mobile
        } else if (id == R.id.nav_logout) {
            mFirebaseRef.unauth();
            mUserPreferences.edit().clear().commit();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupRepeatButton(View button, final ApiMethod<String> action, final String description) {
        button.setOnTouchListener(new RepeatListener(UIUtils.initialButtonRepeatInterval, UIUtils.buttonRepeatInterval,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Transaction transaction = new Transaction(description);
                        Firebase transactionRef = mFirebaseRef.child(uID);
                        transactionRef.push().setValue(transaction);
                        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                    }
                }, buttonInAnim, buttonOutAnim, MainActivity.this.getBaseContext()));
    }

    private void setupDefaultButton(View button,
                                    final ApiMethod<String> clickAction,
                                    final ApiMethod<String> longClickAction,
                                    final String description) {
        // Set animation
        button.setOnTouchListener(feedbackTouchListener);
        if (clickAction != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Transaction transaction = new Transaction(description);
                    Firebase transactionRef = mFirebaseRef.child(uID);
                    transactionRef.push().setValue(transaction);
                    UIUtils.handleVibration(MainActivity.this);
                    clickAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                }
            });
        }
        if (longClickAction != null) {
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Transaction transaction = new Transaction(description);
                    Firebase transactionRef = mFirebaseRef.child(uID);
                    transactionRef.push().setValue(transaction);
                    UIUtils.handleVibration(MainActivity.this);
                    longClickAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                    return true;
                }
            });
        }
    }

    private ApiCallback<String> defaultActionCallback = ApiMethod.getDefaultActionCallback();
}
