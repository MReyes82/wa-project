package com.f1setups;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main
{
    void main(String[] args)
    {
        SQLConnection sqlConnection = new SQLConnection();
        var users = sqlConnection.test();

        if (users == null)
        {
            System.err.println("[SERVER] No users in database.");
            return;
        }
        for (var user : users)
        {
            System.out.println(user.toString());
        }
    }
}
