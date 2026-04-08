package com.f1setups;


import com.f1setups.controllers.AuthController;
import com.f1setups.dao.UserDAO;
import com.f1setups.services.AuthService;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main
{
    public static void main(String[] args) throws IOException {
        var userDAO = new UserDAO();
        var authService = new AuthService(userDAO);
        var authController = new AuthController(authService);

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        server.createContext("/api/auth", authController);
        server.setExecutor(null);
        server.start();
        System.out.println("[Main] HTTP Server started on http://localhost:8080");
        System.out.println("[Main] Login endpoing: POST http://localhost:8080/login");
    }
}
