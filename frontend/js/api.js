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
        const response = await fetch('http://localhost:8080/login',
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json' // Set the content type to JSON so the server expects JSON data
                },
                body: JSON.stringify(payload) // Convert the payload object to a JSON string
            });

        // Parse the JSON coming back from the server
        const data = await response.json();
        // Check the HTTP status code to determine if the login was successful
        if (!response.ok)
        {
            // If the response is not ok, throw an error with the message from the server
            throw new Error(data.message || 'Login failed');
        }
        return data; // Return the LoginResponse object from the server
    }
    catch (error)
    {
        console.error("Network or server error:", error);
        throw error; // Re-throw the error to be handled by the caller
    }
}