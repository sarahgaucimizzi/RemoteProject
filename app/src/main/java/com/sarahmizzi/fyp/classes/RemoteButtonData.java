package com.sarahmizzi.fyp.classes;

/**
 * Created by Sarah on 09-Mar-16.
 */
public class RemoteButtonData extends Transaction{
    int count;

    public RemoteButtonData(String description, int count) {
        super(description);
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementCount(){
        this.count++;
    }
}
