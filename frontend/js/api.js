// Keep the backend root in one place. The PHP backend serves API routes under /api.
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

function getAuthHeaders()
{
    const token = localStorage.getItem('f1_auth_token');

    if (!token)
    {
        throw new Error('Debes iniciar sesion para administrar tus setups.');
    }

    return {
        'Authorization': `Bearer ${token}`
    };
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
        }, 'No se pudo iniciar sesion'); // Return the LoginResponse object from the server
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
        }, 'No se pudo crear la cuenta');
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
        throw new Error('Selecciona un juego y una pista antes de cargar un setup.');
    }

    // Public route for one default setup.
    const url = new URL(`${API_BASE_URL}/setups/default`);
    url.searchParams.set('gameId', gameId);
    url.searchParams.set('trackId', trackId);

    const setup = await requestJson(url.toString(),
    {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    }, 'No se pudo cargar el setup base');

    if (!setup)
    {
        throw new Error('No se encontro setup base para este juego y pista.');
    }

    return setup;
}

async function getDefaultSetups(gameId, trackId)
{
    if (!gameId || !trackId)
    {
        throw new Error('Selecciona un juego y una pista antes de cargar los setups base.');
    }

    // Public route for every default setup template on the selected game and track.
    const url = new URL(`${API_BASE_URL}/setups/defaults`);
    url.searchParams.set('gameId', gameId);
    url.searchParams.set('trackId', trackId);

    return await requestJson(url.toString(),
    {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    }, 'No se pudieron cargar los setups base');
}

async function getCommunitySetups(gameId, trackId)
{
    if (!gameId || !trackId)
    {
        throw new Error('Selecciona un juego y una pista antes de cargar setups de la comunidad.');
    }

    // Public route for all non-default setups on the selected game and track.
    const url = new URL(`${API_BASE_URL}/setups/community`);
    url.searchParams.set('gameId', gameId);
    url.searchParams.set('trackId', trackId);

    return await requestJson(url.toString(),
    {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    }, 'No se pudieron cargar los setups de la comunidad');
}

async function searchCommunitySetups(gameId, query)
{
    if (!gameId || !query?.trim())
    {
        throw new Error('Ingresa un juego y un nombre de setup para buscar.');
    }

    const url = new URL(`${API_BASE_URL}/setups/community/search`);
    url.searchParams.set('gameId', gameId);
    url.searchParams.set('query', query.trim());

    return await requestJson(url.toString(),
    {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    }, 'No se pudo buscar en los setups de la comunidad');
}

async function getMySetups(gameId, trackId)
{
    if (!gameId || !trackId)
    {
        throw new Error('Selecciona un juego y una pista antes de cargar tus setups.');
    }

    const url = new URL(`${API_BASE_URL}/setups/me`);
    url.searchParams.set('gameId', gameId);
    url.searchParams.set('trackId', trackId);

    return await requestJson(url.toString(),
    {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            ...getAuthHeaders()
        }
    }, 'No se pudieron cargar tus setups');
}

async function searchMySetups(gameId, query)
{
    if (!gameId || !query?.trim())
    {
        throw new Error('Ingresa un juego y un nombre de setup para buscar.');
    }

    const url = new URL(`${API_BASE_URL}/setups/me/search`);
    url.searchParams.set('gameId', gameId);
    url.searchParams.set('query', query.trim());

    return await requestJson(url.toString(),
    {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            ...getAuthHeaders()
        }
    }, 'No se pudo buscar en tus setups');
}

async function getMySetup(setupId)
{
    return await requestJson(`${API_BASE_URL}/setups/me/${setupId}`,
    {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            ...getAuthHeaders()
        }
    }, 'No se pudo cargar tu setup');
}

async function createMySetup(setup)
{
    return await requestJson(`${API_BASE_URL}/setups/me`,
    {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...getAuthHeaders()
        },
        body: JSON.stringify(setup)
    }, 'No se pudo crear el setup');
}

async function updateMySetup(setupId, setup)
{
    return await requestJson(`${API_BASE_URL}/setups/me/${setupId}`,
    {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...getAuthHeaders()
        },
        body: JSON.stringify(setup)
    }, 'No se pudo actualizar el setup');
}

async function deleteMySetup(setupId)
{
    return await requestJson(`${API_BASE_URL}/setups/me/${setupId}`,
    {
        method: 'DELETE',
        headers: {
            ...getAuthHeaders()
        }
    }, 'No se pudo eliminar el setup');
}
