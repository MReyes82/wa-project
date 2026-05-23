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

            showSetupStatus('Cargando setup base...');

            try {
                const setup = await getDefaultSetup(gameId, trackId);
                ui.renderDefaultSetup(setup, gameId, trackId);
            } catch (error) {
                showSetupStatus(error.message, 'Cambiar pista', () => {
                    localStorage.removeItem(storageKeys.trackId);
                    routeUser();
                });
            }
        }

        function initDashboardSetups() {
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

            bindClick('btn-latest-community', () => {
                navigateTo('see-community-setups');
            });

            bindClick('btn-latest-my', () => {
                if (requireAuth()) {
                    navigateTo('see-my-setups');
                }
            });

            loadDefaultSetup();
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

        function openCreateSetup() {
            if (!requireAuth()) {
                return;
            }

            localStorage.setItem(storageKeys.setupSource, setupSources.my);
            localStorage.setItem(storageKeys.setupMode, setupModes.create);
            localStorage.removeItem(storageKeys.selectedSetupId);
            localStorage.removeItem(storageKeys.selectedSetupJson);
            navigateTo('view-setup');
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
                cachedCommunitySetups = ui.asSetupArray(await getCommunitySetups(gameId, trackId));
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
                showSetupForm(createBlankSetup(), mode);
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

            hideViewStatus();
            configureViewActions(mode, setupSources.my, false);
            readMode.hidden = true;
            setupForm.hidden = false;
            saveButton.textContent = mode === setupModes.edit ? 'Guardar cambios' : 'Crear setup';

            populateSetupForm(setup);
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
                payload[fieldName] = parseNumberField(formData, fieldName, Number.parseInt);
            });

            config.decimalSetupFields.forEach(fieldName => {
                payload[fieldName] = parseNumberField(formData, fieldName, Number.parseFloat);
            });

            return payload;
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
            initViewSetupLogic
        };
    }

    window.F1SetupPages = {
        create: createSetupPages
    };
})();
