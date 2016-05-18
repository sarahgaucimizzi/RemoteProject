package com.sarahmizzi.fyp.connection;

/**
 * Created by Sarah on 29-Feb-16.
 * Refer to Kore Remote on Android.
 */

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.util.Log;

import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiCallback;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ListType;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.PlayerType;
import com.sarahmizzi.fyp.notifications.Input;
import com.sarahmizzi.fyp.notifications.Player;
import com.sarahmizzi.fyp.notifications.System;

public class HostConnectionObserver
        implements HostConnection.PlayerNotificationsObserver,
        HostConnection.SystemNotificationsObserver,
        HostConnection.InputNotificationsObserver {
    public static final String TAG = HostConnectionObserver.class.getSimpleName();

    private int lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
    private PlayerType.GetActivePlayersReturnType lastGetActivePlayerResult = null;
    private PlayerType.PropertyValue lastGetPropertiesResult = null;
    private ListType.ItemsAll lastGetItemResult = null;
    private int lastErrorCode;
    private String lastErrorDescription;

    /**
     * Interface that an observer has to implement to receive player events
     */
    public interface PlayerEventsObserver {
        public static final int PLAYER_NO_RESULT = 0,
                PLAYER_CONNECTION_ERROR = 1,
                PLAYER_IS_PLAYING = 2,
                PLAYER_IS_PAUSED = 3,
                PLAYER_IS_STOPPED = 4;

        /**
         * Notifies that something is playing
         */
        public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                 PlayerType.PropertyValue getPropertiesResult,
                                 ListType.ItemsAll getItemResult);

        /**
         * Notifies that something is paused
         */
        public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                  PlayerType.PropertyValue getPropertiesResult,
                                  ListType.ItemsAll getItemResult);

        /**
         * Notifies that media is stopped/nothing is playing
         */
        public void playerOnStop();

        /**
         * Called when we get a connection error
         */
        public void playerOnConnectionError(int errorCode, String description);

        /**
         * Notifies that we don't have a result yet
         */
        public void playerNoResultsYet();

        /**
         * Notifies that XBMC has quit/shutdown/sleep
         */
        public void systemOnQuit();

        /**
         * Notifies that XBMC has requested input
         */
        public void inputOnInputRequested(String title, String type, String value);

        /**
         * Notifies the observer that it this is stopping
         */
        public void observerOnStopObserving();
    }

    /**
     * The connection on which to listen
     */
    private HostConnection connection;

    /**
     * The list of observers
     */
    private List<PlayerEventsObserver> playerEventsObservers = new ArrayList<PlayerEventsObserver>();

    /**
     * Handlers for which observer, on which to notify them
     */

    private Handler checkerHandler = new Handler();
    private Runnable httpCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            final int HTTP_NOTIFICATION_CHECK_INTERVAL = 3000;
            // If no one is listening to this, just exit
            if (playerEventsObservers.size() == 0) return;

            // Check whats playing
            checkWhatsPlaying();

            // Keep checking
            checkerHandler.postDelayed(this, HTTP_NOTIFICATION_CHECK_INTERVAL);
        }
    };

    public HostConnectionObserver(HostConnection connection) {
        this.connection = connection;
    }

    /**
     * Registers a new observer that will be notified about player events
     */
    public void registerPlayerObserver(PlayerEventsObserver observer, boolean replyImmediately) {
        if (this.connection == null)
            return;

        playerEventsObservers.add(observer);

        if (replyImmediately) replyWithLastResult(observer);

        if (playerEventsObservers.size() == 1) {
            checkerHandler.post(httpCheckerRunnable);
        }
    }

    /**
     * Unregisters a previously registered observer
     */
    public void unregisterPlayerObserver(PlayerEventsObserver observer) {
        playerEventsObservers.remove(observer);

        Log.d(TAG, "Unregistering observer. Still got " + playerEventsObservers.size() +
                " observers.");

        if (playerEventsObservers.size() == 0) {
            // Unregister and stop checking
            checkerHandler.removeCallbacks(httpCheckerRunnable);
            lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
        }
    }

    /**
     * Unregisters all observers
     */
    public void stopObserving() {
        for (final PlayerEventsObserver observer : playerEventsObservers)
            observer.observerOnStopObserving();

        playerEventsObservers.clear();

        checkerHandler.removeCallbacks(httpCheckerRunnable);
        lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
    }

    /**
     * The {@link HostConnection.PlayerNotificationsObserver} interface methods
     */
    public void onPlay(Player.OnPlay notification) {
        chainCallGetActivePlayers();
    }

    public void onPause(Player.OnPause
                                notification) {
        chainCallGetActivePlayers();
    }

    public void onSpeedChanged(Player.OnSpeedChanged notification) {
        chainCallGetActivePlayers();
    }

    public void onSeek(Player.OnSeek notification) {
        chainCallGetActivePlayers();
    }

    public void onStop(Player.OnStop notification) {
        notifyNothingIsPlaying(playerEventsObservers);
    }

    /**
     * The {@link HostConnection.SystemNotificationsObserver} interface methods
     */
    public void onQuit(System.OnQuit notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.systemOnQuit();
        }
    }

    public void onRestart(System.OnRestart notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.systemOnQuit();
        }
    }

    public void onSleep(System.OnSleep notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.systemOnQuit();
        }
    }

    public void onInputRequested(Input.OnInputRequested notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.inputOnInputRequested(notification.title, notification.type, notification.value);
        }
    }

    /**
     * Checks whats playing and notifies observers
     */
    private void checkWhatsPlaying() {
        Log.d(TAG, "Checking whats playing");

        chainCallGetActivePlayers();
    }

    /**
     * Calls Player.GetActivePlayers
     * On success chains execution to chainCallGetProperties
     */
    private void chainCallGetActivePlayers() {
        com.sarahmizzi.fyp.kodi.jsonrpc.api.Player.GetActivePlayers getActivePlayers = new com.sarahmizzi.fyp.kodi.jsonrpc.api.Player.GetActivePlayers();
        getActivePlayers.execute(connection, new ApiCallback<ArrayList<PlayerType.GetActivePlayersReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlayerType.GetActivePlayersReturnType> result) {
                if (result.isEmpty()) {
                    Log.d(TAG, "Nothing is playing");
                    notifyNothingIsPlaying(playerEventsObservers);
                    return;
                }
                chainCallGetProperties(result.get(0));
            }

            @Override
            public void onError(int errorCode, String description) {
                Log.d(TAG, "Notifying error");
                notifyConnectionError(errorCode, description, playerEventsObservers);
            }
        }, checkerHandler);
    }

    /**
     * Calls Player.GetProperties
     * On success chains execution to chainCallGetItem
     */
    private void chainCallGetProperties(final PlayerType.GetActivePlayersReturnType getActivePlayersResult) {
        String propertiesToGet[] = new String[]{
                PlayerType.PropertyName.SPEED,
                PlayerType.PropertyName.PERCENTAGE,
                PlayerType.PropertyName.POSITION,
                PlayerType.PropertyName.TIME,
                PlayerType.PropertyName.TOTALTIME,
                PlayerType.PropertyName.REPEAT,
                PlayerType.PropertyName.SHUFFLED,
                PlayerType.PropertyName.CURRENTAUDIOSTREAM,
                PlayerType.PropertyName.CURRENTSUBTITLE,
                PlayerType.PropertyName.AUDIOSTREAMS,
                PlayerType.PropertyName.SUBTITLES,
                PlayerType.PropertyName.PLAYLISTID,
        };

        com.sarahmizzi.fyp.kodi.jsonrpc.api.Player.GetProperties getProperties = new com.sarahmizzi.fyp.kodi.jsonrpc.api.Player.GetProperties(getActivePlayersResult.playerid, propertiesToGet);
        getProperties.execute(connection, new ApiCallback<PlayerType.PropertyValue>() {
            @Override
            public void onSuccess(PlayerType.PropertyValue result) {
                chainCallGetItem(getActivePlayersResult, result);
            }

            @Override
            public void onError(int errorCode, String description) {
                notifyConnectionError(errorCode, description, playerEventsObservers);
            }
        }, checkerHandler);
    }

    /**
     * Calls Player.GetItem
     * On success notifies observers
     */
    private void chainCallGetItem(final PlayerType.GetActivePlayersReturnType getActivePlayersResult,
                                  final PlayerType.PropertyValue getPropertiesResult) {
//        COMMENT, LYRICS, MUSICBRAINZTRACKID, MUSICBRAINZARTISTID, MUSICBRAINZALBUMID,
//        MUSICBRAINZALBUMARTISTID, TRAILER, ORIGINALTITLE, LASTPLAYED, MPAA, COUNTRY,
//        PRODUCTIONCODE, SET, SHOWLINK, FILE,
//        ARTISTID, ALBUMID, TVSHOW_ID, SETID, WATCHEDEPISODES, DISC, TAG, GENREID,
//        ALBUMARTISTID, DESCRIPTION, THEME, MOOD, STYLE, ALBUMLABEL, SORTTITLE, UNIQUEID,
//        DATEADDED, CHANNEL, CHANNELTYPE, HIDDEN, LOCKED, CHANNELNUMBER, STARTTIME, ENDTIME,
//        EPISODEGUIDE, ORIGINALTITLE, PLAYCOUNT, PLOTOUTLINE, SET,
        String[] propertiesToGet = new String[]{
                ListType.FieldsAll.ART,
                ListType.FieldsAll.ARTIST,
                ListType.FieldsAll.ALBUMARTIST,
                ListType.FieldsAll.ALBUM,
                ListType.FieldsAll.CAST,
                ListType.FieldsAll.DIRECTOR,
                ListType.FieldsAll.DISPLAYARTIST,
                ListType.FieldsAll.DURATION,
                ListType.FieldsAll.EPISODE,
                ListType.FieldsAll.FANART,
                ListType.FieldsAll.FILE,
                ListType.FieldsAll.FIRSTAIRED,
                ListType.FieldsAll.GENRE,
                ListType.FieldsAll.IMDBNUMBER,
                ListType.FieldsAll.PLOT,
                ListType.FieldsAll.PREMIERED,
                ListType.FieldsAll.RATING,
                ListType.FieldsAll.RESUME,
                ListType.FieldsAll.RUNTIME,
                ListType.FieldsAll.SEASON,
                ListType.FieldsAll.SHOWTITLE,
                ListType.FieldsAll.STREAMDETAILS,
                ListType.FieldsAll.STUDIO,
                ListType.FieldsAll.TAGLINE,
                ListType.FieldsAll.THUMBNAIL,
                ListType.FieldsAll.TITLE,
                ListType.FieldsAll.TOP250,
                ListType.FieldsAll.TRACK,
                ListType.FieldsAll.VOTES,
                ListType.FieldsAll.WRITER,
                ListType.FieldsAll.YEAR,
                ListType.FieldsAll.DESCRIPTION,
        };
        com.sarahmizzi.fyp.kodi.jsonrpc.api.Player.GetItem getItem = new com.sarahmizzi.fyp.kodi.jsonrpc.api.Player.GetItem(getActivePlayersResult.playerid, propertiesToGet);
        getItem.execute(connection, new ApiCallback<ListType.ItemsAll>() {
            @Override
            public void onSuccess(ListType.ItemsAll result) {
                notifySomethingIsPlaying(getActivePlayersResult, getPropertiesResult, result, playerEventsObservers);
            }

            @Override
            public void onError(int errorCode, String description) {
                notifyConnectionError(errorCode, description, playerEventsObservers);
            }
        }, checkerHandler);
    }

    private boolean forceReply = false;

    /**
     * Notifies a list of observers of a connection error
     * Only notifies them if the result is different from the last one
     */
    private void notifyConnectionError(final int errorCode, final String description, List<PlayerEventsObserver> observers) {
        // Reply if different from last result
        if (forceReply ||
                (lastCallResult != PlayerEventsObserver.PLAYER_CONNECTION_ERROR) ||
                (lastErrorCode != errorCode)) {
            lastCallResult = PlayerEventsObserver.PLAYER_CONNECTION_ERROR;
            lastErrorCode = errorCode;
            lastErrorDescription = description;
            forceReply = false;
            // Copy list to prevent ConcurrentModificationExceptions
            List<PlayerEventsObserver> allObservers = new ArrayList<>(observers);
            for (final PlayerEventsObserver observer : allObservers) {
                notifyConnectionError(errorCode, description, observer);
            }
        }
    }

    /**
     * Notifies a specific observer of a connection error
     * Always notifies the observer, and doesn't save results in last call
     */
    private void notifyConnectionError(final int errorCode, final String description, PlayerEventsObserver observer) {
        observer.playerOnConnectionError(errorCode, description);
    }


    /**
     * Nothing is playing, notify observers calling playerOnStop
     * Only notifies them if the result is different from the last one
     */
    private void notifyNothingIsPlaying(List<PlayerEventsObserver> observers) {
        // Reply if forced or different from last result
        if (forceReply ||
                (lastCallResult != PlayerEventsObserver.PLAYER_IS_STOPPED)) {
            lastCallResult = PlayerEventsObserver.PLAYER_IS_STOPPED;
            forceReply = false;
            // Copy list to prevent ConcurrentModificationExceptions
            List<PlayerEventsObserver> allObservers = new ArrayList<>(observers);
            for (final PlayerEventsObserver observer : allObservers) {
                notifyNothingIsPlaying(observer);
            }
        }
    }

    /**
     * Notifies a specific observer
     * Always notifies the observer, and doesn't save results in last call
     */
    private void notifyNothingIsPlaying(PlayerEventsObserver observer) {
        observer.playerOnStop();
    }

    /**
     * Something is playing or paused, notify observers
     * Only notifies them if the result is different from the last one
     */
    private void notifySomethingIsPlaying(final PlayerType.GetActivePlayersReturnType getActivePlayersResult,
                                          final PlayerType.PropertyValue getPropertiesResult,
                                          final ListType.ItemsAll getItemResult,
                                          List<PlayerEventsObserver> observers) {
        int currentCallResult = (getPropertiesResult.speed == 0) ?
                PlayerEventsObserver.PLAYER_IS_PAUSED : PlayerEventsObserver.PLAYER_IS_PLAYING;
        if (forceReply ||
                (lastCallResult != currentCallResult) ||
                (lastGetPropertiesResult.speed != getPropertiesResult.speed) ||
                (lastGetPropertiesResult.shuffled != getPropertiesResult.shuffled) ||
                (!lastGetPropertiesResult.repeat.equals(getPropertiesResult.repeat)) ||
                (lastGetItemResult.id != getItemResult.id) ||
                (!lastGetItemResult.label.equals(getItemResult.label))) {
            lastCallResult = currentCallResult;
            lastGetActivePlayerResult = getActivePlayersResult;
            lastGetPropertiesResult = getPropertiesResult;
            lastGetItemResult = getItemResult;
            forceReply = false;
            // Copy list to prevent ConcurrentModificationExceptions
            List<PlayerEventsObserver> allObservers = new ArrayList<>(observers);
            for (final PlayerEventsObserver observer : allObservers) {
                notifySomethingIsPlaying(getActivePlayersResult, getPropertiesResult, getItemResult, observer);
            }
        }
    }

    /**
     * Something is playing or paused, notify a specific observer
     * Always notifies the observer, and doesn't save results in last call
     */
    private void notifySomethingIsPlaying(final PlayerType.GetActivePlayersReturnType getActivePlayersResult,
                                          final PlayerType.PropertyValue getPropertiesResult,
                                          final ListType.ItemsAll getItemResult,
                                          PlayerEventsObserver observer) {
        if (getPropertiesResult.speed == 0) {
            // Paused
            observer.playerOnPause(getActivePlayersResult, getPropertiesResult, getItemResult);
        } else {
            // Playing
            observer.playerOnPlay(getActivePlayersResult, getPropertiesResult, getItemResult);
        }
    }

    /**
     * Replies to the observer with the last result we got.
     * If we have no result, nothing will be called on the observer interface.
     */
    public void replyWithLastResult(PlayerEventsObserver observer) {
        switch (lastCallResult) {
            case PlayerEventsObserver.PLAYER_CONNECTION_ERROR:
                notifyConnectionError(lastErrorCode, lastErrorDescription, observer);
                break;
            case PlayerEventsObserver.PLAYER_IS_STOPPED:
                notifyNothingIsPlaying(observer);
                break;
            case PlayerEventsObserver.PLAYER_IS_PAUSED:
            case PlayerEventsObserver.PLAYER_IS_PLAYING:
                notifySomethingIsPlaying(lastGetActivePlayerResult, lastGetPropertiesResult, lastGetItemResult, observer);
                break;
            case PlayerEventsObserver.PLAYER_NO_RESULT:
                observer.playerNoResultsYet();
                break;
        }
    }

    /**
     * Forces a refresh of the current cached results
     */
    public void forceRefreshResults() {
        forceReply = true;
        chainCallGetActivePlayers();
    }
}

