package com.f1setups.dao;

import com.f1setups.models.ControllerType;
import com.f1setups.models.SessionType;
import com.f1setups.models.Setup;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SetupDao implements Dao<Setup>
{
    // whitelist of available fields to be used within the table definition of the schema
    private static final List<String> allowedFields = List.of(
            "team_id",
            "title",
            "annotation",
            "session_type",
            "controller_type",
            "is_wet_weather",
            "created_at",
            "front_wing",
            "rear_wing",
            "diff_on_throttle",
            "diff_off_throttle",
            "engine_braking",
            "front_camber",
            "rear_camber",
            "front_toe",
            "rear_toe",
            "front_suspension",
            "rear_suspension",
            "front_anti_roll_bar",
            "rear_anti_roll_bar",
            "front_ride_height",
            "rear_ride_height",
            "brake_pressure",
            "brake_bias",
            "front_right_pressure",
            "front_left_pressure",
            "rear_right_pressure",
            "rear_left_pressure"
    );

    /**
     * READ singular operation for the Setup entity, retrieves a single record
     * from the database with the specified ID and maps it to a Setup object.
     * using ? guardrail for the query.
     * @param id long number retrieved from the data layer
     * @return An optional object for whether the retrieval is successful or failed.
     */
    @Override
    public Optional<Setup> get(long id)
    {
        String query = "SELECT * FROM setup WHERE id = ?";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(query))
        {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next())
            {
                System.err.println("[SetupDao] No setup found with id: " + id + ". Returning empty object");
                return Optional.empty();
            }
            Setup setup = mapRowToSetup(rs);
            return Optional.of(setup);
        }
        catch (SQLException e)
        {
            System.err.println("[SetupDao] Error in get(id): " + e.getMessage() +
                    " Returning empty object");
            return Optional.empty();
        }
    }

    /**
     * READ singular operation for the Setup entity, retrieves a single record
     * from the database with a specified field and value and maps it to a Setup object.
     * Only after checking that the field is valid and exists in the table definition of the schema,
     * Uses ? guardrail for the query for both the field and value parameters
     * @param field column defined in the table of the entity
     * @param value actual value to be used
     * @return An optional object for whether the retrieval is successful or failed.
     */
    @Override
    public Optional<Setup> getBy(String field, String value)
    {
        // Check for invalid arguments
        if (value == null || value.isEmpty())
        {
            System.err.println("[SetupDao] getBy failed: value cannot be null or empty. Returning empty object");
            return Optional.empty();
        }

        if (!allowedFields.contains(field))
        {
            System.err.println("[SetupDao] getBy failed: field is not allowed: " + field);
            return  Optional.empty();
        }

        String query = "SELECT * FROM setup WHERE ? = ?";
        try (Connection con = DatabaseUtil.getConnection();
            PreparedStatement ps = con.prepareStatement(query))
        {
            ps.setString(1, field);
            ps.setString(2, value);
            ResultSet rs = ps.executeQuery();

            if (!rs.next())
            {
                System.err.println("[SetupDao] getBy failed: no setup found with value: " + value);
                return Optional.empty();
            }

            var setup =  mapRowToSetup(rs);
            return Optional.of(setup);
        }
        catch (SQLException e)
        {
            System.err.println("[SetupDao] getBy failed: " + e.getMessage());
            return Optional.empty();
        }
    }
    @Override
    public List<Setup> getAll()
    {
        String query = "SELECT * FROM setup";
        List<Setup> setups = new ArrayList<>();

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery())
        {
            while (rs.next()) // while there are still setups to be retrieved from the result set
            {
                // map the current setup
                Setup setup = mapRowToSetup(rs);
                setups.add(setup);
            }
            return setups;
        }
        catch (SQLException e)
        {
            System.err.println("[SetupDao] getAll failed: " + e.getMessage());
            return setups;
        }
    }
    @Override
    public Optional<Setup> save(@NotNull Setup setup)
    {
        return Optional.empty();
    }
    @Override
    public Optional<Setup> updateFull(Setup setup)
    {
        return Optional.empty();
    }
    @Override
    public boolean updatePartial(long id, Map<String, Object> fields)
    {
        return true;
    }
    @Override
    public void delete(Setup setup)
    {

    }

    private Setup mapRowToSetup(ResultSet rs) throws SQLException
    {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        int gameVersionId = rs.getInt("game_version_id");
        int trackId = rs.getInt("track_id");
        int teamId = rs.getInt("team_id");
        String title = rs.getString("title");
        String annotation = rs.getString("annotation");
        String sessionTypeString = rs.getString("session_type");
        SessionType sessionType = SessionType.fromString(sessionTypeString);
        String controllerTypeString = rs.getString("controller_type");
        ControllerType controllerType = ControllerType.fromString(controllerTypeString);
        boolean isWetWeather = rs.getBoolean("is_wet_weather");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        // Aero
        int frontWing = rs.getInt("front_wing");
        int rearWing = rs.getInt("rear_wing");

        // Transmission
        int diffOnThrottle = rs.getInt("diff_on_throttle");
        int diffOffThrottle = rs.getInt("diff_off_throttle");
        int engineBraking = rs.getInt("engine_braking");

        // Suspension geometry
        float frontCamber = rs.getFloat("front_camber");
        float rearCamber = rs.getFloat("rear_camber");
        float frontToe = rs.getFloat("front_toe");
        float rearToe = rs.getFloat("rear_toe");

        // Suspension
        int frontSuspension = rs.getInt("front_suspension");
        int rearSuspension = rs.getInt("rear_suspension");
        int frontAntiRollBar = rs.getInt("front_anti_roll_bar");
        int rearAntiRollBar = rs.getInt("rear_anti_roll_bar");
        int frontRideHeight = rs.getInt("front_ride_height");
        int rearRideHeight = rs.getInt("rear_ride_height");

        // Brakes
        int brakePressure = rs.getInt("brake_pressure");
        int brakeBias = rs.getInt("brake_bias");

        // Tyres
        float frontRightPressure = rs.getFloat("front_right_pressure");
        float frontLeftPressure = rs.getFloat("front_left_pressure");
        float rearRightPressure = rs.getFloat("rear_right_pressure");
        float rearLeftPressure = rs.getFloat("rear_left_pressure");

        return new Setup(id, userId, gameVersionId, trackId, teamId, title, annotation,
                sessionType, controllerType, isWetWeather, createdAt,
                frontWing, rearWing, diffOnThrottle, diffOffThrottle, engineBraking,
                frontCamber, rearCamber, frontToe, rearToe,
                frontSuspension, rearSuspension, frontAntiRollBar, rearAntiRollBar,
                frontRideHeight, rearRideHeight,
                brakePressure, brakeBias,
                frontRightPressure, frontLeftPressure, rearRightPressure, rearLeftPressure);
    }
}
