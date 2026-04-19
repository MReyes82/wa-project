// Wait for the HTML to fully load before attaching listeners
document.addEventListener('DOMContentLoaded', () => {

    // These exist in the root index.html, so it's safe to grab them immediately
    const appRoot = document.getElementById('app-root');
    const dynamicStyleLink = document.getElementById('dynamic-page-style');

    // --- Core of the navigation ---
    async function navigateTo(pageName) {
        try {
            // Swap the css file
            dynamicStyleLink.href = `css/pages/${pageName}.css`;
            // Fetch the raw HTML text from the file
            const response = await fetch(`pages/${pageName}.html`);
            const htmlString = await response.text();
            // Inject the HTML into the page frame
            appRoot.innerHTML = htmlString;

            // Re-attach event listeners based on which page just loaded
            if (pageName === 'auth') {
                initAuthLogic();
            } else if (pageName === 'dashboard') {
                initDashboardLogic();
            }
        } catch (error) {
            console.error("Failed to load page:", error);
            appRoot.innerHTML = "<h2>Error loading module.</h2>";
        }
    }

    // --- Auth guard ---
    function checkAuthState() {
        const token = localStorage.getItem('f1_user_id');
        if (token) {
            navigateTo('dashboard');
        } else {
            navigateTo('auth');
        }
    }

    // --- Page-Specific Logic Initializes ---
    // These must be attached after the HTML is injected to avoid losing old event listeners
    function initAuthLogic() {
        // Grab the elements of the form now, because now they exist in the DOM
        const loginForm = document.getElementById('login-form');
        const registerForm = document.getElementById('register-form');
        const authTitle = document.getElementById('auth-title');
        const authMessage = document.getElementById('auth-message');

        // UI Toggling Logic
        document.getElementById('show-login').addEventListener('click', () => {
            registerForm.style.display = 'none';
            loginForm.style.display = 'block';
            authTitle.textContent = 'Welcome to F1 Setups';
            authMessage.textContent = '';
        });

        document.getElementById('show-register').addEventListener('click', () => {
            loginForm.style.display = 'none';
            registerForm.style.display = 'block';
            authTitle.textContent = 'Join';
            authMessage.textContent = '';
        });

        // Registration Logic
        registerForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            authMessage.textContent = 'Warming up the tires...';
            authMessage.style.color = 'var(--text-muted)';

            const username = document.getElementById('reg-username-input').value;
            const email = document.getElementById('reg-email-input').value;
            const password = document.getElementById('reg-password-input').value;

            try {
                const result = await registerUser(username, email, password);
                authMessage.textContent = "Account created! You can now login.";
                authMessage.style.color = '#4caf50';

                setTimeout(() => {
                    document.getElementById('show-login').click();
                }, 2000);
            } catch (error) {
                authMessage.textContent = error.message;
                authMessage.style.color = 'var(--accent-color)';
            }
        });

        // Login Logic
        loginForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            authMessage.textContent = 'Logging in...';
            authMessage.style.color = 'black';

            const email = document.getElementById('email-input').value;
            const password = document.getElementById('password-input').value;

            try {
                const result = await loginUser(email, password);
                authMessage.textContent = "Login successful!";
                authMessage.style.color = 'green';

                localStorage.setItem('f1_user_id', result.userId);
                checkAuthState();
            } catch (error) {
                authMessage.textContent = error.message;
                authMessage.style.color = 'red';
            }
        });
    }

    function initDashboardLogic()
    {
        // Grab the elements only after dashboard.html is injected
        document.getElementById('logout-button').addEventListener('click', () =>
        {
            localStorage.removeItem('f1_user_id');
            checkAuthState();
        });
    }

    // --- Kick off the app ---
    checkAuthState();
});