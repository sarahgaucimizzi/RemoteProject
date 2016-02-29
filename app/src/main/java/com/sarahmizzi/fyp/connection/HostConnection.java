package com.sarahmizzi.fyp.connection;

import android.os.*;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiCallback;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiException;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiMethod;
import com.sarahmizzi.fyp.notifications.Input;
import com.sarahmizzi.fyp.notifications.Player;
import com.sarahmizzi.fyp.notifications.System;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Created by Sarah on 02-Feb-16.
 * For communication with host.
 */
public class HostConnection {
    final String TAG = HostConnection.class.getSimpleName();
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000; // ms
    public static final int PROTOCOL_HTTP = 1;
    private final int connectTimeout;
    private final HostInfo hostInfo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HashMap<String, MethodCallInfo<?>> clientCallbacks = new HashMap<String, MethodCallInfo<?>>();

    private ExecutorService executorService;
    private OkHttpClient httpClient = null;
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    public HostConnection(final HostInfo hostInfo) {
        this(hostInfo, DEFAULT_CONNECT_TIMEOUT);
    }

    public HostConnection(final HostInfo hostInfo, int connectTimeout) {
        this.hostInfo = hostInfo;
        // Create a single threaded executor
        this.executorService = Executors.newSingleThreadExecutor();
        // Set timeout
        this.connectTimeout = connectTimeout;
    }

    /**
     * Calls the a method on the server
     * This call is always asynchronous. The results will be posted, through the
     * {@link ApiCallback callback} parameter, on the specified {@link android.os.Handler}.
     *
     * @param method   Method object that represents the methood too call
     * @param callback {@link ApiCallback} to post the response to
     * @param handler  {@link Handler} to invoke callbacks on
     * @param <T>      Method return type
     */
    public <T> void execute(final ApiMethod<T> method, final ApiCallback<T> callback,
                            final Handler handler) {
        Log.d(TAG, "Starting method execute. Method: " + method.getMethodName() +
                " on host: " + hostInfo.getJsonRpcHttpEndpoint());

        // Launch background thread
        Runnable command = new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                executeThroughOkHttp(method, callback, handler);
            }
        };

        executorService.execute(command);
        //new Thread(command).start();
    }

    /**
     * Sends the JSON RPC request through HTTP (using OkHttp library)
     */
    private <T> void executeThroughOkHttp(final ApiMethod<T> method, final ApiCallback<T> callback,
                                          final Handler handler) {
        OkHttpClient client = getOkHttpClient();
        String jsonRequest = method.toJsonString();

        try {
            Request request = new Request.Builder()
                    .url(hostInfo.getJsonRpcHttpEndpoint())
                    .post(RequestBody.create(MEDIA_TYPE_JSON, jsonRequest))
                    .build();
            Log.d(TAG, "Sending request via OkHttp: " + jsonRequest);
            Response response = sendOkHttpRequest(client, request);
            final T result = method.resultFromJson(parseJsonResponse(handleOkHttpResponse(response)));

            if ((handler != null) && (callback != null)) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(result);
                    }
                });
            }
        } catch (final ApiException e) {
            // Got an error, call error handler
            if ((handler != null) && (callback != null)) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(e.getCode(), e.getMessage());
                    }
                });
            }
        }
    }

    public OkHttpClient getOkHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                    .authenticator(new Authenticator() {
                        @Override
                        public Request authenticate(Route route, Response response) throws IOException {
                            if (TextUtils.isEmpty(hostInfo.getUsername()))
                                return null;

                            String credential = Credentials.basic(hostInfo.getUsername(), hostInfo.getPassword());
                            return response.request().newBuilder().header("Authorization", credential).build();
                        }
                    })
                    .build();
        }
        return httpClient;
    }

    private OkHttpClient getNewOkHttpClientNoKeepAlive() {
        java.lang.System.setProperty("http.keepAlive", "false");
        httpClient = null;
        return getOkHttpClient();
    }

    /**
     * Send an OkHttp POST request
     *
     * @param request Request to send
     * @throws ApiException
     */
    private Response sendOkHttpRequest(final OkHttpClient client, final Request request) throws ApiException {
        try {
            return client.newCall(request).execute();
        } catch (ProtocolException e) {
            Log.w(TAG, "Got a Protocol Exception when trying to send OkHttp request. " +
                    "Trying again without connection pooling to try to circunvent this", e);
            // Hack to circumvent a Protocol Exception that occurs when the server returns bogus Status Line
            // http://forum.kodi.tv/showthread.php?tid=224288
            httpClient = getNewOkHttpClientNoKeepAlive();
            throw new ApiException(ApiException.IO_EXCEPTION_WHILE_SENDING_REQUEST, e);
        } catch (IOException e) {
            Log.w(TAG, "Failed to send OkHttp request.", e);
            throw new ApiException(ApiException.IO_EXCEPTION_WHILE_SENDING_REQUEST, e);
        } catch (RuntimeException e) {
            // Seems like OkHttp throws a RuntimeException when it gets a malformed URL
            Log.w(TAG, "Got a Runtime exception when sending OkHttp request. Probably a malformed URL.", e);
            throw new ApiException(ApiException.IO_EXCEPTION_WHILE_SENDING_REQUEST, e);
        }
    }

    /**
     * Reads the response from the server
     *
     * @param response TcpResponse from OkHttp
     * @return TcpResponse body string
     * @throws ApiException
     */
    private String handleOkHttpResponse(Response response) throws ApiException {
        try {
            // LogUtils.LOGD(TAG, "Reading HTTP response.");
            int responseCode = response.code();

            switch (responseCode) {
                case 200:
                    // All ok, read response
                    String res = response.body().string();
                    response.body().close();
                    Log.d(TAG, "OkHTTP response: " + res);
                    return res;
                case 401:
                    Log.d(TAG, "OkHTTP response read error. Got a 401: " + response);
                    throw new ApiException(ApiException.HTTP_RESPONSE_CODE_UNAUTHORIZED,
                            "Server returned response code: " + response);
                case 404:
                    Log.d(TAG, "OkHTTP response read error. Got a 404: " + response);
                    throw new ApiException(ApiException.HTTP_RESPONSE_CODE_NOT_FOUND,
                            "Server returned response code: " + response);
                default:
                    Log.d(TAG, "OkHTTP response read error. Got: " + response);
                    throw new ApiException(ApiException.HTTP_RESPONSE_CODE_UNKNOWN,
                            "Server returned response code: " + response);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to read OkHTTP response.", e);
            throw new ApiException(ApiException.IO_EXCEPTION_WHILE_READING_RESPONSE, e);
        }
    }

    private ObjectNode parseJsonResponse(String response) throws ApiException {
        // LogUtils.LOGD(TAG, "Parsing JSON response");
        try {
            ObjectNode jsonResponse = (ObjectNode) objectMapper.readTree(response);

            if (jsonResponse.has(ApiMethod.ERROR_NODE)) {
                throw new ApiException(ApiException.API_ERROR, jsonResponse);
            }

            if (!jsonResponse.has(ApiMethod.RESULT_NODE)) {
                // Something strange is going on
                throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST,
                        "Result doesn't contain a result node.");
            }

            return jsonResponse;
        } catch (JsonProcessingException e) {
            Log.w(TAG, "Got an exception while parsing JSON response.", e);
            throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e);
        } catch (IOException e) {
            Log.w(TAG, "Got an exception while parsing JSON response.", e);
            throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e);
        }
    }

    /**
     * Helper class to aggregate a method, callback and handler
     *
     * @param <T>
     */
    private static class MethodCallInfo<T> {
        public final ApiMethod<T> method;
        public final ApiCallback<T> callback;
        public final Handler handler;

        public MethodCallInfo(ApiMethod<T> method, ApiCallback<T> callback, Handler handler) {
            this.method = method;
            this.callback = callback;
            this.handler = handler;
        }
    }

    public interface PlayerNotificationsObserver {
        public void onPlay(Player.OnPlay notification);
        public void onPause(Player.OnPause notification);
        public void onSpeedChanged(Player.OnSpeedChanged notification);
        public void onSeek(Player.OnSeek notification);
        public void onStop(Player.OnStop notification);
    }

    public interface SystemNotificationsObserver {
        public void onQuit(System.OnQuit notification);
        public void onRestart(System.OnRestart notification);
        public void onSleep(System.OnSleep notification);
    }

    public interface InputNotificationsObserver {
        public void onInputRequested(Input.OnInputRequested notification);
    }
}
