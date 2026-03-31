package com.f1setups.dao;

import com.f1setups.models.User;
import org.intellij.lang.annotations.Language;

import java.sql.*;

/*
    * This class handles the User CRUD operations.
    * Has the static connection for now
 */

public class UserDAO
{
    private final String URL = "jdbc:mysql://localhost:3306/f1setups";
    private final String USERNAME = "root";
    private final String PASSWORD = "password";

    /**
        CREATE operation for the user implementing prepared statement safety
        using question marks (?) guardrails for query safety
        @param user: User object created by the controller.
        @return : true if rowsAffected > 0 to confirm the operation was successful, false if the connection failed
     */
    public boolean createUser(User user)
    {
        String query = "INSERT INTO users (username,email,password) VALUES (?,?,?)";
        // execution try
        try
        {
            // Prepare the statement using the connection
            Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement ps = con.prepareStatement(query);
            // replace the ? safely
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(1,  user.getPassword());
            // compute rows affected to know if the query was successful
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e)
        {
            System.out.println("[SERVER] Error in creating user at UserDAO.java");
            return false;
        }
    }
    /**
     READ operation for the user implementing prepared statement safety
     using question marks (?) guardrails for query safety
     @param username: provided username that works as unique identifier in the client-side
     according business logic.
     @return : User object or null if the user wasn't found
     */
    public User getUserByUsername(String username)
    {
        // TODO: fix this
        String query = "SELECT * FROM users WHERE username = ?";
        User user = null;
        try
        {
            Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement ps = con.prepareStatement(query);

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            int id = rs.getInt("id");
            String name = rs.getString("username");
            String email = rs.getString("email");
            String password = rs.getString("password");
            user = new User(id, name, email, password);
            return user;

        } catch (SQLException e)
        {
            System.out.println("[SERVER] Error in getting user at UserDAO.java");
            return user;
        }
    }
}
