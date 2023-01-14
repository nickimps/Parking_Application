package com.example.gpsapp;

import android.provider.Settings;

public class User {

    private String username;
    private String password;
    private String name;
    private String permit;
    private String lastDeviceID;

    public User() {}

    public User(String username, String password, String name, String permit, String lastDeviceID) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.permit = permit;
        this.lastDeviceID = lastDeviceID;
    }

    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getName() {
        return name;
    }

    public String getPermit() {
        return permit;
    }

    public String getLastDeviceID() {
        return lastDeviceID;
    }
}
