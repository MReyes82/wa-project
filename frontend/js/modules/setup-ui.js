// Rendering helpers for catalogs, setup cards, and setup detail views.
(function () {
    const catalog = window.F1Catalog;
    const config = window.F1SetupConfig;

    function formatValue(value) {
        if (value === null || value === undefined || value === '') {
            return 'N/A';
        }

        if (typeof value === 'boolean') {
            return value ? 'Si' : 'No';
        }

        return String(value);
    }

    function formatEnum(value) {
        return catalog.getSessionLabel(value) !== String(value || 'N/A')
            ? catalog.getSessionLabel(value)
            : catalog.getControllerLabel(value);
    }

    function formatDate(value) {
        if (!value) {
            return 'N/A';
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value);
        }

        return date.toLocaleString('es-MX', {
            dateStyle: 'medium',
            timeStyle: 'short'
        });
    }

    function getSetupId(setup) {
        return Number(setup?.id || setup?.setupId || 0);
    }

    function getSetupTitle(setup) {
        const setupId = getSetupId(setup);
        return setup?.title || (setupId ? `Setup ${setupId}` : 'Setup');
    }

    function asSetupArray(response) {
        return Array.isArray(response) ? response : [];
    }

    function createOption(value, text) {
        const option = document.createElement('option');
        option.value = value;
        option.textContent = text;

        return option;
    }

    function populateSelect(select, items, options = {}) {
        if (!select) {
            return;
        }

        select.textContent = '';

        if (options.emptyLabel) {
            select.appendChild(createOption('', options.emptyLabel));
        }

        items.forEach(item => {
            const value = options.valueProperty === 'value' ? item.value : item.id;
            select.appendChild(createOption(value, item.name));
        });
    }

    function populateSetupFilters(prefix) {
        populateSelect(document.getElementById(`${prefix}-weather-filter`), catalog.weather, {
            emptyLabel: 'Todos'
        });
        populateSelect(document.getElementById(`${prefix}-session-filter`), catalog.sessions, {
            emptyLabel: 'Todas'
        });
        populateSelect(document.getElementById(`${prefix}-controller-filter`), catalog.controllers, {
            emptyLabel: 'Todos'
        });
        populateSelect(document.getElementById(`${prefix}-team-filter`), catalog.teams, {
            emptyLabel: 'Todos'
        });
    }

    function populateSetupFormCatalogs() {
        populateSelect(document.getElementById('setup-team-input'), catalog.teams);
        populateSelect(document.getElementById('setup-session-input'), catalog.sessions);
        populateSelect(document.getElementById('setup-controller-input'), catalog.controllers);
        populateSelect(document.getElementById('setup-weather-input'), catalog.weather, {
            valueProperty: 'value'
        });
    }

    function renderGameOptions(container, onSelect) {
        if (!container) {
            return;
        }

        container.textContent = '';

        catalog.games.forEach(game => {
            const card = document.createElement('article');
            card.className = 'game-card selection-card';

            const title = document.createElement('h3');
            title.textContent = game.name;

            const button = document.createElement('button');
            button.className = 'btn-primary game-select-btn';
            button.type = 'button';
            button.dataset.id = game.id;
            button.textContent = 'Seleccionar';
            button.addEventListener('click', () => onSelect(game.id));

            card.appendChild(title);
            card.appendChild(button);
            container.appendChild(card);
        });
    }

    function renderTrackOptions(container, onSelect) {
        if (!container) {
            return;
        }

        container.textContent = '';

        catalog.tracks.forEach(track => {
            const card = document.createElement('article');
            card.className = 'game-card selection-card track-card';

            const flag = document.createElement('span');
            flag.className = 'track-flag';
            flag.textContent = catalog.getCountryFlag(track.country);

            const title = document.createElement('h3');
            title.textContent = track.name;

            const button = document.createElement('button');
            button.className = 'btn-primary track-select-btn';
            button.type = 'button';
            button.dataset.id = track.id;
            button.textContent = 'Seleccionar';
            button.addEventListener('click', () => onSelect(track.id));

            card.appendChild(flag);
            card.appendChild(title);
            card.appendChild(button);
            container.appendChild(card);
        });
    }

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

    function createReadField(label, value) {
        const field = document.createElement('div');
        field.className = 'read-field';

        const labelElement = document.createElement('span');
        labelElement.textContent = label;

        const valueElement = document.createElement('strong');
        valueElement.textContent = formatValue(value);

        field.appendChild(labelElement);
        field.appendChild(valueElement);

        return field;
    }

    function createReadRow(label, value) {
        const row = document.createElement('div');
        row.className = 'read-row';

        const labelElement = document.createElement('span');
        labelElement.textContent = label;

        const valueElement = document.createElement('strong');
        valueElement.textContent = formatValue(value);

        row.appendChild(labelElement);
        row.appendChild(valueElement);

        return row;
    }

    function createReadRangeRow(label, value, gameId, fieldName) {
        const rule = window.F1SetupRanges?.getFieldRule(gameId, fieldName);

        if (!rule) {
            return createReadRow(label, value);
        }

        if (!rule.available) {
            return null;
        }

        const row = document.createElement('div');
        row.className = 'read-range-row';

        const header = document.createElement('div');
        header.className = 'range-row-header';

        const labelElement = document.createElement('span');
        labelElement.textContent = label;

        const valueElement = document.createElement('strong');
        valueElement.textContent = `${formatValue(value)}${rule.unit || ''}`;

        header.appendChild(labelElement);
        header.appendChild(valueElement);

        const slider = document.createElement('input');
        slider.type = 'range';
        slider.min = rule.min;
        slider.max = rule.max;
        slider.step = rule.step;
        slider.value = window.F1SetupRanges.clampFieldValue(gameId, fieldName, value);
        slider.disabled = true;

        const limits = document.createElement('div');
        limits.className = 'range-limits';

        const min = document.createElement('span');
        min.textContent = `${rule.min}${rule.unit || ''}`;

        const max = document.createElement('span');
        max.textContent = `${rule.max}${rule.unit || ''}`;

        limits.appendChild(min);
        limits.appendChild(max);

        row.appendChild(header);
        row.appendChild(slider);
        row.appendChild(limits);

        return row;
    }

    function createSetupCard(setup, source, actions) {
        const card = document.createElement('article');
        card.className = 'setup-list-card';

        const content = document.createElement('div');

        const chip = document.createElement('span');
        chip.className = 'chip';
        chip.textContent = source === config.setupSources.my ? 'Personal' : 'Comunidad';

        const title = document.createElement('h3');
        title.textContent = getSetupTitle(setup);

        const annotation = document.createElement('p');
        annotation.textContent = setup.annotation || 'Sin notas guardadas.';

        const metadata = document.createElement('div');
        metadata.className = 'setup-list-meta';

        [
            catalog.getTeamLabel(setup.teamId),
            formatEnum(setup.sessionType),
            formatEnum(setup.controllerType),
            catalog.getSetupWeatherLabel(setup),
            formatDate(setup.createdAt)
        ].forEach(value => {
            const item = document.createElement('span');
            item.textContent = value;
            metadata.appendChild(item);
        });

        content.appendChild(chip);
        content.appendChild(title);
        content.appendChild(annotation);
        content.appendChild(metadata);

        const actionContainer = document.createElement('div');
        actionContainer.className = 'setup-list-actions';

        actions.forEach(action => {
            const button = document.createElement('button');
            button.className = action.className;
            button.type = 'button';
            button.textContent = action.label;
            button.addEventListener('click', () => action.handler(setup));
            actionContainer.appendChild(button);
        });

        card.appendChild(content);
        card.appendChild(actionContainer);

        return card;
    }

    function renderSetupCards(setups, source, list, actions) {
        list.textContent = '';

        setups.forEach(setup => {
            list.appendChild(createSetupCard(setup, source, actions));
        });
    }

    function renderDefaultSetup(setup, gameId, trackId) {
        const status = document.getElementById('setup-status');
        const setupView = document.getElementById('setup-view');
        const setupTitle = document.getElementById('setup-title');
        const setupAnnotation = document.getElementById('setup-annotation');
        const setupMeta = document.getElementById('setup-meta');
        const setupGroupsContainer = document.getElementById('setup-groups');
        const setupJson = document.getElementById('setup-json');
        const rawJsonPanel = document.getElementById('raw-json-panel');

        // Flag to toggle debug mode
        status.hidden = true;
        setupView.hidden = false;

        setupTitle.textContent = setup.title || 'Setup base';
        setupAnnotation.textContent = setup.annotation || '';
        setupMeta.textContent = '';
        setupGroupsContainer.textContent = '';

        if (rawJsonPanel && setupJson) {
            rawJsonPanel.hidden = !config.debug.showRawSetupJson;
            setupJson.textContent = config.debug.showRawSetupJson ? JSON.stringify(setup, null, 2) : '';
        }

        [
            ['Juego', catalog.getGameLabel(gameId)],
            ['Pista', catalog.getTrackLabel(trackId)],
            ['Sesion', formatEnum(setup.sessionType)],
            ['Control', formatEnum(setup.controllerType)],
            ['Clima', catalog.getSetupWeatherLabel(setup)],
            ['Creado', formatDate(setup.createdAt)]
        ].forEach(([label, value]) => {
            setupMeta.appendChild(createSetupField(label, value));
        });

        config.setupGroups.forEach(group => {
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

    function renderReadSetup(setup, gameId, trackId) {
        const readMode = document.getElementById('setup-read-mode');
        const setupForm = document.getElementById('setup-form');
        const readTitle = document.getElementById('read-setup-title');
        const readAnnotation = document.getElementById('read-setup-annotation');
        const readMeta = document.getElementById('read-setup-meta');
        const readGroups = document.getElementById('read-setup-groups');

        readMode.hidden = false;
        setupForm.hidden = true;

        readTitle.textContent = getSetupTitle(setup);
        readAnnotation.textContent = setup.annotation || 'Sin notas guardadas.';
        readMeta.textContent = '';
        readGroups.textContent = '';

        [
            ['Juego', catalog.getGameLabel(gameId)],
            ['Pista', catalog.getTrackLabel(trackId)],
            ['Equipo', catalog.getTeamLabel(setup.teamId)],
            ['Sesion', formatEnum(setup.sessionType)],
            ['Control', formatEnum(setup.controllerType)],
            ['Clima', catalog.getSetupWeatherLabel(setup)],
            ['Creado', formatDate(setup.createdAt)]
        ].forEach(([label, value]) => {
            readMeta.appendChild(createReadField(label, value));
        });

        config.setupGroups.forEach(group => {
            const groupElement = document.createElement('article');
            groupElement.className = 'read-group';

            const groupTitle = document.createElement('h3');
            groupTitle.textContent = group.title;
            groupElement.appendChild(groupTitle);

            group.fields.forEach(([label, property]) => {
                const row = createReadRangeRow(label, setup[property], gameId, property);

                if (row) {
                    groupElement.appendChild(row);
                }
            });

            readGroups.appendChild(groupElement);
        });
    }

    window.F1SetupUi = {
        formatValue,
        formatEnum,
        formatDate,
        getSetupId,
        getSetupTitle,
        asSetupArray,
        populateSetupFilters,
        populateSetupFormCatalogs,
        renderGameOptions,
        renderTrackOptions,
        renderSetupCards,
        renderDefaultSetup,
        renderReadSetup
    };
})();
