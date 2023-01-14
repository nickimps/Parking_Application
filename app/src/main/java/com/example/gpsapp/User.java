package com.example.gpsapp;


public class User {

    private String username;
    private String password;
    private String name;
    private String permit;

    public User(String username, String password, String name, String permit) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.permit = permit;
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
}
