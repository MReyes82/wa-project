package com.f1setups.services;

import com.f1setups.DTO.*;
import com.f1setups.dao.*;
import com.f1setups.models.User;

import java.util.Optional;

public class AuthService
{
    // instance of the Dao passed in the constructor
    private UserDAO userDAO;

    public AuthService(UserDAO userDAO)
    {
        this.userDAO = userDAO;
    }

    /**
     * Main call for the authentication service, uses the UserDAO instance
     * defined in the class' attributes for the query handling.
     * @param email : email of the login request passed from the frontend
     * @param password : password of the login request passed from the frontend.
     * @return User: oject of the authenticated user only if a successful authentication.
     * @throws Exception :
     */
    public User authenticate(String email,  String password) throws Exception
    {
        Optional<User> user = userDAO.getBy("email", email);
        if (user.isEmpty())
        {
            throw new Exception("[AuthService] Invalid credentials");
        }

        if (user.get().getEmail().equals(email) && user.get().getPassword().equals(password))
        {
            return user.get();
        }

        else
        {
            throw new Exception("[AuthService] Invalid credentials");
        }
    }
}
