package com.f1setups;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLConnection
{
    public List<User> test()
    {
        List<User> users = new ArrayList<>();
        try
        {
            Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/f1setups", "root", "password");
            System.out.println("[SERVER] Connected to database successfully");

            System.out.println("[SERVER] Fetching users from database...");
            var preparedStatement = c.prepareStatement("SELECT * FROM users");
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet == null)
            {
                return  null;
            }

            while (resultSet.next())
            {
                System.out.println("[SERVER] Adding user" + resultSet.getString("id") + "to list...");
                users.add(new User(resultSet.getInt("id"), resultSet.getString("username"), resultSet.getString("password"), resultSet.getString("email")));
            }

        } catch (SQLException e)
        {
            System.err.println("Connection failed: " + e.getMessage());
            return null;
        }

        return users;
    }
}
