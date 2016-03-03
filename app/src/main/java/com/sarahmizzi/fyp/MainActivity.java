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
import android.widget.TextView;
import com.firebase.client.Firebase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sarahmizzi.fyp.classes.Transaction;
import com.sarahmizzi.fyp.classes.Video;
import com.sarahmizzi.fyp.connection.HostConnectionObserver;
import com.sarahmizzi.fyp.connection.HostInfo;
import com.sarahmizzi.fyp.connection.HostManager;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiCallback;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiMethod;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.GUI;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.Input;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ListType;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.PlayerType;
import com.sarahmizzi.fyp.utils.RepeatListener;
import com.sarahmizzi.fyp.utils.TcpRequest;
import com.sarahmizzi.fyp.utils.TcpClient;
import com.sarahmizzi.fyp.utils.UIUtils;
import org.json.JSONException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, HostConnectionObserver.PlayerEventsObserver {
    final String TAG = MainActivity.class.getSimpleName();

    private HostManager hostManager = null;
    private HostConnectionObserver hostConnectionObserver;
    private Handler callbackHandler = new Handler();
    private View.OnTouchListener feedbackTouchListener;

    private int currentActivePlayerId = -1;
    private String currentNowPlayingItemType = null;

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

    @Override
    protected void onResume() {
        super.onResume();
        hostConnectionObserver.registerPlayerObserver(this, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        hostConnectionObserver.unregisterPlayerObserver(this);
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

    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getItemResult, getPropertiesResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        currentNowPlayingItemType = getItemResult.type;
        // Switch icon
        //UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getItemResult, getPropertiesResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        currentNowPlayingItemType = getItemResult.type;
        // Switch icon
        //UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);
    }

    public void playerOnStop() {
        HostInfo hostInfo = hostManager.getHostInfo();

        /*switchToPanel(R.id.info_panel, true);
        infoTitle.setText(R.string.nothing_playing);
        infoMessage.setText(String.format(getString(R.string.connected_to), hostInfo.getName()));*/
    }

    public void playerOnConnectionError(int errorCode, String description) {
        HostInfo hostInfo = hostManager.getHostInfo();

        /*switchToPanel(R.id.info_panel, false);
        if (hostInfo != null) {
            infoTitle.setText(R.string.connecting);
            // TODO: check error code
            infoMessage.setText(String.format(getString(R.string.connecting_to), hostInfo.getName(), hostInfo.getAddress()));
        } else {
            infoTitle.setText(R.string.no_xbmc_configured);
            infoMessage.setText(null);
        }*/
    }

    public void playerNoResultsYet() {
        // Initialize info panel
        /*switchToPanel(R.id.info_panel, false);
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo != null) {
            infoTitle.setText(R.string.connecting);
        } else {
            infoTitle.setText(R.string.no_xbmc_configured);
        }
        infoMessage.setText(null);*/
    }

    public void systemOnQuit() {
        playerNoResultsYet();
    }

    public void inputOnInputRequested(String title, String type, String value) {}
    public void observerOnStopObserving() {}

    private void setNowPlayingInfo(ListType.ItemsAll nowPlaying,
                                   PlayerType.PropertyValue properties) {
        String title, underTitle, thumbnailUrl;
        int currentRewindIcon, currentFastForwardIcon;

        switch (nowPlaying.type) {
            case ListType.ItemsAll.TYPE_MOVIE:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                /*switchToPanel(R.id.media_panel, true);

                title = nowPlaying.title;
                underTitle = nowPlaying.tagline;
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;*/
                break;
            case ListType.ItemsAll.TYPE_EPISODE:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                /*switchToPanel(R.id.media_panel, true);

                title = nowPlaying.title;
                String season = String.format(getString(R.string.season_episode_abbrev), nowPlaying.season, nowPlaying.episode);
                underTitle = String.format("%s | %s", nowPlaying.showtitle, season);
                thumbnailUrl = nowPlaying.art.poster;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;*/
                break;
            case ListType.ItemsAll.TYPE_SONG:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                /*switchToPanel(R.id.media_panel, true);

                title = nowPlaying.title;
                underTitle = nowPlaying.displayartist + " | " + nowPlaying.album;
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = skipNextIcon;
                currentRewindIcon = skipPreviousIcon;*/
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                /*switchToPanel(R.id.media_panel, true);

                title = nowPlaying.title;
                underTitle = Utils.listStringConcat(nowPlaying.artist, ", ") + " | " + nowPlaying.album;
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;*/
                break;
            case ListType.ItemsAll.TYPE_CHANNEL:
                Log.d(TAG, nowPlaying.type.toString() + " : " + nowPlaying.title);
                /*switchToPanel(R.id.media_panel, true);

                title = nowPlaying.label;
                underTitle = nowPlaying.title;
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;*/
                break;
            default:
                Log.d(TAG, nowPlaying.type.toString() + " - default case : " + nowPlaying.title);
                String videoID = nowPlaying.thumbnail.split("\\%2f")[4];
                try {
                    readJsonFromUrl("https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + videoID + "&key=AIzaSyB5Dyf5NFWhTErQ0J3o1iQYgZuEXR7AwLI", videoID);
                }
                catch(JSONException jE) {
                    Log.e(TAG, "JSONEXCEPTION:" + jE.getMessage());
                }
                catch(IOException e){
                    Log.e(TAG, "IOEXCEPTION:" + e.getMessage());
                }
                /*switchToPanel(R.id.media_panel, true);
                title = nowPlaying.label;
                underTitle = "";
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;*/

                break;
        }

        //nowPlayingTitle.setText(title);
        //nowPlayingDetails.setText(underTitle);

        //fastForwardButton.setImageResource(currentFastForwardIcon);
        //rewindButton.setImageResource(currentRewindIcon);
//        // If not video, change aspect ration of poster to a square
//        boolean isVideo = (nowPlaying.type.equals(ListType.ItemsAll.TYPE_MOVIE)) ||
//                (nowPlaying.type.equals(ListType.ItemsAll.TYPE_EPISODE));
//        if (!isVideo) {
//            ViewGroup.LayoutParams layoutParams = thumbnail.getLayoutParams();
//            layoutParams.width = layoutParams.height;
//            thumbnail.setLayoutParams(layoutParams);
//        }

        /*UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                thumbnailUrl, title,
                thumbnail, thumbnail.getWidth(), thumbnail.getHeight());*/
    }

    public void readJsonFromUrl(String link, String id) throws IOException, JSONException {
        // Connect to the URL using java's native library
        URL url = new URL(link);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.connect();

        // Convert to a JSON object to print data
        com.google.gson.JsonParser jp = new com.google.gson.JsonParser();
        //Convert the input stream to a json element
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
        String[] tags = gson.fromJson(tagsArray , String[].class);

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
}
