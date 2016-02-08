package com.sarahmizzi.fyp.connection;

/**
 * Created by Sarah on 01-Feb-16.
 * Kodi Host Information
 */
public class HostInfo {
    final String TAG = HostInfo.class.getSimpleName();
    public static final int DEFAULT_HTTP_PORT = 8080;
    public static final int DEFAULT_WOL_PORT = 9;
    public static final int DEFAULT_EVENT_SERVER_PORT = 9777;
    private static final String JSON_RPC_ENDPOINT = "/jsonrpc";

    String name;
    String address;
    int port;
    String username;
    String password;

    public HostInfo(String name, String address, int port, String username, String password) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJsonRpcHttpEndpoint() {
        return "http://" + address + ":" + port + JSON_RPC_ENDPOINT;
    }
}
