package com.f1setups.controllers;

import com.f1setups.models.Setup;
import com.f1setups.services.AuthService;
import com.f1setups.services.SetupService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SetupController implements HttpHandler
{
    private SetupService setupService;
    private AuthService authService;
    private Gson gson;

    public SetupController(SetupService setupService, AuthService authService)
    {
        this.setupService = setupService;
        this.authService = authService;
        this.gson = new GsonBuilder() // Patch to avoid error when parsing from modern LocalDateTime
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (date, type, context) ->
                        new JsonPrimitive(date.toString())) // Converts it to a standard string like "2026-05-21T14:05:35"
                .create();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException
    {
        addCorsHeaders(httpExchange);

        if ("OPTIONS".equalsIgnoreCase(httpExchange.getRequestMethod()))
        {
            httpExchange.sendResponseHeaders(204, -1);
            return;
        }

        String method = httpExchange.getRequestMethod();
        String path = httpExchange.getRequestURI().getPath();

        try
        {
            // Public default setup list route. It returns qualifying and race defaults for the selected track.
            if ("GET".equalsIgnoreCase(method) && "/api/setups/defaults".equalsIgnoreCase(path))
            {
                getDefaultSetups(httpExchange);
                return;
            }

            // Public default setup route. The plain /api/setups path stays as a temporary compatibility alias.
            if ("GET".equalsIgnoreCase(method) &&
                    ("/api/setups".equalsIgnoreCase(path) || "/api/setups/default".equalsIgnoreCase(path)))
            {
                getDefaultSetup(httpExchange);
                return;
            }

            // Public community setup list for the selected game and track.
            if ("GET".equalsIgnoreCase(method) && "/api/setups/community".equalsIgnoreCase(path))
            {
                getCommunitySetups(httpExchange);
                return;
            }

            // Public community title search for the selected game across every track.
            if ("GET".equalsIgnoreCase(method) && "/api/setups/community/search".equalsIgnoreCase(path))
            {
                searchCommunitySetups(httpExchange);
                return;
            }

            // Authenticated title search for the current user's setups across every track in the selected game.
            if ("GET".equalsIgnoreCase(method) && "/api/setups/me/search".equalsIgnoreCase(path))
            {
                searchUserSetups(httpExchange);
                return;
            }

            // Authenticated collection operations: list mine or create mine.
            if ("/api/setups/me".equalsIgnoreCase(path))
            {
                handleUserSetupCollection(httpExchange, method);
                return;
            }

            // Authenticated single-resource operations: get, update, or delete one of mine.
            if (path.startsWith("/api/setups/me/"))
            {
                handleUserSetupResource(httpExchange, method, path);
                return;
            }

            sendError(httpExchange, 404, "Setup route not found");
        }
        catch (IllegalArgumentException | JsonSyntaxException e)
        {
            sendError(httpExchange, 400, e.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sendError(httpExchange, 500, "Failed to process setup request");
        }
    }

    /**
     * Handles the public default setup endpoint for a selected game and track.
     * @param httpExchange active HTTP exchange
     * @throws IOException if writing the response fails
     */
    private void getDefaultSetup(HttpExchange httpExchange) throws IOException
    {
        try
        {
            Map<String, String> params = parseQueryParams(httpExchange.getRequestURI().getQuery());
            int gameId = getRequiredIntParam(params, "gameId");
            int trackId = getRequiredIntParam(params, "trackId");

            sendJson(httpExchange, 200, setupService.getDefaultSetup(gameId, trackId));
        }
        catch (Exception e)
        {
            sendServiceError(httpExchange, e);
        }
    }

    /**
     * Handles the public default setup list endpoint for a selected game and track.
     * @param httpExchange active HTTP exchange
     * @throws IOException if writing the response fails
     */
    private void getDefaultSetups(HttpExchange httpExchange) throws IOException
    {
        try
        {
            Map<String, String> params = parseQueryParams(httpExchange.getRequestURI().getQuery());
            int gameId = getRequiredIntParam(params, "gameId");
            int trackId = getRequiredIntParam(params, "trackId");

            sendJson(httpExchange, 200, setupService.getDefaultSetups(gameId, trackId));
        }
        catch (Exception e)
        {
            sendServiceError(httpExchange, e);
        }
    }

    /**
     * Handles the public community setup list endpoint for a selected game and track.
     * @param httpExchange active HTTP exchange
     * @throws IOException if writing the response fails
     */
    private void getCommunitySetups(HttpExchange httpExchange) throws IOException
    {
        try
        {
            Map<String, String> params = parseQueryParams(httpExchange.getRequestURI().getQuery());
            int gameId = getRequiredIntParam(params, "gameId");
            int trackId = getRequiredIntParam(params, "trackId");

            sendJson(httpExchange, 200, setupService.getCommunitySetups(gameId, trackId));
        }
        catch (Exception e)
        {
            sendServiceError(httpExchange, e);
        }
    }

    /**
     * Handles public community setup title search for one game across every track.
     * @param httpExchange active HTTP exchange
     * @throws IOException if writing the response fails
     */
    private void searchCommunitySetups(HttpExchange httpExchange) throws IOException
    {
        try
        {
            Map<String, String> params = parseQueryParams(httpExchange.getRequestURI().getQuery());
            int gameId = getRequiredIntParam(params, "gameId");
            String query = getRequiredStringParam(params, "query");

            sendJson(httpExchange, 200, setupService.searchCommunitySetups(gameId, query));
        }
        catch (Exception e)
        {
            sendServiceError(httpExchange, e);
        }
    }

    /**
     * Handles authenticated setup title search for the current user in one game across every track.
     * @param httpExchange active HTTP exchange
     * @throws IOException if writing the response fails
     */
    private void searchUserSetups(HttpExchange httpExchange) throws IOException
    {
        Integer authenticatedUserId = getAuthenticatedUserId(httpExchange);
        if (authenticatedUserId == null)
        {
            return;
        }

        try
        {
            Map<String, String> params = parseQueryParams(httpExchange.getRequestURI().getQuery());
            int gameId = getRequiredIntParam(params, "gameId");
            String query = getRequiredStringParam(params, "query");

            sendJson(httpExchange, 200, setupService.searchUserSetups(authenticatedUserId, gameId, query));
        }
        catch (Exception e)
        {
            sendServiceError(httpExchange, e);
        }
    }

    /**
     * Routes authenticated collection requests under /api/setups/me.
     * GET lists setups owned by the authenticated user, POST creates one.
     * @param httpExchange active HTTP exchange
     * @param method HTTP method from the request
     * @throws IOException if writing the response fails
     */
    private void handleUserSetupCollection(HttpExchange httpExchange, String method) throws IOException
    {
        // The authenticated user id comes from the bearer token, not from query params or JSON.
        Integer authenticatedUserId = getAuthenticatedUserId(httpExchange);
        if (authenticatedUserId == null)
        {
            return;
        }

        try
        {
            if ("GET".equalsIgnoreCase(method))
            {
                Map<String, String> params = parseQueryParams(httpExchange.getRequestURI().getQuery());
                int gameId = getRequiredIntParam(params, "gameId");
                int trackId = getRequiredIntParam(params, "trackId");

                sendJson(httpExchange, 200, setupService.getUserSetups(authenticatedUserId, gameId, trackId));
                return;
            }

            if ("POST".equalsIgnoreCase(method))
            {
                Setup setup = parseSetupRequest(httpExchange);

                sendJson(httpExchange, 201, setupService.createUserSetup(authenticatedUserId, setup));
                return;
            }

            sendError(httpExchange, 405, "Method not allowed for setup collection");
        }
        catch (Exception e)
        {
            sendServiceError(httpExchange, e);
        }
    }

    /**
     * Routes authenticated single-setup requests under /api/setups/me/{setupId}.
     * GET reads, PUT updates, and DELETE removes only setups owned by the authenticated user.
     * @param httpExchange active HTTP exchange
     * @param method HTTP method from the request
     * @param path request path containing the setup id
     * @throws IOException if writing the response fails
     */
    private void handleUserSetupResource(HttpExchange httpExchange, String method, String path) throws IOException
    {
        // Ownership checks happen in SetupService using this authenticated user id.
        Integer authenticatedUserId = getAuthenticatedUserId(httpExchange);
        if (authenticatedUserId == null)
        {
            return;
        }

        try
        {
            int setupId = parseSetupId(path);

            if ("GET".equalsIgnoreCase(method))
            {
                sendJson(httpExchange, 200, setupService.getUserSetup(authenticatedUserId, setupId));
                return;
            }

            if ("PUT".equalsIgnoreCase(method))
            {
                Setup setup = parseSetupRequest(httpExchange);

                sendJson(httpExchange, 200, setupService.updateUserSetup(authenticatedUserId, setupId, setup));
                return;
            }

            if ("DELETE".equalsIgnoreCase(method))
            {
                setupService.deleteUserSetup(authenticatedUserId, setupId);
                httpExchange.sendResponseHeaders(204, -1);
                httpExchange.close();
                return;
            }

            sendError(httpExchange, 405, "Method not allowed for setup resource");
        }
        catch (Exception e)
        {
            sendServiceError(httpExchange, e);
        }
    }

    /**
     * Extracts and validates the bearer token from the request headers.
     * @param httpExchange active HTTP exchange
     * @return authenticated user id, or null after sending a 401 response
     * @throws IOException if writing the 401 response fails
     */
    private Integer getAuthenticatedUserId(HttpExchange httpExchange) throws IOException
    {
        try
        {
            // Expected header shape: Authorization: Bearer <token>
            String authorizationHeader = httpExchange.getRequestHeaders().getFirst("Authorization");
            return authService.getAuthenticatedUserId(authorizationHeader);
        }
        catch (Exception e)
        {
            sendError(httpExchange, 401, "Unauthorized");
            return null;
        }
    }

    /**
     * Parses a setup JSON request body into the existing Setup model.
     * @param httpExchange active HTTP exchange
     * @return parsed setup object
     */
    private Setup parseSetupRequest(HttpExchange httpExchange)
    {
        // Parse POST/PUT request bodies into the existing Setup model.
        InputStream inputStream = httpExchange.getRequestBody();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Setup setup = gson.fromJson(inputStreamReader, Setup.class);

        if (setup == null)
        {
            throw new IllegalArgumentException("Setup request body cannot be empty");
        }

        return setup;
    }

    /**
     * Extracts the setup id from a /api/setups/me/{setupId} path.
     * @param path request path
     * @return parsed setup id
     */
    private int parseSetupId(String path)
    {
        // Extract the id from /api/setups/me/{setupId}.
        String prefix = "/api/setups/me/";
        String setupId = path.substring(prefix.length());

        if (setupId.isBlank() || setupId.contains("/"))
        {
            throw new IllegalArgumentException("Invalid setup id");
        }

        return Integer.parseInt(setupId);
    }

    /**
     * Helper method to turn the query URI into a string map.
     * @param query the raw query string from the URI, e.g. "gameId=1&trackId=2"
     * @return a Map of the query parameters, e.g. {"gameId": "1", "trackId": "2"}
     */
    private Map<String, String> parseQueryParams(String query)
    {
        Map<String, String> result = new HashMap<>();

        if (query == null || query.isBlank())
        {
            return result;
        }

        for (String param : query.split("&"))
        {
            if (param.isBlank())
            {
                continue;
            }

            String[] entry = param.split("=", 2);
            if (entry.length > 1)
            {
                String key = URLDecoder.decode(entry[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(entry[1], StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * Reads a required integer query parameter.
     * @param params parsed query parameter map
     * @param name parameter name
     * @return parsed integer value
     */
    private int getRequiredIntParam(Map<String, String> params, String name)
    {
        String value = params.get(name);
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException("Missing required query parameter: " + name);
        }

        return Integer.parseInt(value);
    }

    /**
     * Reads a required string query parameter.
     * @param params parsed query parameter map
     * @param name parameter name
     * @return trimmed parameter value
     */
    private String getRequiredStringParam(Map<String, String> params, String name)
    {
        String value = params.get(name);
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException("Missing required query parameter: " + name);
        }

        return value.trim();
    }

    /**
     * Maps service/controller exceptions to HTTP status codes and JSON errors.
     * @param httpExchange active HTTP exchange
     * @param e exception raised while handling the request
     * @throws IOException if writing the response fails
     */
    private void sendServiceError(HttpExchange httpExchange, Exception e) throws IOException
    {
        String message = e.getMessage() == null ? "Setup request failed" : e.getMessage();

        // Controller parsing errors are client errors.
        if (e instanceof IllegalArgumentException || e instanceof JsonSyntaxException)
        {
            sendError(httpExchange, 400, message);
            return;
        }

        if (message.toLowerCase().contains("not found"))
        {
            sendError(httpExchange, 404, message);
            return;
        }

        if (message.toLowerCase().contains("must") ||
                message.toLowerCase().contains("cannot") ||
                message.toLowerCase().contains("required") ||
                message.toLowerCase().contains("invalid") ||
                message.toLowerCase().contains("missing"))
        {
            sendError(httpExchange, 400, message);
            return;
        }

        e.printStackTrace();
        sendError(httpExchange, 500, message);
    }

    /**
     * Adds CORS headers required by the browser frontend.
     * @param httpExchange active HTTP exchange
     */
    private void addCorsHeaders(HttpExchange httpExchange)
    {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    /**
     * Sends a JSON response with the provided status code.
     * @param httpExchange active HTTP exchange
     * @param statusCode HTTP response status
     * @param body response body to serialize
     * @throws IOException if writing the response fails
     */
    private void sendJson(HttpExchange httpExchange, int statusCode, Object body) throws IOException
    {
        // All setup responses use JSON, including error bodies.
        String json = gson.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        httpExchange.getResponseHeaders().set("Content-Type", "application/json");
        httpExchange.sendResponseHeaders(statusCode, bytes.length);

        OutputStream os = httpExchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /**
     * Sends a JSON error response.
     * @param httpExchange active HTTP exchange
     * @param statusCode HTTP response status
     * @param message error message for the response body
     * @throws IOException if writing the response fails
     */
    private void sendError(HttpExchange httpExchange, int statusCode, String message) throws IOException
    {
        sendJson(httpExchange, statusCode, Map.of("error", message));
    }
}
