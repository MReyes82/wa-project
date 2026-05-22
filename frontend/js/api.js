// Keep the backend root in one place. Main.java starts the server at localhost:8080
// and registers the API contexts under /api.
const API_BASE_URL = 'http://localhost:8080/api';

// Read the response body once and parse it as JSON when possible.
// Some backend errors may return plain text, so fallback to a message object.
async function parseJsonResponse(response)
{
    const text = await response.text();

    if (!text)
    {
        return null;
    }

    try
    {
        return JSON.parse(text);
    }
    catch (error)
    {
        return { message: text };
    }
}

// Shared fetch helper for JSON endpoints. It keeps status/error handling
// consistent across auth calls and the setup retrieval call.
async function requestJson(url, options, fallbackMessage)
{
    try
    {
        const response = await fetch(url, options);
        const data = await parseJsonResponse(response);

        if (!response.ok)
        {
            throw new Error(data?.message || data?.error || fallbackMessage);
        }

        return data;
    }
    catch (error)
    {
        console.error("Network or server error:", error);
        throw error;
    }
}

async function loginUser(email, password)
{
    // Prepare the payload for the LoginRequest.java at backend
    const payload = 
    {
        email: email,
        password: password
    }

    try
    {
        // Create the fetch call
        return await requestJson(`${API_BASE_URL}/auth/login`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json' // Set the content type to JSON so the server expects JSON data
            },
            body: JSON.stringify(payload) // Convert the payload object to a JSON string
        }, 'Login failed'); // Return the LoginResponse object from the server
    }
    catch (error)
    {
        throw error; // Re-throw the error to be handled by the caller
    }
}
async function registerUser(username, email, password)
{
    const payload =
    {
        username: username,
        email: email,
        password: password,
    }

    try {
        return await requestJson(`${API_BASE_URL}/auth/register`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        }, 'Registration failed');
    }
    catch (error)
    {
        throw error;
    }
}

async function getDefaultSetup(gameId, trackId)
{
    // The setup endpoint requires both selected ids as query parameters.
    if (!gameId || !trackId)
    {
        throw new Error('Select a game and track before loading a setup.');
    }

    // Matches SetupController behind Main.java's /api/setups context.
    const url = new URL(`${API_BASE_URL}/setups`);
    url.searchParams.set('gameId', gameId);
    url.searchParams.set('trackId', trackId);

    const setup = await requestJson(url.toString(),
    {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    }, 'Failed to retrieve default setup');

    if (!setup)
    {
        throw new Error('No default setup found for this game and track.');
    }

    return setup;
}
