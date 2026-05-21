package com.f1setups.dao;

import com.f1setups.models.ControllerType;
import com.f1setups.models.SessionType;
import com.f1setups.models.Setup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class SetupDaoIT
{
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("f1setups")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init.sql");

    @BeforeAll
    static void initConnectionProvider()
    {
        DatabaseUtil.setConnectionProvider(() -> DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ));
    }

    @AfterAll
    static void resetConnectionProvider()
    {
        DatabaseUtil.resetConnectionProvider();
    }

    @Test
    void saveAndGetRoundTrip() throws Exception
    {
        Setup setup = createSetup(insertUser(), "Integration Setup");
        SetupDao setupDao = new SetupDao();

        Optional<Setup> saved = setupDao.save(setup);

        assertTrue(saved.isPresent());
        assertTrue(saved.get().getId() > 0);

        Optional<Setup> fetched = setupDao.get(saved.get().getId());

        assertTrue(fetched.isPresent());
        assertSetupFieldsEqual(saved.get(), fetched.get());
    }

    @Test
    void getByReturnsSavedSetupForAllowedField() throws Exception
    {
        SetupDao setupDao = new SetupDao();
        Setup saved = setupDao.save(createSetup(insertUser(), "GetBy Setup"))
                .orElseThrow();

        Optional<Setup> fetched = setupDao.getBy("title", saved.getTitle());

        assertTrue(fetched.isPresent());
        assertSetupFieldsEqual(saved, fetched.get());
    }

    @Test
    void getAllIncludesSavedSetups() throws Exception
    {
        SetupDao setupDao = new SetupDao();
        Setup first = setupDao.save(createSetup(insertUser(), "GetAll Setup One"))
                .orElseThrow();
        Setup second = setupDao.save(createSetup(insertUser(), "GetAll Setup Two"))
                .orElseThrow();

        List<Setup> setups = setupDao.getAll();

        assertTrue(setups.stream().anyMatch(setup -> setup.getId() == first.getId()));
        assertTrue(setups.stream().anyMatch(setup -> setup.getId() == second.getId()));
    }

    @Test
    void updateFullPersistsAllUpdatedFields() throws Exception
    {
        SetupDao setupDao = new SetupDao();
        Setup saved = setupDao.save(createSetup(insertUser(), "Original Full Update Setup"))
                .orElseThrow();
        Setup replacement = createUpdatedSetup(saved.getId(), saved.getUserId());

        Optional<Setup> updated = setupDao.updateFull(replacement);

        assertTrue(updated.isPresent());
        assertSetupFieldsEqual(replacement, updated.get());
    }

    @Test
    void updatePartialPersistsOnlyRequestedFields() throws Exception
    {
        SetupDao setupDao = new SetupDao();
        Setup saved = setupDao.save(createSetup(insertUser(), "Original Partial Update Setup"))
                .orElseThrow();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("title", "Updated Partial Setup");
        fields.put("session_type", SessionType.RACE.name());
        fields.put("controller_type", ControllerType.WHEEL.name());
        fields.put("is_wet_weather", true);
        fields.put("brake_bias", 61);

        boolean updated = setupDao.updatePartial(saved.getId(), fields);
        Optional<Setup> fetched = setupDao.get(saved.getId());

        assertTrue(updated);
        assertTrue(fetched.isPresent());
        assertEquals("Updated Partial Setup", fetched.get().getTitle());
        assertEquals(SessionType.RACE, fetched.get().getSessionType());
        assertEquals(ControllerType.WHEEL, fetched.get().getControllerType());
        assertTrue(fetched.get().getWetWeather());
        assertEquals(61, fetched.get().getBrakeBias());
        assertEquals(saved.getFrontWing(), fetched.get().getFrontWing());
        assertEquals(saved.getRearWing(), fetched.get().getRearWing());
    }

    @Test
    void deleteRemovesSetup() throws Exception
    {
        SetupDao setupDao = new SetupDao();
        Setup saved = setupDao.save(createSetup(insertUser(), "Delete Setup"))
                .orElseThrow();

        setupDao.delete(saved);

        assertTrue(setupDao.get(saved.getId()).isEmpty());
    }

    @Test
    void missingRowsAndConstraintFailuresReturnFailureResults() throws Exception
    {
        SetupDao setupDao = new SetupDao();
        int missingSetupId = 999_999;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("title", "Missing Setup");

        Optional<Setup> invalidSave = setupDao.save(createSetup(missingSetupId, "Invalid User Setup"));
        Optional<Setup> missingById = setupDao.get(missingSetupId);
        Optional<Setup> missingByTitle = setupDao.getBy("title", "Missing Setup");
        Optional<Setup> missingFullUpdate = setupDao.updateFull(createUpdatedSetup(missingSetupId, insertUser()));
        boolean missingPartialUpdate = setupDao.updatePartial(missingSetupId, fields);

        assertTrue(invalidSave.isEmpty());
        assertTrue(missingById.isEmpty());
        assertTrue(missingByTitle.isEmpty());
        assertTrue(missingFullUpdate.isEmpty());
        assertFalse(missingPartialUpdate);
    }

    private Setup createSetup(int userId, String title)
    {
        return new Setup(
                0,
                userId,
                1,
                1,
                1,
                title,
                "Notes",
                SessionType.PRACTICE,
                ControllerType.GAMEPAD,
                false,
                LocalDateTime.now(),
                10,
                20,
                30,
                40,
                50,
                1.1f,
                1.2f,
                0.1f,
                0.2f,
                5,
                6,
                7,
                8,
                9,
                10,
                100,
                55,
                21.5f,
                21.6f,
                21.7f,
                21.8f
        );
    }

    private Setup createUpdatedSetup(int setupId, int userId)
    {
        return new Setup(
                setupId,
                userId,
                2,
                2,
                2,
                "Updated Full Setup",
                "Updated notes",
                SessionType.QUALIFYING,
                ControllerType.WHEEL,
                true,
                LocalDateTime.now(),
                11,
                21,
                31,
                41,
                51,
                1.3f,
                1.4f,
                0.3f,
                0.4f,
                15,
                16,
                17,
                18,
                19,
                20,
                101,
                56,
                22.5f,
                22.6f,
                22.7f,
                22.8f
        );
    }

    private void assertSetupFieldsEqual(Setup expected, Setup actual)
    {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUserId(), actual.getUserId());
        assertEquals(expected.getGameVersionId(), actual.getGameVersionId());
        assertEquals(expected.getTrackId(), actual.getTrackId());
        assertEquals(expected.getTeamId(), actual.getTeamId());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getAnnotation(), actual.getAnnotation());
        assertEquals(expected.getSessionType(), actual.getSessionType());
        assertEquals(expected.getControllerType(), actual.getControllerType());
        assertEquals(expected.getWetWeather(), actual.getWetWeather());
        assertNotNull(actual.getCreatedAt());
        assertEquals(expected.getFrontWing(), actual.getFrontWing());
        assertEquals(expected.getRearWing(), actual.getRearWing());
        assertEquals(expected.getDiffOnThrottle(), actual.getDiffOnThrottle());
        assertEquals(expected.getDiffOffThrottle(), actual.getDiffOffThrottle());
        assertEquals(expected.getEngineBraking(), actual.getEngineBraking());
        assertEquals(expected.getFrontCamber(), actual.getFrontCamber(), 0.001f);
        assertEquals(expected.getRearCamber(), actual.getRearCamber(), 0.001f);
        assertEquals(expected.getFrontToe(), actual.getFrontToe(), 0.001f);
        assertEquals(expected.getRearToe(), actual.getRearToe(), 0.001f);
        assertEquals(expected.getFrontSuspension(), actual.getFrontSuspension());
        assertEquals(expected.getRearSuspension(), actual.getRearSuspension());
        assertEquals(expected.getFrontAntiRollBar(), actual.getFrontAntiRollBar());
        assertEquals(expected.getRearAntiRollBar(), actual.getRearAntiRollBar());
        assertEquals(expected.getFrontRideHeight(), actual.getFrontRideHeight());
        assertEquals(expected.getRearRideHeight(), actual.getRearRideHeight());
        assertEquals(expected.getBrakePressure(), actual.getBrakePressure());
        assertEquals(expected.getBrakeBias(), actual.getBrakeBias());
        assertEquals(expected.getFrontRightPressure(), actual.getFrontRightPressure(), 0.001f);
        assertEquals(expected.getFrontLeftPressure(), actual.getFrontLeftPressure(), 0.001f);
        assertEquals(expected.getRearRightPressure(), actual.getRearRightPressure(), 0.001f);
        assertEquals(expected.getRearLeftPressure(), actual.getRearLeftPressure(), 0.001f);
    }

    private int insertUser() throws Exception
    {
        long uniqueId = System.nanoTime();

        return insertUser("tester-" + uniqueId, "tester-" + uniqueId + "@mail.com", "hashed", "salt");
    }

    private int insertUser(String username, String email, String password, String salt) throws Exception
    {
        String query = "INSERT INTO users (username, email, password, salt) VALUES (?,?,?,?)";
        try (Connection con = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             PreparedStatement ps = con.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS))
        {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, salt);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Failed to create test user");
    }
}
