// Wait for the HTML to fully load before attaching listeners
document.addEventListener('DOMContentLoaded', () => 
{
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const authTitle = document.getElementById('auth-title');
    const authMessage = document.getElementById('auth-message')

    // --- UI Toggling Logic ---
    document.getElementById('show-register').addEventListener('click', () =>
    {
        loginForm.style.display = 'none';
        registerForm.style.display = 'block';
        authTitle.textContent = 'Join';
        authMessage.textContent = ''; // Clear errors
    });

    document.getElementById('show-login').addEventListener('click', () =>
    {
        registerForm.style.display = 'none';
        loginForm.style.display = 'block';
        authTitle.textContent = 'Welcome to F1 Setups';
        authMessage.textContent = '';
    });

    // --- Registration Logic ---
    registerForm.addEventListener('submit', async (event) =>
    {
        event.preventDefault();
        authMessage.textContent = 'Warming up the tires...';
        authMessage.style.color = 'var(--text-muted)';

        const username = document.getElementById('reg-username-input').value;
        const email = document.getElementById('reg-email-input').value;
        const password = document.getElementById('reg-password-input').value;

        try {
            // Call your new API function
            const result = await registerUser(username, email, password);

            authMessage.textContent = "Account created! You can now login.";
            authMessage.style.color = '#4caf50'; // Success Green

            // Auto-switch back to the login form after a successful registration
            setTimeout(() => {
                document.getElementById('show-login').click();
            }, 2000);

        } catch (error) {
            authMessage.textContent = error.message;
            authMessage.style.color = 'var(--accent-color)'; // Error Red
        }
    });

    // Login logic
    loginForm.addEventListener('submit', async (event) =>
    {
        // We stop the browser from refreshing the page
        event.preventDefault();

        // Crear old messages
        authMessage.textContent = 'Logging in...';
        authMessage.style.color = 'black';

        // Get the email and password values from the form
        const email = document.getElementById('email-input').value;
        const password = document.getElementById('password-input').value;
        // try to call the API function to log in the user
        try
        {
            const response = await loginUser(email, password);
            // Handle successful login 
            console.log("Server responded with:", response);
            authMessage.textContent = "Login successful!";
            authMessage.style.color = 'green';

        }
        catch (error)
        {
            // Handle auth failure
            authMessage.textContent = error.message;
            authMessage.style.color = 'red';
        }
    });
});
