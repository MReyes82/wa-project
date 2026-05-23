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
        const token = localStorage.getItem('f1_auth_token');
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
                const selectedGameId = event.currentTarget.getAttribute('data-id');
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
                const selectedTrackId = event.currentTarget.getAttribute('data-id');
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
            authTitle.textContent = 'Iniciar sesion';
            authMessage.textContent = '';
        });

        document.getElementById('show-register').addEventListener('click', () => {
            loginForm.style.display = 'none';
            registerForm.style.display = 'block';
            authTitle.textContent = 'Crear cuenta';
            authMessage.textContent = '';
        });

        // Registration Logic
        registerForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            authMessage.textContent = 'Preparando cuenta...';
            authMessage.style.color = 'var(--text-muted)';

            const username = document.getElementById('reg-username-input').value;
            const email = document.getElementById('reg-email-input').value;
            const password = document.getElementById('reg-password-input').value;

            try {
                const result = await registerUser(username, email, password);
                authMessage.textContent = "Cuenta creada. Ya puedes iniciar sesion.";
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
            authMessage.textContent = 'Iniciando sesion...';
            authMessage.style.color = 'var(--text-muted)';

            const email = document.getElementById('email-input').value;
            const password = document.getElementById('password-input').value;

            try {
                const result = await loginUser(email, password);
                authMessage.textContent = "Sesion iniciada.";
                authMessage.style.color = 'var(--color-success)';

                if (!result.token) {
                    throw new Error('La respuesta de inicio de sesion no incluyo token de autenticacion.');
                }

                localStorage.setItem('f1_user_id', result.userId);
                localStorage.setItem('f1_auth_token', result.token);
                checkAuthState();
            } catch (error) {
                authMessage.textContent = error.message;
                authMessage.style.color = 'red';
            }
        });
    }

    // Friendly names for ids stored during the game and track selection flow.
    const gameLabels = {
        '2': 'F1 22',
        '3': 'F1 23',
        '4': 'F1 24',
        '5': 'F1 25',
    };

    const trackLabels = {
        '1': 'Bahrain GP',
        '19': 'Italian GP'
    };

    // Maps backend Setup JSON properties into dashboard sections.
    const setupGroups = [
        {
            title: 'Aerodinamica',
            fields: [
                ['Ala delantera', 'frontWing'],
                ['Ala trasera', 'rearWing']
            ]
        },
        {
            title: 'Transmision',
            fields: [
                ['Diferencial en aceleracion', 'diffOnThrottle'],
                ['Diferencial en retencion', 'diffOffThrottle'],
                ['Freno motor', 'engineBraking']
            ]
        },
        {
            title: 'Geometria de suspension',
            fields: [
                ['Caida delantera', 'frontCamber'],
                ['Caida trasera', 'rearCamber'],
                ['Convergencia delantera', 'frontToe'],
                ['Convergencia trasera', 'rearToe']
            ]
        },
        {
            title: 'Suspension',
            fields: [
                ['Suspension delantera', 'frontSuspension'],
                ['Suspension trasera', 'rearSuspension'],
                ['Barra delantera', 'frontAntiRollBar'],
                ['Barra trasera', 'rearAntiRollBar'],
                ['Altura delantera', 'frontRideHeight'],
                ['Altura trasera', 'rearRideHeight']
            ]
        },
        {
            title: 'Frenos',
            fields: [
                ['Presion de freno', 'brakePressure'],
                ['Balance de freno', 'brakeBias']
            ]
        },
        {
            title: 'Neumaticos',
            fields: [
                ['Presion delantera derecha', 'frontRightPressure'],
                ['Presion delantera izquierda', 'frontLeftPressure'],
                ['Presion trasera derecha', 'rearRightPressure'],
                ['Presion trasera izquierda', 'rearLeftPressure']
            ]
        }
    ];

    // Normalize empty values before placing them in the dashboard.
    function formatValue(value) {
        if (value === null || value === undefined || value === '') {
            return 'N/A';
        }

        if (typeof value === 'boolean') {
            return value ? 'Si' : 'No';
        }

        return String(value);
    }

    // Convert enum strings like TIME_TRIAL into readable labels.
    function formatEnum(value) {
        if (!value) {
            return 'N/A';
        }

        const labels = {
            PRACTICE: 'Practica',
            QUALIFYING: 'Clasificacion',
            RACE: 'Carrera',
            TIME_TRIAL: 'Contrarreloj',
            GAMEPAD: 'Mando',
            WHEEL: 'Volante'
        };

        return labels[value] || String(value);
    }

    // Build one reusable label/value row for metadata and setup values.
    function createSetupField(label, value) {
        const field = document.createElement('div');
        field.className = 'setup-field';

        const labelElement = document.createElement('span');
        labelElement.textContent = label;

        const valueElement = document.createElement('strong');
        valueElement.textContent = formatValue(value);

        field.appendChild(labelElement);
        field.appendChild(valueElement);

        return field;
    }

    // Show loading and error states in the same dashboard area.
    function showSetupStatus(message, actionLabel, actionHandler) {
        const status = document.getElementById('setup-status');
        const setupView = document.getElementById('setup-view');

        setupView.hidden = true;
        status.hidden = false;
        status.textContent = '';

        const messageElement = document.createElement('p');
        messageElement.textContent = message;
        status.appendChild(messageElement);

        if (actionLabel && actionHandler) {
            const actionButton = document.createElement('button');
            actionButton.className = 'btn-secondary setup-status-action';
            actionButton.textContent = actionLabel;
            actionButton.addEventListener('click', actionHandler);
            status.appendChild(actionButton);
        }
    }

    // Keep the selected game and track visible above the setup details.
    function setSetupContext(gameId, trackId) {
        const context = document.getElementById('setup-context');
        const gameName = gameLabels[gameId] || `Juego ${gameId || '-'}`;
        const trackName = trackLabels[trackId] || `Pista ${trackId || '-'}`;

        context.textContent = `${gameName} / ${trackName}`;
    }

    // Render the raw setup JSON into structured dashboard sections.
    function renderDefaultSetup(setup, gameId, trackId) {
        const status = document.getElementById('setup-status');
        const setupView = document.getElementById('setup-view');
        const setupTitle = document.getElementById('setup-title');
        const setupAnnotation = document.getElementById('setup-annotation');
        const setupMeta = document.getElementById('setup-meta');
        const setupGroupsContainer = document.getElementById('setup-groups');
        const setupJson = document.getElementById('setup-json');

        status.hidden = true;
        setupView.hidden = false;

        // Clear previous dashboard content before rendering a newly loaded setup.
        setupTitle.textContent = setup.title || 'Setup base';
        setupAnnotation.textContent = setup.annotation || '';
        setupMeta.textContent = '';
        setupGroupsContainer.textContent = '';
        setupJson.textContent = JSON.stringify(setup, null, 2);

        const wetWeather = setup.isWetWeather ?? setup.wetWeather;
        const weatherLabel = wetWeather === null || wetWeather === undefined
            ? 'N/A'
            : (wetWeather ? 'Mojado' : 'Seco');
        const metadata = [
            ['Juego', gameLabels[gameId] || `Juego ${gameId}`],
            ['Pista', trackLabels[trackId] || `Pista ${trackId}`],
            ['Sesion', formatEnum(setup.sessionType)],
            ['Control', formatEnum(setup.controllerType)],
            ['Clima', weatherLabel],
            ['Creado', setup.createdAt]
        ];

        metadata.forEach(([label, value]) => {
            setupMeta.appendChild(createSetupField(label, value));
        });

        setupGroups.forEach(group => {
            const groupElement = document.createElement('article');
            groupElement.className = 'setup-group';

            const groupTitle = document.createElement('h3');
            groupTitle.textContent = group.title;
            groupElement.appendChild(groupTitle);

            group.fields.forEach(([label, property]) => {
                groupElement.appendChild(createSetupField(label, setup[property]));
            });

            setupGroupsContainer.appendChild(groupElement);
        });
    }

    // Read the selected ids from localStorage, call api.js, then render the response.
    async function loadDefaultSetup() {
        const gameId = localStorage.getItem('f1_game_id');
        const trackId = localStorage.getItem('f1_track_id');

        setSetupContext(gameId, trackId);

        if (!gameId || !trackId) {
            showSetupStatus('Selecciona un juego y una pista para cargar un setup base.', 'Seleccionar setup', () => {
                navigateTo('game-select');
            });
            return;
        }

        // Show immediate feedback while the backend request is in flight.
        showSetupStatus('Cargando setup base...');

        try {
            const setup = await getDefaultSetup(gameId, trackId);
            renderDefaultSetup(setup, gameId, trackId);
        } catch (error) {
            showSetupStatus(error.message, 'Cambiar pista', () => {
                localStorage.removeItem('f1_track_id');
                routeUser();
            });
        }
    }

    function initDashboardLogic() {
        // Grab the elements only after dashboard.html is injected
        document.getElementById('logout-button').addEventListener('click', () => {
            localStorage.removeItem('f1_user_id');
            localStorage.removeItem('f1_auth_token');
            checkAuthState();
        });

        document.getElementById('btn-change-track').addEventListener('click', () => {
            localStorage.removeItem('f1_track_id');
            routeUser();
        });

        document.getElementById('btn-change-game').addEventListener('click', () => {
            localStorage.removeItem('f1_game_id');
            localStorage.removeItem('f1_track_id');
            navigateTo('game-select');
        });

        // Fetch the default setup as soon as the dashboard page is ready.
        loadDefaultSetup();
    }

    // --- Kick off the app --
    routeUser();
});
