package com.f1setups.DTO;

public class LoginRequest
{
    public String email;
    public String password;

    // empty and full constructors
    public LoginRequest() {}
    public LoginRequest(String email, String password)
    {
        this.email = email;
        this.password = password;
    }
}
