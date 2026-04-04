package com.f1setups.DTO;

public class LoginResponse
{
    public boolean success;
    public String message;
    public int usedId;

    public LoginResponse()
    {

    }
    public LoginResponse(boolean success, String message, int usedId)
    {
        this.success = success;
        this.message = message;
        this.usedId = usedId;
    }
}
