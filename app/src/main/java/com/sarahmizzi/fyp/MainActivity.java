package com.sarahmizzi.fyp;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import com.firebase.client.Firebase;
import com.sarahmizzi.fyp.classes.Transaction;
import com.sarahmizzi.fyp.connection.HostManager;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiCallback;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiMethod;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.GUI;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.Input;
import com.sarahmizzi.fyp.utils.RepeatListener;
import com.sarahmizzi.fyp.utils.UIUtils;

import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    final String TAG = MainActivity.class.getSimpleName();
    private HostManager hostManager = null;
    private Handler callbackHandler = new Handler();
    private View.OnTouchListener feedbackTouchListener;

    private Animation buttonInAnim;
    private Animation buttonOutAnim;

    Firebase mFirebaseRef;
    String uID;

    ImageButton upButton;
    ImageButton downButton;
    ImageButton leftButton;
    ImageButton rightButton;
    ImageButton okButton;
    ImageButton homeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mFirebaseRef = new Firebase("https://sweltering-torch-8619.firebaseio.com/android/transactions");
        uID = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("UID", "error");

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

        okButton = (ImageButton) findViewById(R.id.ok_button);
        homeButton = (ImageButton) findViewById(R.id.home_button);

        setupRepeatButton(upButton, new Input.Up(), "UP");
        setupRepeatButton(downButton, new Input.Down(), "DOWN");
        setupRepeatButton(leftButton, new Input.Left(), "LEFT");
        setupRepeatButton(rightButton, new Input.Right(), "RIGHT");

        setupDefaultButton(okButton, new Input.Select(), null);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Transaction transaction = new Transaction(uID, "HOME");
                Firebase transactionRef = mFirebaseRef.child(new UUID((long)10, (long)10).toString());
                transactionRef.setValue(transaction);
                GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.HOME);
                action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

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
                        Transaction transaction = new Transaction(uID, description);
                        Firebase transactionRef = mFirebaseRef.child(new UUID((long)10, (long)10).toString());
                        transactionRef.setValue(transaction);
                        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                    }
                }, buttonInAnim, buttonOutAnim, MainActivity.this.getBaseContext()));
    }

    private void setupDefaultButton(View button,
                                    final ApiMethod<String> clickAction,
                                    final ApiMethod<String> longClickAction) {
        // Set animation
        button.setOnTouchListener(feedbackTouchListener);
        if (clickAction != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Transaction transaction = new Transaction(uID, "OK");
                    Firebase transactionRef = mFirebaseRef.child(new UUID((long)10, (long)10).toString());
                    transactionRef.setValue(transaction);
                    UIUtils.handleVibration(MainActivity.this);
                    clickAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                }
            });
        }
        if (longClickAction != null) {
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Transaction transaction = new Transaction(uID, "OK");
                    Firebase transactionRef = mFirebaseRef.child(new UUID((long)10, (long)10).toString());
                    transactionRef.setValue(transaction);
                    UIUtils.handleVibration(MainActivity.this);
                    longClickAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                    return true;
                }
            });
        }
    }

    private ApiCallback<String> defaultActionCallback = ApiMethod.getDefaultActionCallback();
}
