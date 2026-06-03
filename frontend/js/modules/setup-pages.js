// Page controllers for dashboard setup actions, setup lists, and setup detail forms.
(function () {
    const catalog = window.F1Catalog;
    const config = window.F1SetupConfig;
    const ui = window.F1SetupUi;

    function createSetupPages(dependencies) {
        const {
            bindClick,
            navigateTo,
            routeUser,
            requireAuth,
            getCurrentSelection,
            clearSetupViewState
        } = dependencies;

        const { storageKeys, setupModes, setupSources } = config;
        let cachedCommunitySetups = [];
        let cachedMySetups = [];
        const numericSetupFields = [
            ...config.integerSetupFields,
            ...config.decimalSetupFields
        ];

        function getSetupId(setup) {
            return ui.getSetupId(setup);
        }

        function getSelectedSetupId() {
            return Number(localStorage.getItem(storageKeys.selectedSetupId) || 0);
        }

        function storeSelectedSetup(setup, source, mode = setupModes.read) {
            const setupId = getSetupId(setup);

            localStorage.setItem(storageKeys.setupSource, source);
            localStorage.setItem(storageKeys.setupMode, mode);
            localStorage.setItem(storageKeys.selectedSetupJson, JSON.stringify(setup));

            if (setupId > 0) {
                localStorage.setItem(storageKeys.selectedSetupId, String(setupId));
            } else {
                localStorage.removeItem(storageKeys.selectedSetupId);
            }
        }

        function getStoredSelectedSetup() {
            const rawSetup = localStorage.getItem(storageKeys.selectedSetupJson);

            if (!rawSetup) {
                return null;
            }

            try {
                return JSON.parse(rawSetup);
            } catch (error) {
                localStorage.removeItem(storageKeys.selectedSetupJson);
                return null;
            }
        }

        function setSetupContext(gameId, trackId) {
            const context = document.getElementById('setup-context');
            if (context) {
                context.textContent = catalog.getSelectionLabel(gameId, trackId);
            }
        }

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
                actionButton.type = 'button';
                actionButton.textContent = actionLabel;
                actionButton.addEventListener('click', actionHandler);
                status.appendChild(actionButton);
            }
        }

        async function loadDefaultSetup() {
            const { gameId, trackId } = getCurrentSelection();

            setSetupContext(gameId, trackId);

            if (!gameId || !trackId) {
                showSetupStatus('Selecciona un juego y una pista para cargar un setup base.', 'Seleccionar setup', () => {
                    navigateTo('game-select');
                });
                return;
            }

            showSetupStatus('Cargando setups base...');

            try {
                const setups = sortDefaultSetups(ui.asSetupArray(await getDefaultSetups(gameId, trackId)));
                ui.renderDefaultSetups(setups, gameId, trackId, createDefaultSetupActions);
            } catch (error) {
                showSetupStatus(error.message, 'Cambiar pista', () => {
                    localStorage.removeItem(storageKeys.trackId);
                    routeUser();
                });
            }
        }

        function sortDefaultSetups(setups) {
            const sessionOrder = {
                QUALIFYING: 1,
                RACE: 2
            };

            return [...setups].sort((left, right) => {
                const leftOrder = sessionOrder[left.sessionType] || 3;
                const rightOrder = sessionOrder[right.sessionType] || 3;

                return leftOrder - rightOrder;
            });
        }

        function createDefaultSetupActions() {
            return [
                {
                    label: 'Editar',
                    className: 'btn-secondary',
                    handler: (setup) => {
                        if (!localStorage.getItem(storageKeys.authToken)) {
                            window.alert('Debes iniciar sesion para crear un setup desde el setup base.');
                            return;
                        }

                        openCreateSetup(setup);
                    }
                }
            ];
        }

        function initDashboardSetups() {
            bindClick('btn-sidebar-community-setups', () => {
                navigateTo('see-community-setups');
            });

            bindClick('btn-sidebar-my-setups', () => {
                if (requireAuth()) {
                    navigateTo('see-my-setups');
                }
            });

            bindClick('btn-see-community-setups', () => {
                navigateTo('see-community-setups');
            });

            bindClick('btn-see-my-setups', () => {
                if (requireAuth()) {
                    navigateTo('see-my-setups');
                }
            });

            bindClick('btn-new-setup', () => {
                openCreateSetup();
            });

            bindClick('btn-latest-default', () => {
                document.querySelector('.setup-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });

            bindClick('btn-dashboard-my-search', () => {
                if (requireAuth()) {
                    if (saveDashboardSearch('my', 'dashboard-my-search')) {
                        navigateTo('search-results');
                    }
                }
            });

            bindClick('btn-dashboard-community-search', () => {
                if (saveDashboardSearch('community', 'dashboard-community-search')) {
                    navigateTo('search-results');
                }
            });

            loadDefaultSetup();
            loadDashboardLatestUserSetup();
            loadDashboardCommunitySetups();
        }

        function saveDashboardSearch(source, inputId) {
            const query = getValidSearchQuery(document.getElementById(inputId));
            if (!query) {
                return false;
            }

            localStorage.setItem(storageKeys.setupSearchSource, source);
            localStorage.setItem(storageKeys.setupSearchQuery, query);

            return true;
        }

        function getValidSearchQuery(input) {
            const query = input?.value.trim() || '';

            if (query) {
                input?.setCustomValidity('');
                return query;
            }

            if (input) {
                input.setCustomValidity('Ingresa un nombre de setup para buscar.');
                input.reportValidity();
                input.focus();
                input.addEventListener('input', () => {
                    input.setCustomValidity('');
                }, { once: true });
            }

            return '';
        }

        async function loadDashboardLatestUserSetup() {
            const latestCard = document.getElementById('dashboard-latest-user-setup');
            const latestButton = document.getElementById('btn-dashboard-latest-user');
            const { gameId, trackId } = getCurrentSelection();

            if (!latestCard || !latestButton) {
                return;
            }

            if (!localStorage.getItem(storageKeys.authToken)) {
                setDashboardLatestCard(latestCard, latestButton, null, 'Inicia sesion para ver tu ultimo setup.');
                return;
            }

            try {
                const setups = ui.asSetupArray(await getMySetups(gameId, trackId));
                const latestSetup = sortSetupsByDate(setups)[0];

                if (!latestSetup) {
                    setDashboardLatestCard(latestCard, latestButton, null, 'No tienes setups guardados para esta pista.');
                    return;
                }

                setDashboardLatestCard(latestCard, latestButton, latestSetup);
            } catch (error) {
                setDashboardLatestCard(latestCard, latestButton, null, error.message);
            }
        }

        function setDashboardLatestCard(card, button, setup, fallbackMessage = '') {
            const title = card.querySelector('h3');
            const text = card.querySelector('p');

            if (!setup) {
                title.textContent = 'Ultimo setup agregado';
                text.textContent = fallbackMessage;
                button.disabled = true;
                button.onclick = null;
                return;
            }

            title.textContent = ui.getSetupTitle(setup);
            text.textContent = `${ui.formatEnum(setup.sessionType)} / ${catalog.getTrackLabel(setup.trackId)} / ${catalog.getSetupWeatherLabel(setup)}`;
            button.disabled = false;
            button.onclick = () => {
                openSetupRead(setup, setupSources.my);
            };
        }

        async function loadDashboardCommunitySetups() {
            const container = document.getElementById('dashboard-community-setups');
            const { gameId, trackId } = getCurrentSelection();

            if (!container) {
                return;
            }

            container.textContent = '';

            try {
                const setups = sortSetupsByDate(
                    ui.asSetupArray(await getCommunitySetups(gameId, trackId)).map(normalizeSetupResult)
                ).slice(0, 3);

                if (!setups.length) {
                    container.appendChild(createDashboardStatusCard('No hay registros de comunidad para esta pista.'));
                    return;
                }

                setups.forEach(setup => {
                    container.appendChild(createDashboardCommunityCard(setup));
                });
            } catch (error) {
                container.appendChild(createDashboardStatusCard(error.message));
            }
        }

        function createDashboardCommunityCard(setup) {
            const card = document.createElement('article');
            card.className = 'dashboard-card';

            const author = document.createElement('div');
            author.className = 'community-setup-author';

            const emoji = document.createElement('span');
            emoji.textContent = '👤';

            const title = document.createElement('h3');
            title.textContent = getSetupAuthorLabel(setup);

            author.appendChild(emoji);
            author.appendChild(title);

            const details = document.createElement('p');
            details.textContent = `${ui.formatEnum(setup.sessionType)} / ${catalog.getTrackLabel(setup.trackId)} / ${catalog.getSetupWeatherLabel(setup)}`;

            const button = document.createElement('button');
            button.className = 'btn-secondary';
            button.type = 'button';
            button.textContent = 'Ir al setup';
            button.addEventListener('click', () => {
                openSetupRead(setup, setupSources.community);
            });

            card.appendChild(author);
            card.appendChild(details);
            card.appendChild(button);

            return card;
        }

        function getSetupAuthorLabel(setup) {
            return setup.username || setup.authorUsername || setup.author || 'Autor sin nombre';
        }

        function createDashboardStatusCard(message) {
            const card = document.createElement('article');
            card.className = 'dashboard-card-status';
            card.textContent = message;

            return card;
        }

        function sortSetupsByDate(setups) {
            return [...setups].sort((left, right) => {
                const leftTime = new Date(left.createdAt || 0).getTime();
                const rightTime = new Date(right.createdAt || 0).getTime();

                return rightTime - leftTime;
            });
        }

        function setListContext(elementId) {
            const context = document.getElementById(elementId);
            const { gameId, trackId } = getCurrentSelection();

            if (context) {
                context.textContent = catalog.getSelectionLabel(gameId, trackId);
            }
        }

        function showListStatus(statusId, listId, message) {
            const status = document.getElementById(statusId);
            const list = document.getElementById(listId);

            if (list) {
                list.textContent = '';
            }

            if (status) {
                status.hidden = false;
                status.textContent = message;
            }
        }

        function hideListStatus(statusId) {
            const status = document.getElementById(statusId);
            if (status) {
                status.hidden = true;
                status.textContent = '';
            }
        }

        function attachSetupFilters(prefix, renderHandler) {
            ['weather', 'session', 'controller', 'team'].forEach(filterName => {
                const filter = document.getElementById(`${prefix}-${filterName}-filter`);
                if (filter) {
                    filter.addEventListener('change', renderHandler);
                }
            });
        }

        function getSetupFilters(prefix) {
            return {
                weather: document.getElementById(`${prefix}-weather-filter`)?.value || '',
                session: document.getElementById(`${prefix}-session-filter`)?.value || '',
                controller: document.getElementById(`${prefix}-controller-filter`)?.value || '',
                team: document.getElementById(`${prefix}-team-filter`)?.value || ''
            };
        }

        function filterSetups(setups, prefix) {
            const filters = getSetupFilters(prefix);

            return setups.filter(setup => {
                const wetWeather = catalog.getWeatherFlag(setup);
                const weatherValue = wetWeather === null || wetWeather === undefined
                    ? ''
                    : (wetWeather ? 'wet' : 'dry');

                return (!filters.weather || filters.weather === weatherValue)
                    && (!filters.session || filters.session === setup.sessionType)
                    && (!filters.controller || filters.controller === setup.controllerType)
                    && (!filters.team || filters.team === String(setup.teamId));
            });
        }

        function renderSetupCards(setups, source, listId, statusId, emptyMessage, actions) {
            const list = document.getElementById(listId);
            if (!list) {
                return;
            }

            list.textContent = '';

            if (!setups.length) {
                showListStatus(statusId, listId, emptyMessage);
                return;
            }

            hideListStatus(statusId);
            ui.renderSetupCards(setups, source, list, actions);
        }

        function openSetupRead(setup, source) {
            storeSelectedSetup(setup, source);
            navigateTo('view-setup');
        }

        function openSetupEdit(setup) {
            if (!requireAuth()) {
                return;
            }

            storeSelectedSetup(setup, setupSources.my, setupModes.edit);
            navigateTo('view-setup');
        }

        function openCreateSetup(seedSetup = null) {
            if (!requireAuth()) {
                return;
            }

            localStorage.setItem(storageKeys.setupSource, setupSources.my);
            localStorage.setItem(storageKeys.setupMode, setupModes.create);
            localStorage.removeItem(storageKeys.selectedSetupId);

            if (seedSetup) {
                // Default setups are public templates; editing them starts a private create flow with copied values.
                localStorage.setItem(storageKeys.selectedSetupJson, JSON.stringify(createSetupDraft(seedSetup)));
            } else {
                localStorage.removeItem(storageKeys.selectedSetupJson);
            }

            navigateTo('view-setup');
        }

        function createSetupDraft(seedSetup) {
            const draft = createBlankSetup();

            config.setupFormFields.forEach(fieldName => {
                if (seedSetup[fieldName] !== undefined && seedSetup[fieldName] !== null) {
                    draft[fieldName] = seedSetup[fieldName];
                }
            });

            return draft;
        }

        async function deleteUserSetupFromList(setup) {
            const setupId = getSetupId(setup);

            if (!setupId || !window.confirm('Eliminar este setup?')) {
                return;
            }

            showListStatus('my-setups-status', 'my-setups-list', 'Eliminando setup...');

            try {
                await deleteMySetup(setupId);
                await loadMySetups();
            } catch (error) {
                showListStatus('my-setups-status', 'my-setups-list', error.message);
            }
        }

        function initCommunitySetupsLogic() {
            bindClick('btn-community-back-dashboard', () => {
                navigateTo('dashboard');
            });

            setListContext('community-context');
            ui.populateSetupFilters('community');
            attachSetupFilters('community', renderCommunitySetups);
            loadCommunitySetups();
        }

        async function loadCommunitySetups() {
            const { gameId, trackId } = getCurrentSelection();

            if (!gameId || !trackId) {
                showListStatus(
                    'community-setups-status',
                    'community-setups-list',
                    'Selecciona un juego y una pista antes de ver la comunidad.'
                );
                return;
            }

            showListStatus('community-setups-status', 'community-setups-list', 'Cargando registros publicos...');

            try {
                cachedCommunitySetups = ui.asSetupArray(await getCommunitySetups(gameId, trackId)).map(normalizeSetupResult);
                renderCommunitySetups();
            } catch (error) {
                showListStatus('community-setups-status', 'community-setups-list', error.message);
            }
        }

        function renderCommunitySetups() {
            const filteredSetups = filterSetups(cachedCommunitySetups, 'community');
            const emptyMessage = cachedCommunitySetups.length
                ? 'No hay registros publicos con esos filtros.'
                : 'No hay registros publicos para esta seleccion.';

            renderSetupCards(
                filteredSetups,
                setupSources.community,
                'community-setups-list',
                'community-setups-status',
                emptyMessage,
                [
                    {
                        label: 'Ver',
                        className: 'btn-secondary',
                        handler: (setup) => openSetupRead(setup, setupSources.community)
                    }
                ]
            );
        }

        function initMySetupsLogic() {
            if (!requireAuth()) {
                return;
            }

            bindClick('btn-my-back-dashboard', () => {
                navigateTo('dashboard');
            });

            bindClick('btn-create-setup', () => {
                openCreateSetup();
            });

            setListContext('my-setups-context');
            ui.populateSetupFilters('my');
            attachSetupFilters('my', renderMySetups);
            loadMySetups();
        }

        async function loadMySetups() {
            const { gameId, trackId } = getCurrentSelection();

            if (!gameId || !trackId) {
                showListStatus(
                    'my-setups-status',
                    'my-setups-list',
                    'Selecciona un juego y una pista antes de ver tus setups.'
                );
                return;
            }

            showListStatus('my-setups-status', 'my-setups-list', 'Cargando tus setups...');

            try {
                cachedMySetups = ui.asSetupArray(await getMySetups(gameId, trackId));
                renderMySetups();
            } catch (error) {
                showListStatus('my-setups-status', 'my-setups-list', error.message);
            }
        }

        function renderMySetups() {
            const filteredSetups = filterSetups(cachedMySetups, 'my');
            const emptyMessage = cachedMySetups.length
                ? 'No hay setups propios con esos filtros.'
                : 'Todavia no tienes setups guardados para esta seleccion.';

            renderSetupCards(
                filteredSetups,
                setupSources.my,
                'my-setups-list',
                'my-setups-status',
                emptyMessage,
                [
                    {
                        label: 'Ver',
                        className: 'btn-secondary',
                        handler: (setup) => openSetupRead(setup, setupSources.my)
                    },
                    {
                        label: 'Editar',
                        className: 'btn-secondary',
                        handler: openSetupEdit
                    },
                    {
                        label: 'Eliminar',
                        className: 'btn-ghost danger-action',
                        handler: deleteUserSetupFromList
                    }
                ]
            );
        }

        function initSearchResultsLogic() {
            const source = getSearchSource();

            if (source === setupSources.my && !requireAuth()) {
                return;
            }

            bindClick('btn-search-back-dashboard', () => {
                navigateTo('dashboard');
            });

            const form = document.getElementById('search-results-form');
            const input = document.getElementById('search-results-query');

            if (input) {
                input.value = localStorage.getItem(storageKeys.setupSearchQuery) || '';
            }

            if (form) {
                form.addEventListener('submit', (event) => {
                    event.preventDefault();

                    const query = getValidSearchQuery(input);
                    if (query) {
                        saveSearchQuery(query);
                        loadSearchResults();
                    }
                });
            }

            setSearchResultsHeader(source);
            loadSearchResults();
        }

        function getSearchSource() {
            const source = localStorage.getItem(storageKeys.setupSearchSource);

            return source === setupSources.my ? setupSources.my : setupSources.community;
        }

        function setSearchResultsHeader(source) {
            const badge = document.getElementById('search-results-badge');
            const context = document.getElementById('search-results-context');
            const { gameId } = getCurrentSelection();
            const sourceLabel = source === setupSources.my ? 'Mis setups' : 'Comunidad';

            if (badge) {
                badge.textContent = sourceLabel;
            }

            if (context) {
                context.textContent = `${catalog.getGameLabel(gameId)} / busqueda en todas las pistas`;
            }
        }

        function saveSearchQuery(query) {
            localStorage.setItem(storageKeys.setupSearchQuery, query.trim());
        }

        async function loadSearchResults() {
            const source = getSearchSource();
            const { gameId } = getCurrentSelection();
            const query = (localStorage.getItem(storageKeys.setupSearchQuery) || '').trim();

            if (!query) {
                showListStatus(
                    'search-results-status',
                    'search-results-list',
                    'Ingresa un nombre de setup para buscar.'
                );
                return;
            }

            if (!gameId) {
                showListStatus(
                    'search-results-status',
                    'search-results-list',
                    'Selecciona un juego antes de buscar setups.'
                );
                return;
            }

            showListStatus('search-results-status', 'search-results-list', 'Buscando setups...');

            try {
                const response = source === setupSources.my
                    ? await searchMySetups(gameId, query)
                    : await searchCommunitySetups(gameId, query);
                const results = ui.asSetupArray(response).map(normalizeSetupResult);

                renderSearchResults(results, source, 'No hay setups con ese nombre para este juego.');
            } catch (error) {
                showListStatus('search-results-status', 'search-results-list', error.message);
            }
        }

        function normalizeSetupResult(result) {
            const setup = result?.setup || result || {};

            return {
                ...setup,
                username: result?.username || setup.username || ''
            };
        }

        function renderSearchResults(setups, source, emptyMessage) {
            renderSetupCards(
                setups,
                source,
                'search-results-list',
                'search-results-status',
                emptyMessage,
                [
                    {
                        label: 'Ver',
                        className: 'btn-secondary',
                        handler: (setup) => openSetupRead(setup, source)
                    }
                ]
            );
        }

        function initViewSetupLogic() {
            ui.populateSetupFormCatalogs();

            bindClick('btn-view-back', () => {
                goBackFromSetupView();
            });

            bindClick('btn-view-edit', () => {
                localStorage.setItem(storageKeys.setupMode, setupModes.edit);
                loadSetupView();
            });

            bindClick('btn-view-delete', () => {
                deleteSelectedUserSetup();
            });

            bindClick('btn-cancel-setup-form', () => {
                cancelSetupForm();
            });

            document.getElementById('setup-form').addEventListener('submit', handleSetupFormSubmit);

            loadSetupView();
        }

        function setViewStatus(message) {
            const status = document.getElementById('view-setup-status');

            status.hidden = false;
            status.textContent = message;
        }

        function hideViewStatus() {
            const status = document.getElementById('view-setup-status');

            status.hidden = true;
            status.textContent = '';
        }

        function setViewHeader(mode, source) {
            const { gameId, trackId } = getCurrentSelection();
            const modeLabel = document.getElementById('view-setup-mode-label');
            const pageTitle = document.getElementById('view-setup-page-title');
            const context = document.getElementById('view-setup-context');

            const labels = {
                [setupModes.read]: source === setupSources.my ? 'Mis setups' : 'Comunidad',
                [setupModes.create]: 'Nuevo',
                [setupModes.edit]: 'Edicion'
            };

            const titles = {
                [setupModes.read]: 'Detalle del setup',
                [setupModes.create]: 'Crear setup',
                [setupModes.edit]: 'Editar setup'
            };

            modeLabel.textContent = labels[mode] || 'Lectura';
            pageTitle.textContent = titles[mode] || 'Setup';
            context.textContent = catalog.getSelectionLabel(gameId, trackId);
        }

        function configureViewActions(mode, source, hasSetup) {
            const editButton = document.getElementById('btn-view-edit');
            const deleteButton = document.getElementById('btn-view-delete');

            editButton.hidden = !(mode === setupModes.read && source === setupSources.my && hasSetup);
            deleteButton.hidden = !(mode === setupModes.read && source === setupSources.my && hasSetup);
        }

        async function loadSetupView() {
            let mode = localStorage.getItem(storageKeys.setupMode) || setupModes.read;
            const source = localStorage.getItem(storageKeys.setupSource) || setupSources.community;

            if (source !== setupSources.my && mode !== setupModes.read) {
                mode = setupModes.read;
                localStorage.setItem(storageKeys.setupMode, mode);
            }

            if ((mode === setupModes.create || mode === setupModes.edit) && !requireAuth()) {
                return;
            }

            setViewHeader(mode, source);
            configureViewActions(mode, source, false);

            if (mode === setupModes.create) {
                showSetupForm(getStoredSelectedSetup() || createBlankSetup(), mode);
                return;
            }

            setViewStatus('Cargando setup...');

            try {
                const setup = await loadSelectedSetup(source);

                if (mode === setupModes.edit) {
                    showSetupForm(setup, mode);
                } else {
                    hideViewStatus();
                    const { gameId, trackId } = getCurrentSelection();
                    ui.renderReadSetup(setup, gameId, trackId);
                    configureViewActions(mode, source, Boolean(getSetupId(setup)));
                }
            } catch (error) {
                showSetupViewError(error.message);
            }
        }

        async function loadSelectedSetup(source) {
            const selectedSetupId = getSelectedSetupId();

            // Private setups are refetched by id so edit/delete uses the latest server state.
            if (source === setupSources.my && selectedSetupId) {
                const currentMode = localStorage.getItem(storageKeys.setupMode) || setupModes.read;
                const setup = await getMySetup(selectedSetupId);
                storeSelectedSetup(setup, setupSources.my, currentMode);

                return setup;
            }

            const storedSetup = getStoredSelectedSetup();
            if (storedSetup) {
                return storedSetup;
            }

            throw new Error('No hay setup seleccionado.');
        }

        function showSetupViewError(message) {
            document.getElementById('setup-read-mode').hidden = true;
            document.getElementById('setup-form').hidden = true;
            configureViewActions(setupModes.read, setupSources.community, false);
            setViewStatus(message);
        }

        function createBlankSetup() {
            const blankSetup = {
                title: '',
                annotation: '',
                teamId: 1,
                sessionType: 'PRACTICE',
                controllerType: 'GAMEPAD',
                isWetWeather: false
            };

            config.integerSetupFields.forEach(field => {
                blankSetup[field] = 0;
            });

            config.decimalSetupFields.forEach(field => {
                blankSetup[field] = 0;
            });

            return blankSetup;
        }

        function showSetupForm(setup, mode) {
            const readMode = document.getElementById('setup-read-mode');
            const setupForm = document.getElementById('setup-form');
            const saveButton = document.getElementById('btn-save-setup');
            const { gameId } = getCurrentSelection();

            hideViewStatus();
            configureViewActions(mode, setupSources.my, false);
            readMode.hidden = true;
            setupForm.hidden = false;
            saveButton.textContent = mode === setupModes.edit ? 'Guardar cambios' : 'Crear setup';

            populateSetupForm(setup);
            configureRangeInputs(gameId);
        }

        function populateSetupForm(setup) {
            config.setupFormFields.forEach(fieldName => {
                const field = document.querySelector(`[name="${fieldName}"]`);
                if (!field) {
                    return;
                }

                if (fieldName === 'isWetWeather') {
                    field.value = catalog.getWeatherFlag(setup) ? 'true' : 'false';
                } else {
                    field.value = setup[fieldName] ?? '';
                }
            });
        }

        function configureRangeInputs(gameId) {
            const engineBrakingGameId = window.F1SetupRanges?.gameIds?.f1_24 || '3';

            numericSetupFields.forEach(fieldName => {
                const input = document.querySelector(`[name="${fieldName}"]`);
                const rule = window.F1SetupRanges?.getFieldRule(gameId, fieldName);

                if (!input || !rule) {
                    return;
                }

                const inputGroup = input.closest('.input-group');
                if (inputGroup) {
                    inputGroup.dataset.setupField = fieldName;
                }

                if (fieldName === 'engineBraking' && String(gameId) !== engineBrakingGameId) {
                    hideUnavailableRangeField(inputGroup, input, fieldName);
                    return;
                }

                if (!rule.available) {
                    hideUnavailableRangeField(inputGroup, input, fieldName);
                    return;
                }

                showRangeField(inputGroup, input, rule, gameId, fieldName);
            });
        }

        function hideUnavailableRangeField(inputGroup, input, fieldName) {
            if (inputGroup) {
                inputGroup.hidden = true;
            }

            input.type = 'hidden';
            input.required = false;
            input.value = window.F1SetupRanges.getHiddenFieldValue(fieldName);
        }

        function showRangeField(inputGroup, input, rule, gameId, fieldName) {
            const clampedValue = window.F1SetupRanges.clampFieldValue(gameId, fieldName, input.value);

            if (inputGroup) {
                inputGroup.hidden = false;
            }

            input.required = true;
            input.min = rule.min;
            input.max = rule.max;
            input.step = rule.step;
            input.value = clampedValue;

            const slider = ensureRangeSlider(input, rule, fieldName);
            const output = ensureRangeOutput(input, rule);
            const label = inputGroup?.querySelector('label');

            input.type = 'hidden';
            input.required = false;

            if (label) {
                label.htmlFor = slider.id;
            }

            slider.min = rule.min;
            slider.max = rule.max;
            slider.step = rule.step;
            slider.value = clampedValue;
            output.textContent = `${input.value}${rule.unit || ''}`;

            input.oninput = () => {
                syncNumberToSlider(input, slider, output, rule, gameId, fieldName);
            };

            slider.oninput = () => {
                input.value = slider.value;
                output.textContent = `${input.value}${rule.unit || ''}`;
            };

            input.onblur = () => {
                input.value = window.F1SetupRanges.clampFieldValue(gameId, fieldName, input.value);
                slider.value = input.value;
                output.textContent = `${input.value}${rule.unit || ''}`;
            };
        }

        function ensureRangeSlider(input, rule, fieldName) {
            let slider = document.getElementById(`${fieldName}-range`);

            if (!slider) {
                slider = document.createElement('input');
                slider.id = `${fieldName}-range`;
                slider.type = 'range';
                slider.className = 'setup-range-slider';
                input.insertAdjacentElement('afterend', slider);
            }

            slider.setAttribute('aria-label', `${fieldName} slider`);

            return slider;
        }

        function ensureRangeOutput(input, rule) {
            let output = input.parentElement.querySelector('.range-value');

            if (!output) {
                output = document.createElement('span');
                output.className = 'range-value';
                input.insertAdjacentElement('afterend', output);
            }

            ensureRangeLimits(input, rule);

            return output;
        }

        function ensureRangeLimits(input, rule) {
            let limits = input.parentElement.querySelector('.range-limits');

            if (!limits) {
                limits = document.createElement('div');
                limits.className = 'range-limits';

                const min = document.createElement('span');
                min.className = 'range-min';

                const max = document.createElement('span');
                max.className = 'range-max';

                limits.appendChild(min);
                limits.appendChild(max);
                input.parentElement.appendChild(limits);
            }

            limits.querySelector('.range-min').textContent = `${rule.min}${rule.unit || ''}`;
            limits.querySelector('.range-max').textContent = `${rule.max}${rule.unit || ''}`;
        }

        function syncNumberToSlider(input, slider, output, rule, gameId, fieldName) {
            const value = input.value === ''
                ? rule.min
                : window.F1SetupRanges.clampFieldValue(gameId, fieldName, input.value);

            slider.value = value;
            output.textContent = `${input.value || value}${rule.unit || ''}`;
        }

        function parseNumberField(formData, fieldName, parser) {
            const value = parser(formData.get(fieldName));

            if (Number.isNaN(value)) {
                throw new Error(`El campo ${fieldName} debe ser numerico.`);
            }

            return value;
        }

        function getSetupPayloadFromForm() {
            const { gameId, trackId } = getCurrentSelection();

            if (!gameId || !trackId) {
                throw new Error('Selecciona un juego y una pista antes de guardar.');
            }

            const formData = new FormData(document.getElementById('setup-form'));
            const title = String(formData.get('title') || '').trim();

            if (!title) {
                throw new Error('El titulo del setup es obligatorio.');
            }

            const payload = {
                id: getSelectedSetupId(),
                userId: Number(localStorage.getItem(storageKeys.userId) || 0),
                gameVersionId: Number(gameId),
                trackId: Number(trackId),
                teamId: parseNumberField(formData, 'teamId', Number.parseInt),
                title: title,
                annotation: String(formData.get('annotation') || '').trim(),
                sessionType: formData.get('sessionType'),
                controllerType: formData.get('controllerType'),
                isWetWeather: formData.get('isWetWeather') === 'true'
            };

            config.integerSetupFields.forEach(fieldName => {
                payload[fieldName] = getNumericPayloadValue(formData, gameId, fieldName, Number.parseInt);
            });

            config.decimalSetupFields.forEach(fieldName => {
                payload[fieldName] = getNumericPayloadValue(formData, gameId, fieldName, Number.parseFloat);
            });

            return payload;
        }

        function getNumericPayloadValue(formData, gameId, fieldName, parser) {
            const rule = window.F1SetupRanges?.getFieldRule(gameId, fieldName);

            if (rule && !rule.available) {
                return window.F1SetupRanges.getHiddenFieldValue(fieldName);
            }

            const value = parseNumberField(formData, fieldName, parser);

            return rule
                ? window.F1SetupRanges.clampFieldValue(gameId, fieldName, value)
                : value;
        }

        async function handleSetupFormSubmit(event) {
            event.preventDefault();

            if (!requireAuth()) {
                return;
            }

            const mode = localStorage.getItem(storageKeys.setupMode) || setupModes.create;
            const saveButton = document.getElementById('btn-save-setup');

            try {
                saveButton.disabled = true;
                setViewStatus('Guardando setup...');

                const payload = getSetupPayloadFromForm();
                if (mode === setupModes.edit && !getSelectedSetupId()) {
                    throw new Error('No hay setup seleccionado para editar.');
                }

                const savedSetup = mode === setupModes.edit
                    ? await updateMySetup(getSelectedSetupId(), payload)
                    : await createMySetup(payload);

                storeSelectedSetup(savedSetup, setupSources.my);
                localStorage.setItem(storageKeys.setupMode, setupModes.read);
                setViewHeader(setupModes.read, setupSources.my);
                hideViewStatus();

                const { gameId, trackId } = getCurrentSelection();
                ui.renderReadSetup(savedSetup, gameId, trackId);
                configureViewActions(setupModes.read, setupSources.my, Boolean(getSetupId(savedSetup)));
            } catch (error) {
                setViewStatus(error.message);
            } finally {
                saveButton.disabled = false;
            }
        }

        function cancelSetupForm() {
            const mode = localStorage.getItem(storageKeys.setupMode);

            if (mode === setupModes.edit) {
                localStorage.setItem(storageKeys.setupMode, setupModes.read);
                loadSetupView();
            } else {
                goBackFromSetupView();
            }
        }

        async function deleteSelectedUserSetup() {
            const setupId = getSelectedSetupId();

            if (!setupId || !window.confirm('Eliminar este setup?')) {
                return;
            }

            try {
                setViewStatus('Eliminando setup...');
                await deleteMySetup(setupId);
                clearSetupViewState();
                navigateTo('see-my-setups');
            } catch (error) {
                setViewStatus(error.message);
            }
        }

        function goBackFromSetupView() {
            const source = localStorage.getItem(storageKeys.setupSource);

            if (source === setupSources.community) {
                navigateTo('see-community-setups');
            } else if (source === setupSources.my) {
                navigateTo('see-my-setups');
            } else {
                navigateTo('dashboard');
            }
        }

        return {
            initDashboardSetups,
            initCommunitySetupsLogic,
            initMySetupsLogic,
            initSearchResultsLogic,
            initViewSetupLogic
        };
    }

    window.F1SetupPages = {
        create: createSetupPages
    };
})();
