package com.f1setups.models;

public enum SessionType
{
    PRACTICE,
    QUALIFYING,
    RACE,
    TIME_TRIAL;


    public static SessionType fromString(String sessionType)
    {
        // Use switch instead of valueOf
        // To avoid case sensitivity issues and to provide a more straight-forward error message
        // using the exception
        return switch (sessionType)
        {
            case "PRACTICE" -> PRACTICE;
            case "QUALIFYING" -> QUALIFYING;
            case "RACE" -> RACE;
            case "TIME_TRIAL" -> TIME_TRIAL;
            default -> throw new IllegalArgumentException("Invalid session type: " + sessionType);
        };
    }
}
