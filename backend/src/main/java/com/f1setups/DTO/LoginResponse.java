package com.f1setups.DTO;

public class LoginResponse
{
    public boolean success;
    public String message;
    public int userId;
    // Bearer token used by authenticated setup routes.
    public String token;

    public LoginResponse()
    {
        success = false;
        message = "";
        userId = 0;
        token = "";
    }
    public LoginResponse(boolean success, String message, int userId)
    {
        this(success, message, userId, "");
    }

    public LoginResponse(boolean success, String message, int userId, String token)
    {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.token = token;
    }
}
