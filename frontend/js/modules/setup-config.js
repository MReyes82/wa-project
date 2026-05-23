// Shared setup constants keep localStorage keys and field lists out of page logic.
(function () {
    const storageKeys = {
        gameId: 'f1_game_id',
        trackId: 'f1_track_id',
        userId: 'f1_user_id',
        authToken: 'f1_auth_token',
        setupSource: 'f1_setup_view_source',
        setupMode: 'f1_setup_form_mode',
        selectedSetupId: 'f1_selected_setup_id',
        selectedSetupJson: 'f1_selected_setup_json'
    };

    const setupSources = {
        community: 'community',
        my: 'my'
    };

    const setupModes = {
        read: 'read',
        create: 'create',
        edit: 'edit'
    };

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

    const integerSetupFields = [
        'frontWing',
        'rearWing',
        'diffOnThrottle',
        'diffOffThrottle',
        'engineBraking',
        'frontSuspension',
        'rearSuspension',
        'frontAntiRollBar',
        'rearAntiRollBar',
        'frontRideHeight',
        'rearRideHeight',
        'brakePressure',
        'brakeBias'
    ];

    const decimalSetupFields = [
        'frontCamber',
        'rearCamber',
        'frontToe',
        'rearToe',
        'frontRightPressure',
        'frontLeftPressure',
        'rearRightPressure',
        'rearLeftPressure'
    ];

    const setupFormFields = [
        'title',
        'teamId',
        'sessionType',
        'controllerType',
        'isWetWeather',
        'annotation',
        ...integerSetupFields,
        ...decimalSetupFields
    ];

    const debug = {
        showRawSetupJson: false
    };

    window.F1SetupConfig = {
        storageKeys,
        setupSources,
        setupModes,
        setupGroups,
        integerSetupFields,
        decimalSetupFields,
        setupFormFields,
        debug
    };
})();
