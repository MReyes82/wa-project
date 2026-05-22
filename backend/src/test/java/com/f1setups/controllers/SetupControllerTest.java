package com.f1setups.controllers;

import com.f1setups.models.ControllerType;
import com.f1setups.models.SessionType;
import com.f1setups.models.Setup;
import com.f1setups.services.AuthService;
import com.f1setups.services.SetupService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SetupControllerTest
{
    @Test
    void defaultRouteCallsSetupServiceAndReturnsJson() throws Exception
    {
        SetupService setupService = mock(SetupService.class);
        AuthService authService = mock(AuthService.class);
        SetupController setupController = new SetupController(setupService, authService);
        HttpExchange httpExchange = createExchange("GET", "/api/setups/default?gameId=4&trackId=19");
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        Setup setup = createSetup(1, 1);

        when(httpExchange.getResponseBody()).thenReturn(responseBody);
        when(setupService.getDefaultSetup(4, 19)).thenReturn(setup);

        setupController.handle(httpExchange);

        verify(setupService).getDefaultSetup(4, 19);
        verify(httpExchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(responseBody.toString().contains("Monza Race Setup"));
    }

    @Test
    void authenticatedRouteWithoutBearerTokenReturnsUnauthorized() throws Exception
    {
        SetupService setupService = mock(SetupService.class);
        AuthService authService = mock(AuthService.class);
        SetupController setupController = new SetupController(setupService, authService);
        HttpExchange httpExchange = createExchange("GET", "/api/setups/me?gameId=4&trackId=19");
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();

        when(httpExchange.getResponseBody()).thenReturn(responseBody);
        when(authService.getAuthenticatedUserId(null)).thenThrow(new Exception("missing token"));

        setupController.handle(httpExchange);

        verify(httpExchange).sendResponseHeaders(eq(401), anyLong());
        verifyNoInteractions(setupService);
        assertTrue(responseBody.toString().contains("Unauthorized"));
    }

    private HttpExchange createExchange(String method, String uri)
    {
        HttpExchange httpExchange = mock(HttpExchange.class);
        Headers requestHeaders = new Headers();
        Headers responseHeaders = new Headers();

        when(httpExchange.getRequestMethod()).thenReturn(method);
        when(httpExchange.getRequestURI()).thenReturn(URI.create(uri));
        when(httpExchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(httpExchange.getResponseHeaders()).thenReturn(responseHeaders);

        return httpExchange;
    }

    private Setup createSetup(int id, int userId)
    {
        return new Setup(
                id,
                userId,
                4,
                19,
                3,
                "Monza Race Setup",
                "Stable rear end",
                SessionType.RACE,
                ControllerType.WHEEL,
                false,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                20,
                25,
                60,
                55,
                40,
                2.5f,
                1.0f,
                0.1f,
                0.2f,
                30,
                20,
                12,
                8,
                25,
                35,
                100,
                56,
                23.0f,
                23.0f,
                21.5f,
                21.5f
        );
    }
}
