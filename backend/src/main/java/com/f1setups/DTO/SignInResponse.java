package com.f1setups.DTO;

public class SignInResponse
{
    public  boolean success;
    public String message;
    public int userId;

    public SignInResponse(boolean success, String message, int userId)
    {
        this.success = success;
        this.message = message;
        this.userId = userId;
    }

    public SignInResponse()
    {
        this.success = false;
        this.message = "";
        this.userId = -1;
    }
}
