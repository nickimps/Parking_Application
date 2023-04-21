package com.parking.linkandpark;

public class User {
    private String username;
    private String password;
    private String name;
    private String permit;
    private Boolean isAdmin;

    /**
     * Constructor class
     *
     * @param username The username
     * @param password The password
     * @param name The user's name
     * @param permit The user's permit number
     * @param isAdmin If the user is admin or not
     */
    public User(String username, String password, String name, String permit, Boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.permit = permit;
        this.isAdmin = isAdmin;
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
    public Boolean getIsAdmin() { return isAdmin; }
}
