package com.sarahmizzi.fyp.classes;

import com.sarahmizzi.fyp.kodi.jsonrpc.api.Input;

/**
 * Created by Sarah on 17-Feb-16.
 */
public class Transaction {
    String uID;
    String description;

    public Transaction(String uID, String description) {
        this.uID = uID;
        this.description = description;
    }

    public String getuID() {
        return uID;
    }

    public void setuID(String uID) {
        this.uID = uID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
