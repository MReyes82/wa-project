package com.f1setups;


import com.f1setups.controllers.AuthController;
import com.f1setups.controllers.SetupController;
import com.f1setups.dao.DatabaseUtil;
import com.f1setups.dao.SetupDao;
import com.f1setups.dao.UserDAO;
import com.f1setups.services.AuthService;
import com.f1setups.services.SetupService;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main
{
    public static void main(String[] args) throws IOException
    {
        var userDAO = new UserDAO();
        var setupDAO = new SetupDao();
        var authService = new AuthService(userDAO);
        var authController = new AuthController(authService);
        var setupService = new SetupService(setupDAO);
        var setupController = new SetupController(setupService);

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        server.createContext("/api/auth", authController);
        server.createContext("/api/setups", setupController);
        server.setExecutor(null);
        server.start();
        System.out.println("[Main] HTTP Server started on http://localhost:8080");
        System.out.println("[Main] Login endpoint: POST http://localhost:8080/api/auth");

        // shutdown the connection pool
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseUtil.closePool();
            System.out.println("[Main] Shutting down connection");
        }));
    }
}
