package com.f1setups.controllers;

import com.f1setups.models.Setup;
import com.f1setups.services.SetupService;
import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        /*String path = httpExchange.getRequestURI().getPath();
        if ("/api/setups".equalsIgnoreCase(path))
        {
            getDefaultSetups(httpExchange);
        }*/
        // Grab the raw query from the URI, we will parse it in the service layer
        String query = httpExchange.getRequestURI().getQuery();
        // Parse into the Map
        Map<String, String> params = parseQueryParams(query);
        String gameVersionIdStr = params.get("gameId");
        String trackIdStr = params.get("trackId");
        // Send bad request error if provided params are invalid
        if (gameVersionIdStr == null || trackIdStr == null)
        {
            String errorJson = "{'error': 'Missing required query parameters: gameId and trackId'}";
            httpExchange.sendResponseHeaders(400, errorJson.getBytes().length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(errorJson.getBytes());
            os.close();
            return;
        }
        var gameId = Integer.parseInt(gameVersionIdStr);
        var trackId = Integer.parseInt(trackIdStr);
        getDefaultSetups(httpExchange, gameId, trackId);
    }

    private void getDefaultSetups(HttpExchange httpExchange, int gameVersionId, int trackId) throws IOException
    {
        try
        {
            // call to the service
            Setup defaultSetup = setupService.getDefaultSetup(gameVersionId, trackId);
            // convert the list to json
            String json = gson.toJson(defaultSetup);
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
    /**
     * Helper method to turn the query URI into a string map
     * @param query the raw query string from the URI, e.g. "gameId=1&trackId=2"
     * @return a Map of the query parameters, e.g. {"gameId": "1", "trackId": "2"}
     */
    private Map<String, String> parseQueryParams(String query)
    {
        Map<String, String> result = new HashMap<>();
        // Check if the passed query is valid
        if (query == null)
        {
            System.err.println("[SetupController] No query parameters provided");
            return result;
        }
        // Split the query in two by the & character, then split each part by the = character to
        // get the key and value, and store them in the result map
        for (String param : query.split("&"))
        {
            String[] entry = param.split("=");
            // Check if the entry is valid, it should have exactly 2 parts, otherwise we ignore it
            if (entry.length > 1)
            {
                // Store it on the map
                result.put(entry[0], entry[1]);
            }
        }
        return result;
    }
}
