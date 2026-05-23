// Frontend catalog data mirrors database/init.sql until catalog endpoints exist.
(function () {
    const games = [
        {
            id: '1',
            releaseYear: 2022,
            name: 'F1 22',
            description: 'Base de setups para la generacion anterior.'
        },
        {
            id: '2',
            releaseYear: 2023,
            name: 'F1 23',
            description: 'Configuraciones para ritmo de carrera y clasificacion.'
        },
        {
            id: '3',
            releaseYear: 2024,
            name: 'F1 24',
            description: 'Setups con ajustes de manejo actuales.'
        },
        {
            id: '4',
            releaseYear: 2025,
            name: 'F1 25',
            description: 'Reglajes para la temporada mas reciente.'
        }
    ];

    const tracks = [
        { id: '1', name: 'Bahrain GP', country: 'Bahrain' },
        { id: '2', name: 'Chinese GP', country: 'China' },
        { id: '3', name: 'Japanese GP', country: 'Japan' },
        { id: '4', name: 'Saudi Arabian GP', country: 'Saudi Arabia' },
        { id: '5', name: 'Imola GP', country: 'Italy' },
        { id: '6', name: 'Australian GP', country: 'Australia' },
        { id: '7', name: 'Azerbaijan GP', country: 'Azerbaijan' },
        { id: '8', name: 'Miami GP', country: 'USA' },
        { id: '9', name: 'Spanish GP', country: 'Spain' },
        { id: '10', name: 'Monaco GP', country: 'Monaco' },
        { id: '11', name: 'United States GP', country: 'USA' },
        { id: '12', name: 'Canadian GP', country: 'Canada' },
        { id: '13', name: 'Austrian GP', country: 'Austria' },
        { id: '14', name: 'British GP', country: 'United Kingdom' },
        { id: '15', name: 'French GP', country: 'France' },
        { id: '16', name: 'Hungarian GP', country: 'Hungary' },
        { id: '17', name: 'Belgian GP', country: 'Belgium' },
        { id: '18', name: 'Dutch GP', country: 'Netherlands' },
        { id: '19', name: 'Italian GP', country: 'Italy' },
        { id: '20', name: 'Mexican GP', country: 'Mexico' },
        { id: '21', name: 'Brazilian GP', country: 'Brazil' },
        { id: '22', name: 'Las Vegas GP', country: 'USA' },
        { id: '23', name: 'Singapore GP', country: 'Singapore' },
        { id: '24', name: 'Qatar GP', country: 'Qatar' },
        { id: '25', name: 'Abu Dhabi GP', country: 'United Arab Emirates' }
    ];

    const teams = [
        { id: '1', name: 'Mercedes' },
        { id: '2', name: 'Red Bull Racing' },
        { id: '3', name: 'Ferrari' },
        { id: '4', name: 'McLaren' },
        { id: '5', name: 'Alpine' },
        { id: '6', name: 'AlphaTauri' },
        { id: '7', name: 'Aston Martin' },
        { id: '8', name: 'Williams' },
        { id: '9', name: 'Alfa Romeo' },
        { id: '10', name: 'Haas' },
        { id: '11', name: 'Sauber' },
        { id: '12', name: 'VCARB' },
        { id: '13', name: 'ApxGp' },
        { id: '14', name: 'Konnersport' },
        { id: '15', name: 'Cadillac' },
        { id: '16', name: 'My team' }
    ];

    const sessions = [
        { id: 'PRACTICE', name: 'Practica' },
        { id: 'QUALIFYING', name: 'Clasificacion' },
        { id: 'RACE', name: 'Carrera' },
        { id: 'TIME_TRIAL', name: 'Contrarreloj' }
    ];

    const controllers = [
        { id: 'GAMEPAD', name: 'Mando' },
        { id: 'WHEEL', name: 'Volante' }
    ];

    const weather = [
        { id: 'dry', value: 'false', name: 'Seco' },
        { id: 'wet', value: 'true', name: 'Mojado' }
    ];

    function findById(items, id) {
        return items.find(item => item.id === String(id));
    }

    function getGameLabel(id) {
        return findById(games, id)?.name || `Juego ${id || '-'}`;
    }

    function getTrackLabel(id) {
        return findById(tracks, id)?.name || `Pista ${id || '-'}`;
    }

    function getTeamLabel(id) {
        return findById(teams, id)?.name || `Equipo ${id || '-'}`;
    }

    function getSessionLabel(id) {
        return findById(sessions, id)?.name || String(id || 'N/A');
    }

    function getControllerLabel(id) {
        return findById(controllers, id)?.name || String(id || 'N/A');
    }

    function getSelectionLabel(gameId, trackId) {
        return `${getGameLabel(gameId)} / ${getTrackLabel(trackId)}`;
    }

    function getWeatherFlag(setup) {
        return setup?.isWetWeather ?? setup?.wetWeather;
    }

    function getWeatherLabelFromFlag(value) {
        if (value === null || value === undefined) {
            return 'N/A';
        }

        return value ? 'Mojado' : 'Seco';
    }

    function getSetupWeatherLabel(setup) {
        return getWeatherLabelFromFlag(getWeatherFlag(setup));
    }

    window.F1Catalog = {
        games,
        tracks,
        teams,
        sessions,
        controllers,
        weather,
        getGameLabel,
        getTrackLabel,
        getTeamLabel,
        getSessionLabel,
        getControllerLabel,
        getSelectionLabel,
        getWeatherFlag,
        getWeatherLabelFromFlag,
        getSetupWeatherLabel
    };
})();
