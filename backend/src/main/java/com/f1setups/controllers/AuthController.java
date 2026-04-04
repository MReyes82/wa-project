package com.f1setups.controllers;

import com.f1setups.models.User;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.f1setups.services.AuthService;
import com.f1setups.DTO.LoginResponse;
import com.f1setups.DTO.LoginRequest;
import com.google.gson.Gson;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.http.HttpResponse;

public class AuthController implements HttpHandler
{
    private AuthService authService;
    private Gson gson;

    public AuthController(AuthService authService)
    {
        this.authService = authService;
        this.gson = new Gson();
    }
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        // only allow POST here since it's login
        if (!("POST".equalsIgnoreCase(httpExchange.getRequestMethod())))
        {
            // Send 405 Method not allowed
            httpExchange.sendResponseHeaders(405, 0);
            httpExchange.close();
            return;
        }

        LoginRequest loginRequest = parseRequest(httpExchange);
        try
        {
            // call to the service
            User loggedUser = authService.authenticate(loginRequest.email, loginRequest.password);
            // If it didn't throw, create the success response object
            var loginResponse = new LoginResponse(true, "Login successful", loggedUser.getId());
            // convert the response to JSON string
            String json = gson.toJson(loginResponse);
            // Now we send 200 OK response and the JSON
            httpExchange.getResponseHeaders().add("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, 0);
            // Log to the output stream
            OutputStream os = httpExchange.getResponseBody();
            os.write(json.getBytes());
            // lastly, close the connection
            os.close();
        }
        catch (Exception e)
        {
            System.err.println("[AuthController] error handling login request: " + e.getMessage());
            var loginResponse = new LoginResponse(false, "Login unsuccessful", -1);
            String json = gson.toJson(loginResponse);
            httpExchange.sendResponseHeaders(401, 0);
            OutputStream os = httpExchange.getResponseBody();
            os.write(json.getBytes());
            os.close();
        }
    }

    public LoginRequest parseRequest(HttpExchange httpExchange)
    {
        // get the stream from the Http body
        InputStream inputStream = httpExchange.getRequestBody();
        // We wrap it into a reader
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        // Call gson to parse it
        var loginRequest = gson.fromJson(inputStreamReader, LoginRequest.class);
        // and we return it
        return loginRequest;
    }
}
