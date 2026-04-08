package com.f1setups.DTO;

public class SignInRequest
{
    public String username;
    public String email;
    public String password;

    public SignInRequest(String username, String email, String password)
    {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public SignInRequest()
    {
        this.username = "";
        this.email = "";
        this.password = "";
    }
}
