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
            // and return the correct initialization function
            if (pageName === "landing")
                initLandingLogic()
            else if (pageName === "game-select")
                initGameSelectLogic()
            else if (pageName === "track-select")
                initTrackSelectLogic()
            else if (pageName === "dashboard")
                initDashboardLogic();
            else if (pageName === "auth")
                initAuthLogic();

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
            navigateTo('landing');
        }
    }

    // --- STATE MACHINE ROUTER ---
    function routeUser() {
        const gameId = localStorage.getItem('f1_game_id');
        const trackId = localStorage.getItem('f1_track_id');
        const userId = localStorage.getItem('f1_user_id');

        // If they have everything selected, show the setups
        if (gameId && trackId) {
            navigateTo('dashboard');
        }
        // If they have a game but no track, show tracks
        else if (gameId) {
            navigateTo('track-select');
        }
        // Otherwise, show the landing page
        else {
            navigateTo('landing');
        }
    }

    // --- Page-Specific Logic Initializes ---
    // These must be attached after the HTML is injected to avoid losing old event listeners
    function initLandingLogic() {
        document.getElementById('btn-browse').addEventListener('click', () => {
            navigateTo('game-select');
        });
        document.getElementById('btn-login-route').addEventListener('click', () => {
            navigateTo('auth');
        });
    }

    function initGameSelectLogic() {
        const buttons = document.querySelectorAll('.game-select-btn');
        buttons.forEach(btn => {
            btn.addEventListener('click', (event) => {
                // Extract the DB ID from the HTML attribute
                const selectedGameId = event.target.getAttribute('data-id');
                // Save it to memory
                localStorage.setItem('f1_game_id', selectedGameId);
                // Re-run the router (it will see the game and push them to track-select)
                routeUser();
            });
        });
    }

    function initTrackSelectLogic() {
        // Back button to change game
        document.getElementById('btn-back-game').addEventListener('click', () => {
            localStorage.removeItem('f1_game_id'); // Clear the state
            routeUser(); // Router kicks them back to game-select
        });

        const buttons = document.querySelectorAll('.track-select-btn');
        buttons.forEach(btn => {
            btn.addEventListener('click', (event) => {
                const selectedTrackId = event.target.getAttribute('data-id');
                localStorage.setItem('f1_track_id', selectedTrackId);
                routeUser(); // Router kicks them to the dashboard!
            });
        });
    }

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

    function initDashboardLogic() {
        // Grab the elements only after dashboard.html is injected
        document.getElementById('logout-button').addEventListener('click', () => {
            localStorage.removeItem('f1_user_id');
            checkAuthState();
        });
        // Easy way to allow changing tracks from the dashboard
        // Add a button with id 'btn-change-track' to the dashboard.html sidebar to use this feature
        const changeTrackBtn = document.getElementById('btn-change-track');
        if (changeTrackBtn) {
            changeTrackBtn.addEventListener('click', () => {
                localStorage.removeItem('f1_track_id');
                routeUser();
            });
        }
    }

    // --- Kick off the app --
    routeUser();
});