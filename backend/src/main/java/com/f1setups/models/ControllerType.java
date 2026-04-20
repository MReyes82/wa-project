package com.f1setups.models;

public enum ControllerType
{
    GAMEPAD,
    WHEEL;

    public static ControllerType fromString(String str)
    {
        // Use switch instead of valueOf
        // To avoid case sensitivity issues and to provide a more straight-forward error message
        // using the exception
        return switch (str)
        {
            case "GAMEPAD" -> GAMEPAD;
            case "WHEEL" -> WHEEL;
            default -> throw new IllegalArgumentException("Controller type not found: " + str);
        };
    }
}
