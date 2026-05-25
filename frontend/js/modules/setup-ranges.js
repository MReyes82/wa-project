// Numeric setup limits by game version. Game ids mirror database/init.sql.
(function () {
    const gameIds = {
        f1_22: '1',
        f1_23: '2',
        f1_24: '3',
        f1_25: '4'
    };

    const integer = 'integer';
    const decimal = 'decimal';

    const commonRules = {
        frontWing: range(0, 50, 1, integer),
        rearWing: range(0, 50, 1, integer),
        brakePressure: range(80, 100, 1, integer),
        brakeBias: range(50, 70, 1, integer, '%')
    };

    const unavailableRules = {
        engineBraking: {
            ...range(0, 100, 1, integer, '%'),
            available: false,
            hiddenValue: 0
        }
    };

    const rulesByGame = {
        [gameIds.f1_22]: {
            diffOnThrottle: range(50, 100, 1, integer, '%'),
            diffOffThrottle: range(50, 100, 1, integer, '%'),
            frontCamber: range(-3.5, -2.5, 0.1, decimal),
            rearCamber: range(-2.0, -1.0, 0.1, decimal),
            frontToe: range(0.05, 0.15, 0.01, decimal),
            rearToe: range(0.20, 0.50, 0.01, decimal),
            frontSuspension: range(1, 11, 1, integer),
            rearSuspension: range(1, 11, 1, integer),
            frontAntiRollBar: range(1, 11, 1, integer),
            rearAntiRollBar: range(1, 11, 1, integer),
            frontRideHeight: range(1, 11, 1, integer),
            rearRideHeight: range(1, 11, 1, integer),
            frontRightPressure: range(22.5, 25.0, 0.1, decimal),
            frontLeftPressure: range(22.5, 25.0, 0.1, decimal),
            rearRightPressure: range(20.5, 23.0, 0.1, decimal),
            rearLeftPressure: range(20.5, 23.0, 0.1, decimal)
        },
        [gameIds.f1_23]: {
            diffOnThrottle: range(50, 100, 1, integer, '%'),
            diffOffThrottle: range(50, 100, 1, integer, '%'),
            frontCamber: range(-3.5, -2.5, 0.1, decimal),
            rearCamber: range(-2.0, -1.0, 0.1, decimal),
            frontToe: range(0.00, 0.10, 0.01, decimal),
            rearToe: range(0.10, 0.30, 0.01, decimal),
            frontSuspension: range(1, 41, 1, integer),
            rearSuspension: range(1, 41, 1, integer),
            frontAntiRollBar: range(1, 21, 1, integer),
            rearAntiRollBar: range(1, 21, 1, integer),
            frontRideHeight: range(30, 50, 1, integer),
            rearRideHeight: range(30, 50, 1, integer),
            frontRightPressure: range(22.0, 25.0, 0.1, decimal),
            frontLeftPressure: range(22.0, 25.0, 0.1, decimal),
            rearRightPressure: range(20.0, 23.0, 0.1, decimal),
            rearLeftPressure: range(20.0, 23.0, 0.1, decimal)
        },
        [gameIds.f1_24]: {
            diffOnThrottle: range(10, 100, 1, integer, '%'),
            diffOffThrottle: range(10, 100, 1, integer, '%'),
            engineBraking: range(0, 100, 1, integer, '%'),
            frontCamber: range(-3.5, -2.5, 0.1, decimal),
            rearCamber: range(-2.2, -0.70, 0.1, decimal),
            frontToe: range(0.00, 0.50, 0.01, decimal),
            rearToe: range(0.00, 0.50, 0.01, decimal),
            frontSuspension: range(1, 41, 1, integer),
            rearSuspension: range(1, 41, 1, integer),
            frontAntiRollBar: range(1, 21, 1, integer),
            rearAntiRollBar: range(1, 21, 1, integer),
            frontRideHeight: range(10, 40, 1, integer),
            rearRideHeight: range(40, 100, 1, integer),
            frontRightPressure: range(22.5, 29.5, 0.1, decimal),
            frontLeftPressure: range(22.5, 29.5, 0.1, decimal),
            rearRightPressure: range(20.5, 26.5, 0.1, decimal),
            rearLeftPressure: range(20.5, 26.5, 0.1, decimal)
        },
        [gameIds.f1_25]: {
            diffOnThrottle: range(10, 100, 1, integer, '%'),
            diffOffThrottle: range(10, 100, 1, integer, '%'),
            frontCamber: range(-3.5, -2.5, 0.1, decimal),
            rearCamber: range(-2.0, -1.0, 0.1, decimal),
            frontToe: range(0.00, 0.20, 0.01, decimal),
            rearToe: range(0.10, 0.25, 0.01, decimal),
            frontSuspension: range(1, 41, 1, integer),
            rearSuspension: range(1, 41, 1, integer),
            frontAntiRollBar: range(1, 21, 1, integer),
            rearAntiRollBar: range(1, 21, 1, integer),
            frontRideHeight: range(15, 35, 1, integer),
            rearRideHeight: range(40, 50, 1, integer),
            frontRightPressure: range(22.5, 29.5, 0.1, decimal),
            frontLeftPressure: range(22.5, 29.5, 0.1, decimal),
            rearRightPressure: range(20.5, 26.5, 0.1, decimal),
            rearLeftPressure: range(20.5, 26.5, 0.1, decimal)
        }
    };

    function range(min, max, step, type, unit = '') {
        return {
            min,
            max,
            step,
            type,
            unit,
            available: true
        };
    }

    function normalizeGameId(gameId) {
        return String(gameId || gameIds.f1_25);
    }

    function getFieldRule(gameId, fieldName) {
        const normalizedGameId = normalizeGameId(gameId);
        const gameRule = rulesByGame[normalizedGameId]?.[fieldName];
        const commonRule = commonRules[fieldName];

        if (gameRule || commonRule) {
            return {
                fieldName,
                ...(commonRule || {}),
                ...(gameRule || {}),
                available: true
            };
        }

        if (unavailableRules[fieldName]) {
            return {
                fieldName,
                ...unavailableRules[fieldName]
            };
        }

        return null;
    }

    function isFieldAvailable(gameId, fieldName) {
        const rule = getFieldRule(gameId, fieldName);

        return Boolean(rule?.available);
    }

    function getHiddenFieldValue(fieldName) {
        return unavailableRules[fieldName]?.hiddenValue ?? 0;
    }

    function clampFieldValue(gameId, fieldName, value) {
        const rule = getFieldRule(gameId, fieldName);

        if (!rule) {
            return value;
        }

        if (!rule.available) {
            return getHiddenFieldValue(fieldName);
        }

        const numericValue = Number(value);
        if (Number.isNaN(numericValue)) {
            return rule.min;
        }

        const clampedValue = Math.min(rule.max, Math.max(rule.min, numericValue));

        return rule.type === integer
            ? Math.round(clampedValue)
            : Number(clampedValue.toFixed(getDecimalPlaces(rule.step)));
    }

    function getDecimalPlaces(step) {
        const stepText = String(step);
        const decimalIndex = stepText.indexOf('.');

        return decimalIndex === -1 ? 0 : stepText.length - decimalIndex - 1;
    }

    window.F1SetupRanges = {
        gameIds,
        getFieldRule,
        isFieldAvailable,
        getHiddenFieldValue,
        clampFieldValue
    };
})();
