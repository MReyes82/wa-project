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

    /**
     * READ all operation for the user implementing prepared statement safety
     * using question marks (?) guardrails for query safety
     * @return List<User> : list of the mapped user objects retrieved by the query
     */
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
            System.err.println("[SetupDao] getAll failed: " + e.getMessage()
            + ". Returning empty list");
            return setups;
        }
    }

    /**
     * CREATE operation for the Setup entity, takes a new Setup object and inserts it into the database.
     * DOES NOT RETURN THE OBJECT WITH THE UPDATED ID (currently)
     * @param setup object that carries the information from the frontend's form
     * @return Optional of Setup whether the operation failed or not.
     */
    @Override
    public Optional<Setup> save(@NotNull Setup setup)
    {
        // I know, this insertion is an abomination
        // But I guess it's what happens when you want to avoid joins...
        String query = "INSERT INTO setup (user_id, game_version_id, track_id, team_id, title, annotation, " +
                "session_type, controller_type, is_wet_weather, front_wing, rear_wing, " +
                "diff_on_throttle, diff_off_throttle, engine_braking, front_camber, rear_camber, " +
                "front_toe, rear_toe, front_suspension, rear_suspension, front_anti_roll_bar, " +
                "rear_anti_roll_bar, front_ride_height, rear_ride_height, brake_pressure, brake_bias, " +
                "front_right_pressure, front_left_pressure, rear_right_pressure, rear_left_pressure) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseUtil.getConnection();
             // mark for auto-generated id retrieval since we don't have any other way to get the new id of the record after the insertion, we will need to implement this
             // in order to return the new object with the correct id after the save operation
             PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
        {
            setPreparedStatementFields(setup, ps);

            int rowsAffected = ps.executeUpdate();
            System.out.println("[SetupDao] Rows affected after INSERT query: " + rowsAffected);
            // retrieve if generated ID to return it to the frontend
            try (ResultSet generatedKeys = ps.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    int generatedKey = generatedKeys.getInt(1);
                    setup.setId(generatedKey);
                    System.out.println("[SetupDao] Setup saved with ID: " + generatedKey);
                }
            }
            // return new object to the frontend layer after the save operation
            return Optional.of(setup);
        }
        catch (SQLException e)
        {
            System.err.println("[SetupDao] Error in saving setup: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * UPDATE operation: Full update - replace all setup fields
     * @param setup new setup object with all fields to update
     * @return Optional of Setup whether the operation failed or not, if successful it returns
     * the updated object after re-fetching it from the database
     */
    @Override
    public Optional<Setup> updateFull(Setup setup)
    {
        String query = "UPDATE setup SET user_id=?, game_version_id=?, track_id=?, team_id=?, title=?, annotation=?, " +
                "session_type=?, controller_type=?, is_wet_weather=?, front_wing=?, rear_wing=?, " +
                "diff_on_throttle=?, diff_off_throttle=?, engine_braking=?, front_camber=?, rear_camber=?, " +
                "front_toe=?, rear_toe=?, front_suspension=?, rear_suspension=?, front_anti_roll_bar=?, " +
                "rear_anti_roll_bar=?, front_ride_height=?, rear_ride_height=?, brake_pressure=?, brake_bias=?, " +
                "front_right_pressure=?, front_left_pressure=?, rear_right_pressure=?, rear_left_pressure=? " +
                "WHERE id=?";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(query))
        {
            setPreparedStatementFields(setup, ps);
            ps.setInt(31, setup.getId());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected < 1)
            {
                System.err.println("[SetupDao] Full update failed: no setup found with id " + setup.getId() + ", returning empty object");
                return Optional.empty();
            }

            System.out.println("[SetupDao] Full update successful. Rows affected: " + rowsAffected);
            // Re-fetch and return the updated setup
            return get(setup.getId());
        }
        catch (SQLException e)
        {
            System.err.println("[SetupDao] Error in updateFull: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * UPDATE operation: Partial update - update only specified fields in the map, the rest of the fields remain unchanged
     * @param id ID of the entity to update
     * @param fields Map of field names to new values (only these fields are updated)
     * @return boolean: true if update succeeded, false otherwise or if the fields map is invalid (null, empty, or contains invalid fields)
     */
    @Override
    public boolean updatePartial(long id, Map<String, Object> fields)
    {
        if (fields == null || fields.isEmpty())
        {
            System.err.println("[SetupDao] Partial update failed: fields map cannot be null or empty");
            return false;
        }
        if (!isFieldsMapSafe(fields))
        {
            System.err.println("[SetupDao] Partial update failed: fields map contains invalid fields");
            return false;
        }

        StringBuilder query = new StringBuilder("UPDATE set ");
        List<Object> params = new ArrayList<>();
        // 0-begin index in order to account for the first element of the query to use correct syntax
        int fieldCount = 0;
        for (String field :  fields.keySet())
        {
            if (fieldCount > 0)
                query.append(", "); // append if not the first parameter
            query.append(field).append(" = ?"); // append ? character to make room for passed paramater in the field map
            params.add(fields.get(field)); // add fields element to the parameters for the actual query
            fieldCount++;
        }
        query.append(" where id = ?"); // end the query
        params.add(id);

        try (Connection con = DatabaseUtil.getConnection();
            PreparedStatement ps = con.prepareStatement(query.toString()))
        {
            // set the parameters in the prepared statement
            for (int i = 0; i < params.size(); i++)
            {
                ps.setObject(i + 1, params.get(i)); // setObject is used to handle different types of parameters dynamically
            }
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected < 1)
            {
                System.err.println("[SetupDao] Partial update failed: no setup found with id " + id);
                return false;
            }
            System.out.println("[SetupDao] Partial update successful. Rows affected: " + rowsAffected);
            return true;
        }
        catch (SQLException e)
        {
            System.err.println("[SetupDao] Error in updatePartial: " + e.getMessage());
            return false;
        }
    }
    @Override
    public void delete(Setup setup)
    {
        String query = "DELETE FROM setup WHERE id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(query))
        {
            ps.setInt(1, setup.getId());
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected < 1)
            {
                System.err.println("[SetupDao] Delete failed: no setup found with id " + setup.getId());
            }
            else
            {
                System.out.println("[SetupDao] Delete successful. Rows affected: " + rowsAffected);
            }
        }
        catch (SQLException e)
        {
            System.err.println("[SetupDao] Error in delete: " + e.getMessage());
        }
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

    /**
     * Helper method to set the fields of the prepared statement for both the save and update operations since they share the same fields,
     * this is to avoid code duplication.
     * @param setup setup object to be used to retrieve the values to set in the prepared statement
     * @param ps initialized PreparedStatement object to set the new values.
     * @throws SQLException if any of the sets fail
     */
    private void setPreparedStatementFields(Setup setup, PreparedStatement ps) throws SQLException
    {
        ps.setInt(1, setup.getUserId());
        ps.setInt(2, setup.getGameVersionId());
        ps.setInt(3, setup.getTrackId());
        ps.setInt(4, setup.getTeamId());
        ps.setString(5, setup.getTitle());
        ps.setString(6, setup.getAnnotation());
        ps.setString(7, setup.getSessionType().toString());
        ps.setString(8, setup.getControllerType().toString());
        ps.setBoolean(9, setup.getWetWeather());
        ps.setInt(10, setup.getFrontWing());
        ps.setInt(11, setup.getRearWing());
        ps.setInt(12, setup.getDiffOnThrottle());
        ps.setInt(13, setup.getDiffOffThrottle());
        ps.setInt(14, setup.getEngineBraking());
        ps.setFloat(15, setup.getFrontCamber());
        ps.setFloat(16, setup.getRearCamber());
        ps.setFloat(17, setup.getFrontToe());
        ps.setFloat(18, setup.getRearToe());
        ps.setInt(19, setup.getFrontSuspension());
        ps.setInt(20, setup.getRearSuspension());
        ps.setInt(21, setup.getFrontAntiRollBar());
        ps.setInt(22, setup.getRearAntiRollBar());
        ps.setInt(23, setup.getFrontRideHeight());
        ps.setInt(24, setup.getRearRideHeight());
        ps.setInt(25, setup.getBrakePressure());
        ps.setInt(26, setup.getBrakeBias());
        ps.setFloat(27, setup.getFrontRightPressure());
        ps.setFloat(28, setup.getFrontLeftPressure());
        ps.setFloat(29, setup.getRearRightPressure());
        ps.setFloat(30, setup.getRearLeftPressure());
    }

    /**
     * Helper function to sanitize partial (PATCH) update query
     *
     * @param fields fields to be changed, passed in updatePartial() method
     * @return true if all fields are valid, false if any field is not allowed
     */
    private static boolean isFieldsMapSafe(Map<String, Object> fields)
    {
        // Check if all the provided fields are valid
        for (var field : fields.keySet())
        {
            if (!SetupDao.allowedFields.contains(field))
            {
                System.err.println("[SetupDAO] updatePartial: Field {" + field + "} is not allowed, aborting operation");
                return false;
            }
        }
        return true;
    }
}
