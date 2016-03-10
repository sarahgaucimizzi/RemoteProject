package com.sarahmizzi.fyp.classes;

import java.util.ArrayList;

/**
 * Created by Sarah on 10-Mar-16.
 */
public class LayoutLog {
    String layoutType;
    String timestamp;
    ArrayList<String> buttonPosition;

    public LayoutLog(String layoutType, String timestamp, ArrayList<String> buttonPosition) {
        this.layoutType = layoutType;
        this.timestamp = timestamp;
        this.buttonPosition = buttonPosition;
    }

    public String getLayoutType() {
        return layoutType;
    }

    public void setLayoutType(String layoutType) {
        this.layoutType = layoutType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<String> getButtonPosition() {
        return buttonPosition;
    }

    public void setButtonPosition(ArrayList<String> buttonPosition) {
        this.buttonPosition = buttonPosition;
    }
}
