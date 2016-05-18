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
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sarahmizzi.fyp.classes.LayoutLog;
import com.sarahmizzi.fyp.classes.RemoteButtonData;
import com.sarahmizzi.fyp.classes.ButtonTransaction;
import com.sarahmizzi.fyp.classes.Video;
import com.sarahmizzi.fyp.connection.HostConnectionObserver;
import com.sarahmizzi.fyp.connection.HostInfo;
import com.sarahmizzi.fyp.connection.HostManager;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiCallback;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiMethod;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.GUI;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.GlobalType;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.Input;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ListType;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.Player;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.PlayerType;
import com.sarahmizzi.fyp.utils.RepeatListener;
import com.sarahmizzi.fyp.utils.TcpRequest;
import com.sarahmizzi.fyp.utils.TcpClient;
import com.sarahmizzi.fyp.utils.TcpResponse;
import com.sarahmizzi.fyp.utils.UIUtils;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, HostConnectionObserver.PlayerEventsObserver {
    final String TAG = MainActivity.class.getSimpleName();

    private HostManager hostManager = null;
    private HostConnectionObserver hostConnectionObserver;
    private Handler callbackHandler = new Handler();
    private View.OnTouchListener feedbackTouchListener;

    private ApiCallback<String> defaultActionCallback = ApiMethod.getDefaultActionCallback();
    private ApiCallback<Integer> defaultIntActionCallback = ApiMethod.getDefaultActionCallback();

    private int currentActivePlayerId = -1;
    private String currentNowPlayingItemType = null;
    private boolean isPlaying = false;
    private boolean isRewindorFastForward = false;

    private Animation buttonInAnim;
    private Animation buttonOutAnim;

    SharedPreferences mUserPreferences;
    SharedPreferences mHostPreferences;
    TcpClient tcpClient;
    Firebase mFirebaseButtonRef;
    Firebase mFirebaseLayoutRef;
    String uID;

    LinearLayout defaultUI;
    RelativeLayout adaptiveUI;

    ImageButton rewindButton;
    ImageButton fastForwardButton;
    ImageButton stopButton;
    ImageButton playPauseButton;
    ImageButton volumeUpButton;
    ImageButton volumeDownButton;
    ImageButton upButton;
    ImageButton downButton;
    ImageButton leftButton;
    ImageButton rightButton;
    ImageButton backButton;
    ImageButton infoButton;
    ImageButton okButton;
    ImageButton homeButton;
    ImageButton moreInfoButton;

    ImageButton button1;
    ImageButton button2;
    ImageButton button3;
    ImageButton button4;
    ImageButton button5;
    ImageButton button6;
    ImageButton button7;
    ImageButton button8;
    ImageButton button9;
    ImageButton button10;
    ImageButton button11;
    ImageButton button12;
    ImageButton button13;
    ImageButton button14;
    ImageButton button15;

    String[] buttonTypes = {
            "REWIND",
            "FASTFORWARD",
            "STOP",
            "PLAYPAUSE",
            "VOLUMEUP",
            "VOLUMEDOWN",
            "UP",
            "DOWN",
            "LEFT",
            "RIGHT",
            "BACK",
            "INFO",
            "OK",
            "HOME",
            "MOREINFO"
    };

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
        tcpClient.getClient().setTimeout(0);

        mFirebaseButtonRef = new Firebase("https://sweltering-torch-8619.firebaseio.com/android/button_transactions");
        mFirebaseLayoutRef = new Firebase("https://sweltering-torch-8619.firebaseio.com/android/layout_logs");

        mUserPreferences = getApplicationContext().getSharedPreferences("USER", MODE_PRIVATE);
        uID = mUserPreferences.getString("UID", "error");
        email.setText(mUserPreferences.getString("EMAIL", ""));

        hostManager = new HostManager(getApplicationContext());
        hostConnectionObserver = hostManager.getHostConnectionObserver();
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

        defaultUI = (LinearLayout) findViewById(R.id.default_remote);
        adaptiveUI = (RelativeLayout) findViewById(R.id.adaptive_remote);

        buttonInAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_in);
        buttonOutAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_out);

        loadButtons();
        setupButtons();
    }

    /*
     * Map buttons
     */
    public void loadButtons() {
        rewindButton = (ImageButton) findViewById(R.id.rewind_button);
        fastForwardButton = (ImageButton) findViewById(R.id.fastforward_button);
        stopButton = (ImageButton) findViewById(R.id.stop_button);
        playPauseButton = (ImageButton) findViewById(R.id.playpause_button);

        volumeUpButton = (ImageButton) findViewById(R.id.volumeup_button);
        volumeDownButton = (ImageButton) findViewById(R.id.volumedown_button);

        upButton = (ImageButton) findViewById(R.id.up_arrow_button);
        downButton = (ImageButton) findViewById(R.id.down_arrow_button);
        leftButton = (ImageButton) findViewById(R.id.left_arrow_button);
        rightButton = (ImageButton) findViewById(R.id.right_arrow_button);

        backButton = (ImageButton) findViewById(R.id.back_button);
        infoButton = (ImageButton) findViewById(R.id.info_button);
        okButton = (ImageButton) findViewById(R.id.ok_button);
        homeButton = (ImageButton) findViewById(R.id.home_button);
        moreInfoButton = (ImageButton) findViewById(R.id.more_info_button);

        button1 = (ImageButton) findViewById(R.id.button1);
        button2 = (ImageButton) findViewById(R.id.button2);
        button3 = (ImageButton) findViewById(R.id.button3);
        button4 = (ImageButton) findViewById(R.id.button4);
        button5 = (ImageButton) findViewById(R.id.button5);
        button6 = (ImageButton) findViewById(R.id.button6);
        button7 = (ImageButton) findViewById(R.id.button7);
        button8 = (ImageButton) findViewById(R.id.button8);
        button9 = (ImageButton) findViewById(R.id.button9);
        button10 = (ImageButton) findViewById(R.id.button10);
        button11 = (ImageButton) findViewById(R.id.button11);
        button12 = (ImageButton) findViewById(R.id.button12);
        button13 = (ImageButton) findViewById(R.id.button13);
        button14 = (ImageButton) findViewById(R.id.button14);
        button15 = (ImageButton) findViewById(R.id.button15);
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
    protected void onResume() {
        super.onResume();
        // Reconnect on resume
        hostConnectionObserver.registerPlayerObserver(this, true);
        if (!tcpClient.getClient().isConnected()) {
            tcpClient = new TcpClient(mHostPreferences.getString("ADDRESS", "error"));
            tcpClient.getClient().setTimeout(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister and stop on pause
        hostConnectionObserver.unregisterPlayerObserver(this);
        tcpClient.getClient().close();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_reconnect) {
            // Remove credentials from memory
            mHostPreferences.edit().clear().commit();
            Intent intent = new Intent(MainActivity.this, ConnectHostActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_update) {
            updateRemote();
        } else if (id == R.id.nav_default_ui) {
            loadDefaultRemote();
        } else if (id == R.id.nav_logout) {
            mFirebaseButtonRef.unauth();
            // Remove credentials from memort
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

    /*
     * Set on click listeners for buttons
     */
    public void setupButtons() {
        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ButtonTransaction transaction = new ButtonTransaction(buttonTypes[0]);
                Firebase transactionRef = mFirebaseButtonRef.child(uID);
                transactionRef.push().setValue(transaction);

                isPlaying = false;
                isRewindorFastForward = true;
                setPlayPauseButton();

                if (ListType.ItemsAll.TYPE_SONG.equals(currentNowPlayingItemType)) {
                    Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.PREVIOUS);
                    action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                } else {
                    Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.DECREMENT);
                    action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
                }
            }
        });

        fastForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ButtonTransaction transaction = new ButtonTransaction(buttonTypes[1]);
                Firebase transactionRef = mFirebaseButtonRef.child(uID);
                transactionRef.push().setValue(transaction);

                isPlaying = false;
                isRewindorFastForward = true;
                setPlayPauseButton();

                if (ListType.ItemsAll.TYPE_SONG.equals(currentNowPlayingItemType)) {
                    Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.NEXT);
                    action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                } else {
                    Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.INCREMENT);
                    action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ButtonTransaction transaction = new ButtonTransaction(buttonTypes[2]);
                Firebase transactionRef = mFirebaseButtonRef.child(uID);
                transactionRef.push().setValue(transaction);

                isPlaying = false;
                isRewindorFastForward = false;

                Player.Stop action = new Player.Stop(currentActivePlayerId);
                action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ButtonTransaction transaction = new ButtonTransaction(buttonTypes[3]);
                Firebase transactionRef = mFirebaseButtonRef.child(uID);
                transactionRef.push().setValue(transaction);

                if (isRewindorFastForward) {
                    isPlaying = true;
                    isRewindorFastForward = false;
                }

                Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
                action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
            }
        });

        setupVolumeRepeatButton(volumeUpButton,
                new com.sarahmizzi.fyp.kodi.jsonrpc.api.Application.SetVolume(GlobalType.IncrementDecrement.INCREMENT), buttonTypes[4]);
        setupVolumeRepeatButton(volumeDownButton,
                new com.sarahmizzi.fyp.kodi.jsonrpc.api.Application.SetVolume(GlobalType.IncrementDecrement.DECREMENT), buttonTypes[5]);

        setupRepeatButton(upButton, new Input.Up(), buttonTypes[6]);
        setupRepeatButton(downButton, new Input.Down(), buttonTypes[7]);
        setupRepeatButton(leftButton, new Input.Left(), buttonTypes[8]);
        setupRepeatButton(rightButton, new Input.Right(), buttonTypes[9]);

        setupDefaultButton(backButton, new Input.Back(), null, buttonTypes[10]);
        setupDefaultButton(infoButton,
                new Input.ExecuteAction(Input.ExecuteAction.INFO),
                new Input.ExecuteAction(Input.ExecuteAction.CODECINFO), buttonTypes[11]);
        setupDefaultButton(okButton, new Input.Select(), null, buttonTypes[12]);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ButtonTransaction transaction = new ButtonTransaction(buttonTypes[13]);
                Firebase transactionRef = mFirebaseButtonRef.child(uID);
                transactionRef.push().setValue(transaction);
                GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.HOME);
                action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
            }
        });

        moreInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ButtonTransaction transaction = new ButtonTransaction(buttonTypes[14]);
                Firebase transactionRef = mFirebaseButtonRef.child(uID);
                transactionRef.push().setValue(transaction);

                TcpRequest request = new TcpRequest();
                request.message = uID;
                tcpClient.getClient().sendTCP(request);

                final MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                        .title("More Information")
                        .content("Proceed to more information section on the laptop")
                        .progress(true, 0)
                        .canceledOnTouchOutside(false)
                        .build();

                materialDialog.show();

                tcpClient.getClient().addListener(new Listener() {
                    public void received(Connection connection, Object object) {
                        if (object instanceof TcpResponse) {
                            materialDialog.dismiss();
                        }
                    }
                });
            }
        });
    }

    private void setupVolumeRepeatButton(View button, final ApiMethod<Integer> action, final String descripton) {
        button.setOnTouchListener(new RepeatListener(UIUtils.initialButtonRepeatInterval, UIUtils.buttonRepeatInterval,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ButtonTransaction transaction = new ButtonTransaction(descripton);
                        Firebase transactionRef = mFirebaseButtonRef.child(uID);
                        transactionRef.push().setValue(transaction);

                        action.execute(hostManager.getConnection(), defaultIntActionCallback, callbackHandler);
                    }
                }));
    }

    private void setupRepeatButton(View button, final ApiMethod<String> action, final String description) {
        button.setOnTouchListener(new RepeatListener(UIUtils.initialButtonRepeatInterval, UIUtils.buttonRepeatInterval,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ButtonTransaction transaction = new ButtonTransaction(description);
                        Firebase transactionRef = mFirebaseButtonRef.child(uID);
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
                    ButtonTransaction transaction = new ButtonTransaction(description);
                    Firebase transactionRef = mFirebaseButtonRef.child(uID);
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
                    ButtonTransaction transaction = new ButtonTransaction(description);
                    Firebase transactionRef = mFirebaseButtonRef.child(uID);
                    transactionRef.push().setValue(transaction);
                    UIUtils.handleVibration(MainActivity.this);
                    longClickAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                    return true;
                }
            });
        }
    }

    /*
     * Change icon if video is playing
     */
    public void setPlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause_black_48dp);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_black_48dp);
        }
    }


    private ApiCallback<Integer> defaultPlaySpeedChangedCallback = new ApiCallback<Integer>() {
        @Override
        public void onSuccess(Integer result) {
            setPlayPauseButton();
        }

        @Override
        public void onError(int errorCode, String description) {
        }
    };

    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getItemResult, getPropertiesResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        currentNowPlayingItemType = getItemResult.type;
        // Switch icon
        isPlaying = true;
        setPlayPauseButton();
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getItemResult, getPropertiesResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        currentNowPlayingItemType = getItemResult.type;
        // Switch icon
        isPlaying = false;
        setPlayPauseButton();
    }

    public void playerOnStop() {
        isPlaying = false;
        setPlayPauseButton();
    }

    public void playerOnConnectionError(int errorCode, String description) {
        HostInfo hostInfo = hostManager.getHostInfo();
    }

    public void playerNoResultsYet() {
    }

    public void systemOnQuit() {
        playerNoResultsYet();
    }

    public void inputOnInputRequested(String title, String type, String value) {
    }

    public void observerOnStopObserving() {
    }

    private void setNowPlayingInfo(ListType.ItemsAll nowPlaying,
                                   PlayerType.PropertyValue properties) {

        switch (nowPlaying.type) {
            case ListType.ItemsAll.TYPE_MOVIE:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                break;
            case ListType.ItemsAll.TYPE_EPISODE:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                break;
            case ListType.ItemsAll.TYPE_SONG:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                break;
            case ListType.ItemsAll.TYPE_CHANNEL:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                break;
            default:
                Log.d(TAG, nowPlaying.type.toString() + " - default case : " + nowPlaying.title);
                String videoID = nowPlaying.thumbnail.split("\\%2f")[4];
                try {
                    readJsonFromUrl("https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + videoID + "&key=AIzaSyB5Dyf5NFWhTErQ0J3o1iQYgZuEXR7AwLI", videoID);
                } catch (JSONException jE) {
                    Log.e(TAG, "JSONEXCEPTION:" + jE.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "IOEXCEPTION:" + e.getMessage());
                }

                break;
        }
    }

    public void readJsonFromUrl(String link, String id) throws IOException, JSONException {
        // Connect to the URL using java's native library
        URL url = new URL(link);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.connect();

        // Convert to a JSON object to print data
        com.google.gson.JsonParser jp = new com.google.gson.JsonParser();
        // Convert the input stream to a json element
        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
        JsonObject rootObj = root.getAsJsonObject();

        JsonArray items = rootObj.getAsJsonArray("items");
        JsonObject firstItem = items.get(0).getAsJsonObject();
        JsonObject snippet = firstItem.getAsJsonObject("snippet");

        // Get Title
        String title = snippet.getAsJsonPrimitive("title").getAsString();

        // Get Tags
        JsonArray tagsArray = snippet.getAsJsonArray("tags");
        Gson gson = new Gson();
        String[] tags = gson.fromJson(tagsArray, String[].class);

        // Get Category
        int categoryId = snippet.getAsJsonPrimitive("categoryId").getAsInt();

        url = new URL("https://www.googleapis.com/youtube/v3/videoCategories?part=snippet&id=" + categoryId + "&key=AIzaSyB5Dyf5NFWhTErQ0J3o1iQYgZuEXR7AwLI");
        request = (HttpURLConnection) url.openConnection();
        request.connect();

        jp = new com.google.gson.JsonParser();
        root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
        rootObj = root.getAsJsonObject();
        items = rootObj.getAsJsonArray("items");
        firstItem = items.get(0).getAsJsonObject();
        snippet = firstItem.getAsJsonObject("snippet");
        String category = snippet.getAsJsonPrimitive("title").getAsString();

        Video video = new Video(id, title, category, tags);

        Firebase firebaseVideoRef = new Firebase("https://sweltering-torch-8619.firebaseio.com/android/video");
        Firebase videoRef = firebaseVideoRef.child(uID);
        videoRef.push().setValue(video);
    }

    public void loadDefaultRemote() {
        loadButtons();
        setupButtons();
        defaultUI.setVisibility(View.VISIBLE);
        adaptiveUI.setVisibility(View.GONE);

        Date date = new Date();
        ArrayList<String> buttons = new ArrayList<>();
        for (int i = 0; i < buttonTypes.length; i++) {
            buttons.add(buttonTypes[i]);
        }
        LayoutLog log = new LayoutLog("DEFAULT", date.toString(), buttons);
        Firebase logRef = mFirebaseLayoutRef.child(uID);
        logRef.push().setValue(log);
    }

    /*
     * Set adaptive layout
     */
    public void updateRemote() {
        final ArrayList<RemoteButtonData> remoteButtonTransactionData = new ArrayList<>();

        Query queryRef = mFirebaseButtonRef.child(uID);

        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot querySnapshot) {
                for (int i = 0; i < buttonTypes.length; i++) {
                    remoteButtonTransactionData.add(new RemoteButtonData(buttonTypes[i], 0));
                }
                for (DataSnapshot currentDataSnapshot : querySnapshot.getChildren()) {
                    ButtonTransaction current = currentDataSnapshot.getChildren().iterator().next().getValue(ButtonTransaction.class);

                    for (int j = 0; j < remoteButtonTransactionData.size(); j++) {
                        if (current.getDescription().equals(remoteButtonTransactionData.get(j).getDescription())) {
                            remoteButtonTransactionData.get(j).incrementCount();
                            break;
                        }
                    }
                }

                updateRemoteUI(sortByMostFrequentlyUsed(remoteButtonTransactionData));

            }

            @Override
            public void onCancelled(FirebaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    public ArrayList<RemoteButtonData> sortByMostFrequentlyUsed(ArrayList<RemoteButtonData> list) {
        ArrayList<RemoteButtonData> sortedList = new ArrayList<>();
        sortedList.add(list.get(0));

        for (int i = 1; i < list.size(); i++) {
            for (int j = 0; j < sortedList.size(); j++) {
                if ((list.get(i).getCount() < sortedList.get(j).getCount()) && (j == (sortedList.size() - 1))) {
                    sortedList.add(list.get(i));
                } else if ((list.get(i).getCount() < sortedList.get(j).getCount())) {
                    continue;
                } else if ((list.get(i).getCount() > sortedList.get(j).getCount())) {
                    sortedList.add(j, list.get(i));
                    break;
                }
            }
        }

        return sortedList;
    }

    public void updateRemoteUI(ArrayList<RemoteButtonData> sortedList) {
        defaultUI.setVisibility(View.GONE);
        adaptiveUI.setVisibility(View.VISIBLE);

        ArrayList<ButtonTransaction> list = new ArrayList<>();
        ArrayList<ImageButton> buttonsList = new ArrayList<>();
        ArrayList<String> buttons = new ArrayList<>();

        buttonsList.add(button1);
        buttonsList.add(button2);
        buttonsList.add(button3);
        buttonsList.add(button4);
        buttonsList.add(button5);
        buttonsList.add(button6);
        buttonsList.add(button7);
        buttonsList.add(button8);
        buttonsList.add(button9);
        buttonsList.add(button10);
        buttonsList.add(button11);
        buttonsList.add(button12);
        buttonsList.add(button13);
        buttonsList.add(button14);
        buttonsList.add(button15);

        for (int i = 0; i < buttonTypes.length; i++) {
            list.add(new ButtonTransaction(buttonTypes[i]));
        }

        for (int i = 0; i < sortedList.size(); i++) {
            switch (sortedList.get(i).getDescription()) {
                case "REWIND":
                    buttonsList.get(0).setImageResource(R.drawable.ic_rewind_black_48dp);
                    rewindButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("REWIND")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("REWIND");
                    break;
                case "FASTFORWARD":
                    buttonsList.get(0).setImageResource(R.drawable.ic_fast_forward_black_48dp);
                    fastForwardButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("FASTFORWARD")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("FASTFORWARD");
                    break;
                case "STOP":
                    buttonsList.get(0).setImageResource(R.drawable.ic_stop_black_48dp);
                    stopButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("STOP")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("STOP");
                    break;
                case "PLAYPAUSE":
                    buttonsList.get(0).setImageResource(R.drawable.ic_play_black_48dp);
                    playPauseButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("PLAYPAUSE")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("PLAYPAUSE");
                    break;
                case "VOLUMEUP":
                    buttonsList.get(0).setImageResource(R.drawable.ic_volume_high_black_48dp);
                    volumeUpButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("VOLUMEUP")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("VOLUMEUP");
                    break;
                case "VOLUMEDOWN":
                    buttonsList.get(0).setImageResource(R.drawable.ic_volume_medium_black_48dp);
                    volumeDownButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("VOLUMEDOWN")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("VOLUMEDOWN");
                    break;
                case "UP":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_up_black_48dp);
                    upButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("UP")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("UP");
                    break;
                case "DOWN":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_down_black_48dp);
                    downButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("DOWN")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("DOWN");
                    break;
                case "LEFT":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_left_black_48dp);
                    leftButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("LEFT")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("LEFT");
                    break;
                case "RIGHT":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_right_black_48dp);
                    rightButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("RIGHT")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("RIGHT");
                    break;
                case "BACK":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_left_bold_circle_black_48dp);
                    backButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("BACK")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("BACK");
                    break;
                case "INFO":
                    buttonsList.get(0).setImageResource(R.drawable.ic_information_black_48dp);
                    infoButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("INFO")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("INFO");
                    break;
                case "OK":
                    buttonsList.get(0).setImageResource(R.drawable.ic_checkbox_blank_circle_black_48dp);
                    okButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("OK")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("OK");
                    break;
                case "HOME":
                    buttonsList.get(0).setImageResource(R.drawable.ic_home_black_48dp);
                    homeButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("HOME")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("HOME");
                    break;
                case "MOREINFO":
                    buttonsList.get(0).setImageResource(R.drawable.ic_window_restore_black_48dp);
                    moreInfoButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    for (ButtonTransaction item : list) {
                        if (item.getDescription().equals("MOREINFO")) {
                            list.remove(item);
                            break;
                        }
                    }
                    buttons.add("MOREINFO");
                    break;
            }
        }

        for (int i = 0; i < list.size(); i++) {
            switch (list.get(i).getDescription()) {
                case "REWIND":
                    buttonsList.get(0).setImageResource(R.drawable.ic_rewind_black_48dp);
                    rewindButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("REWIND");
                    break;
                case "FASTFORWARD":
                    buttonsList.get(0).setImageResource(R.drawable.ic_fast_forward_black_48dp);
                    fastForwardButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("FASTFORWARD");
                    break;
                case "STOP":
                    buttonsList.get(0).setImageResource(R.drawable.ic_stop_black_48dp);
                    stopButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("STOP");
                    break;
                case "PLAYPAUSE":
                    buttonsList.get(0).setImageResource(R.drawable.ic_play_black_48dp);
                    playPauseButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("PLAYPAUSE");
                    break;
                case "VOLUMEUP":
                    buttonsList.get(0).setImageResource(R.drawable.ic_volume_high_black_48dp);
                    volumeUpButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("VOLUMEUP");
                    break;
                case "VOLUMEDOWN":
                    buttonsList.get(0).setImageResource(R.drawable.ic_volume_medium_black_48dp);
                    volumeDownButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("VOLUMEDOWN");
                    break;
                case "UP":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_up_black_48dp);
                    upButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("UP");
                    break;
                case "DOWN":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_down_black_48dp);
                    downButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("DOWN");
                    break;
                case "LEFT":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_left_black_48dp);
                    leftButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("LEFT");
                    break;
                case "RIGHT":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_right_black_48dp);
                    rightButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("RIGHT");
                    break;
                case "BACK":
                    buttonsList.get(0).setImageResource(R.drawable.ic_arrow_left_bold_circle_black_48dp);
                    backButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("BACK");
                    break;
                case "INFO":
                    buttonsList.get(0).setImageResource(R.drawable.ic_information_black_48dp);
                    infoButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("INFO");
                    break;
                case "OK":
                    buttonsList.get(0).setImageResource(R.drawable.ic_checkbox_blank_circle_black_48dp);
                    okButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("OK");
                    break;
                case "HOME":
                    buttonsList.get(0).setImageResource(R.drawable.ic_home_black_48dp);
                    homeButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("HOME");
                    break;
                case "MOREINFO":
                    buttonsList.get(0).setImageResource(R.drawable.ic_window_restore_black_48dp);
                    moreInfoButton = buttonsList.get(0);
                    buttonsList.remove(0);
                    buttons.add("MOREINFO");
                    break;
            }
        }

        setupButtons();

        Date date = new Date();
        LayoutLog log = new LayoutLog("ADAPTIVE", date.toString(), buttons);
        Firebase logRef = mFirebaseLayoutRef.child(uID);
        logRef.push().setValue(log);
    }
}
