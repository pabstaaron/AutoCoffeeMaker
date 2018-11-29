package com.example.ndonaldson.beanster.data;

import java.util.HashMap;
/**
 * Created by ndonaldson on 10/2/18.
 */

public class UserData {
    private String username;
    private String password;
    private HashMap<String, RequestData> favorites;

    public UserData(String username, String password){
        this.username = username;
        this.password = password;
        favorites = new HashMap<>();
    }

    //Used for "no current/past user"
    public UserData(){
        username = "";
        password = "";
        favorites = new HashMap<>();
    }

    public void addFavorite(String title, RequestData settings){
        favorites.put(title, settings);
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public  HashMap<String, RequestData> getFavorites(){
        return favorites;
    }
}