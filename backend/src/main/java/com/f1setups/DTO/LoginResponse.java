package com.f1setups.DTO;

public class LoginResponse
{
    public boolean success;
    public String message;
    public int userId;

    public LoginResponse()
    {
        success = false;
        message = "";
        userId = 0;
    }
    public LoginResponse(boolean success, String message, int usedId)
    {
        this.success = success;
        this.message = message;
        this.userId = usedId;
    }
}
