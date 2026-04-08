package com.f1setups.controllers;

import com.f1setups.DTO.SignInRequest;
import com.f1setups.DTO.SignInResponse;
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
    public void handle(HttpExchange httpExchange) throws IOException
    {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(httpExchange.getRequestMethod()))
        {
            // Send a 204 (No Content) status code and immediately return.
            // We don't need to process any data, we just needed to send the headers above.
            httpExchange.sendResponseHeaders(204, -1);
            return;
        }

        // only allow POST here since it's login
        if (!("POST".equalsIgnoreCase(httpExchange.getRequestMethod())))
        {
            // Send 405 Method not allowed
            httpExchange.sendResponseHeaders(405, 0);
            httpExchange.close();
            return;
        }

        // Get the path to know where to route
        String path = httpExchange.getRequestURI().getPath();
        if ("/api/auth/register".equalsIgnoreCase(path))
        {
            register(httpExchange);
        }
        else if ("/api/auth/login".equalsIgnoreCase(path))
        {
            login(httpExchange);
        }
    }

    private void login(HttpExchange httpExchange) throws IOException
    {
        LoginRequest loginRequest = parseLoginRequest(httpExchange);
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

    private void register(HttpExchange httpExchange) throws IOException
    {
        SignInRequest signInRequest = parseSingInRequest(httpExchange);
        try
        {
            User newUser = authService.registerUser(
                    signInRequest.username,
                    signInRequest.email,
                    signInRequest.password);

            var signInResponse = new SignInResponse(true, "User registered successfully", newUser.getId());
            String json = gson.toJson(signInResponse);
            // Send 200 ok response
            httpExchange.getResponseHeaders().add("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, 0);
            // log to the output stream
            OutputStream os = httpExchange.getResponseBody();
            os.write(json.getBytes());
            os.close();
        }
        catch (Exception e)
        {
            System.err.println("[AuthController] error handling register request: " + e.getMessage());
            var signInResponse = new SignInResponse(false, "Register unsuccessful", -1);
            String json = gson.toJson(signInResponse);
            httpExchange.sendResponseHeaders(401, 0);
            OutputStream os = httpExchange.getResponseBody();
            os.write(json.getBytes());
            os.close();
        }
    }

    private LoginRequest parseLoginRequest(HttpExchange httpExchange)
    {
        // get the stream from the Http body
        InputStream inputStream = httpExchange.getRequestBody();
        // We wrap it into a reader
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        // Call gson to parse it and return it
        return gson.fromJson(inputStreamReader, LoginRequest.class);
    }

    private SignInRequest parseSingInRequest(HttpExchange httpExchange)
    {
        InputStream inputStream = httpExchange.getRequestBody();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        return gson.fromJson(inputStreamReader, SignInRequest.class);
    }
}
