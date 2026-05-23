// Main app shell: routing, auth screens, and catalog selection.
document.addEventListener('DOMContentLoaded', () => {
    const appRoot = document.getElementById('app-root');
    const dynamicStyleLink = document.getElementById('dynamic-page-style');
    const { storageKeys } = window.F1SetupConfig;
    const setupUi = window.F1SetupUi;
    let setupPages;

    async function navigateTo(pageName) {
        try {
            dynamicStyleLink.href = `css/pages/${pageName}.css`;

            const response = await fetch(`pages/${pageName}.html`);
            if (!response.ok) {
                throw new Error(`No se pudo cargar la vista ${pageName}.`);
            }

            appRoot.innerHTML = await response.text();

            const initializers = {
                landing: initLandingLogic,
                auth: initAuthLogic,
                dashboard: initDashboardLogic,
                'game-select': initGameSelectLogic,
                'track-select': initTrackSelectLogic,
                'see-community-setups': setupPages.initCommunitySetupsLogic,
                'see-my-setups': setupPages.initMySetupsLogic,
                'view-setup': setupPages.initViewSetupLogic
            };

            if (initializers[pageName]) {
                initializers[pageName]();
            }
        } catch (error) {
            console.error('Failed to load page:', error);
            appRoot.innerHTML = '<h2>No se pudo cargar la vista.</h2>';
        }
    }

    function bindClick(elementId, handler) {
        const element = document.getElementById(elementId);
        if (element) {
            element.addEventListener('click', handler);
        }
    }

    function getCurrentSelection() {
        return {
            gameId: localStorage.getItem(storageKeys.gameId),
            trackId: localStorage.getItem(storageKeys.trackId)
        };
    }

    function hasAuthToken() {
        return Boolean(localStorage.getItem(storageKeys.authToken));
    }

    function requireAuth() {
        if (hasAuthToken()) {
            return true;
        }

        navigateTo('auth');
        return false;
    }

    function clearSetupViewState() {
        localStorage.removeItem(storageKeys.setupSource);
        localStorage.removeItem(storageKeys.setupMode);
        localStorage.removeItem(storageKeys.selectedSetupId);
        localStorage.removeItem(storageKeys.selectedSetupJson);
    }

    function checkAuthState() {
        if (hasAuthToken()) {
            routeUser();
        } else {
            navigateTo('landing');
        }
    }

    function routeUser() {
        const { gameId, trackId } = getCurrentSelection();

        if (gameId && trackId) {
            navigateTo('dashboard');
        } else if (gameId) {
            navigateTo('track-select');
        } else {
            navigateTo('landing');
        }
    }

    function initLandingLogic() {
        bindClick('btn-browse', () => {
            navigateTo('game-select');
        });

        bindClick('btn-login-route', () => {
            navigateTo('auth');
        });
    }

    function initGameSelectLogic() {
        const container = document.getElementById('game-select-list');

        setupUi.renderGameOptions(container, (selectedGameId) => {
            localStorage.setItem(storageKeys.gameId, selectedGameId);
            localStorage.removeItem(storageKeys.trackId);
            clearSetupViewState();
            routeUser();
        });
    }

    function initTrackSelectLogic() {
        bindClick('btn-back-game', () => {
            localStorage.removeItem(storageKeys.gameId);
            localStorage.removeItem(storageKeys.trackId);
            clearSetupViewState();
            routeUser();
        });

        const container = document.getElementById('track-select-list');
        setupUi.renderTrackOptions(container, (selectedTrackId) => {
            localStorage.setItem(storageKeys.trackId, selectedTrackId);
            clearSetupViewState();
            routeUser();
        });
    }

    function initAuthLogic() {
        const loginForm = document.getElementById('login-form');
        const registerForm = document.getElementById('register-form');
        const authTitle = document.getElementById('auth-title');
        const authMessage = document.getElementById('auth-message');

        bindClick('show-login', () => {
            registerForm.style.display = 'none';
            loginForm.style.display = 'block';
            authTitle.textContent = 'Iniciar sesion';
            authMessage.textContent = '';
        });

        bindClick('show-register', () => {
            loginForm.style.display = 'none';
            registerForm.style.display = 'block';
            authTitle.textContent = 'Crear cuenta';
            authMessage.textContent = '';
        });

        registerForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            authMessage.textContent = 'Preparando cuenta...';
            authMessage.style.color = 'var(--text-muted)';

            const username = document.getElementById('reg-username-input').value;
            const email = document.getElementById('reg-email-input').value;
            const password = document.getElementById('reg-password-input').value;

            try {
                await registerUser(username, email, password);
                authMessage.textContent = 'Cuenta creada. Ya puedes iniciar sesion.';
                authMessage.style.color = 'var(--color-success)';

                setTimeout(() => {
                    document.getElementById('show-login').click();
                }, 2000);
            } catch (error) {
                authMessage.textContent = error.message;
                authMessage.style.color = 'var(--accent-color)';
            }
        });

        loginForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            authMessage.textContent = 'Iniciando sesion...';
            authMessage.style.color = 'var(--text-muted)';

            const email = document.getElementById('email-input').value;
            const password = document.getElementById('password-input').value;

            try {
                const result = await loginUser(email, password);

                if (!result.token) {
                    throw new Error('La respuesta de inicio de sesion no incluyo token de autenticacion.');
                }

                authMessage.textContent = 'Sesion iniciada.';
                authMessage.style.color = 'var(--color-success)';
                localStorage.setItem(storageKeys.userId, result.userId);
                localStorage.setItem(storageKeys.authToken, result.token);
                checkAuthState();
            } catch (error) {
                authMessage.textContent = error.message;
                authMessage.style.color = 'var(--accent-color)';
            }
        });
    }

    function initDashboardLogic() {
        bindClick('logout-button', () => {
            localStorage.removeItem(storageKeys.userId);
            localStorage.removeItem(storageKeys.authToken);
            clearSetupViewState();
            checkAuthState();
        });

        bindClick('btn-change-track', () => {
            localStorage.removeItem(storageKeys.trackId);
            clearSetupViewState();
            routeUser();
        });

        bindClick('btn-change-game', () => {
            localStorage.removeItem(storageKeys.gameId);
            localStorage.removeItem(storageKeys.trackId);
            clearSetupViewState();
            navigateTo('game-select');
        });

        setupPages.initDashboardSetups();
    }

    setupPages = window.F1SetupPages.create({
        bindClick,
        navigateTo,
        routeUser,
        requireAuth,
        getCurrentSelection,
        clearSetupViewState
    });

    routeUser();
});
