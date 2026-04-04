// Wait for the HTML to fully load before attaching listeners
document.addEventListener('DOMContentLoaded', () => 
{
    const loginForm = document.getElementById('login-form');
    const messageDiv = document.getElementById('login-message');

    // Now attach the listener to the form's submit event
    loginForm.addEventListener('submit', async (event) =>
    {
        // We stop the browser from refreshing the page
        event.preventDefault();

        // Crear old messages
        messageDiv.textContent = 'Logging in...';
        messageDiv.style.color = 'black';

        // Get the email and password values from the form
        const email = document.getElementById('email-input').value;
        const password = document.getElementById('password-input').value;
        // try to call the API function to log in the user
        try
        {
            const response = await loginUser(email, password);
            // Handle successful login 
            console.log("Server responded with:", response);
            messageDiv.textContent = "Login successful!";
            messageDiv.style.color = 'green';

        }
        catch (error)
        {
            // Handle auth failure
            messageDiv.textContent = error.message;
            messageDiv.style.color = 'red';
        }
    });
});
