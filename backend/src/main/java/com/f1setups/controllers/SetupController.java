package com.f1setups.controllers;

import com.f1setups.models.Setup;
import com.f1setups.services.SetupService;
import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;

public class SetupController implements HttpHandler
{
    private SetupService setupService;
    private Gson gson;

    public SetupController(SetupService setupService)
    {
        this.setupService = setupService;
        this.gson = new GsonBuilder() // Patch to avoid error when parsing from modern LocalDateTime
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (date, type, context) ->
                        new JsonPrimitive(date.toString())) // Converts it to a standard string like "2026-05-21T14:05:35"
                .create();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException
    {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(httpExchange.getRequestMethod()))
        {
            httpExchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!("GET".equalsIgnoreCase(httpExchange.getRequestMethod())))
        {
            httpExchange.sendResponseHeaders(405, 0);
            httpExchange.close();
            return;
        }
        getDefaultSetups(httpExchange);
        /*String path = httpExchange.getRequestURI().getPath();
        if ("/api/setups".equalsIgnoreCase(path))
        {
            getDefaultSetups(httpExchange);
        }*/

    }

    private void getDefaultSetups(HttpExchange httpExchange) throws IOException
    {
        try
        {
            // call to the service
            List<Setup> defaultSetups = setupService.getDefaultSetup();
            // convert the list to json
            String json = gson.toJson(defaultSetups);
            // send the exchange
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, json.getBytes().length);
            // Log to the output stream
            OutputStream os = httpExchange.getResponseBody();
            os.write(json.getBytes());
            os.close();
        } catch (Exception e)
        {
            // Handle database error
            e.printStackTrace();
            String errorJson = "{'error': 'Failed to retrieve default setups'}";
            httpExchange.sendResponseHeaders(500, errorJson.getBytes().length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(errorJson.getBytes());
            os.close();
        }
    }
}
