package com.f1setups;

public class User
{
    public int id;
    public String username;
    public String password;
    public String email;

    public User(int id, String username, String password, String email)
    {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public  int getId()
    {
        return id;
    }
    public String getUsername()
    {
        return username;
    }
    public String getEmail()
    {
        return email;
    }

    public String toString()
    {
        return "id" + id + "name: " + username + " email: " + email;
    }
}