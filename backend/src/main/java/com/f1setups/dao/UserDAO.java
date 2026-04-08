package com.f1setups.dao;

import com.f1setups.models.User;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
    * This class handles the User CRUD operations.
    * Has the static connection for now
 */

public class UserDAO implements Dao<User>
{
    private final String URL = "jdbc:mysql://localhost:3306/f1setups";
    private final String USERNAME = "root";
    private final String PASSWORD = "password";

    // Whitelist of available fields to be used within the table definition on the schema
    private static final List<String> allowedFields = List.of("username", "email", "password");

    /**
     * READ singular operation for the user implementing prepared statement safety
     * using question marks (?) guardrails for query safety
     * @param id : ID retrieved
     * @return Optional<User> : user retrieved by the query
     */
    @Override
    public Optional<User> get(long id)
    {
        String query = "SELECT * FROM users WHERE id = ?";
        try
        {
            // Prepare the statement using the connection
            Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement ps = con.prepareStatement(query);
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) // check for failed retrieval because of non-matching id to any user
            {
                System.err.println("[UserDAO] No such user for given ID, returning empty object");
                return Optional.empty();
            }

            // Construct object from result set retrieved from the query
            // mapping the row to a new object
            User user = mapRowToUser(rs);

            return Optional.of(user);
        }
        catch (SQLException e)
        {
            System.err.println("[UserDAO] Error in get(id): " + e.getMessage() +
                    " Returning empty object");
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> getBy(String field, String value)
    {
        // check for invalid arguments
        if (value == null || value.isEmpty())
        {
            System.err.println("[UserDAO] Value is null or empty");
            return Optional.empty();
        }

        if (!allowedFields.contains(field))
        {
            System.err.println("[UserDAO] Field not allowed: " + field);
        }

        String query = "SELECT * FROM users WHERE " + field + " = ?";

        try
        {
            // Prepare the statement using the connection
            Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();

            if (!rs.next())
            {
                System.err.println("[UserDAO] No such user: " + value);
                return Optional.empty();
            }

            var user = mapRowToUser(rs);
            return Optional.of(user);
        }
        catch (SQLException e)
        {
            System.err.println("[UserDAO] Error in getBy: " +  e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * READ all operation for the user implementing prepared statement safety
     * using question marks (?) guardrails for query safety
     * @return List<User> : list of user objects retrieved by the query
     */
    @Override
    public List<User> getAll()
    {
        String query = "SELECT * FROM users";
        List<User> users = new ArrayList<>();

        try
        {
            // Prepare the statement using the connection
            Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                // map the current user
                User user = mapRowToUser(rs);
                users.add(user);
            }

            return users;
        }
        catch (SQLException e)
        {
            System.err.println("[UserDAO] Error in getAll(): " + e.getMessage()
            + "Returning empty list object");
            return users;
        }
    }

    /**
        CREATE operation for the user implementing prepared statement safety
        using question marks (?) guardrails for query safety
        @param user: User object created by the controller.
     */
    @Override
    public Optional<User> save(@NotNull User user)
    {
        String query = "INSERT INTO users (username,email,password, salt) VALUES (?,?,?,?)";

        try
        {
            // Prepare the statement using the connection
            Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement ps = con.prepareStatement(query);
            // replace the ? safely
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3,  user.getPassword());
            ps.setString(4, user.getSalt());
            // compute rows affected to know if the query was successful
            int rowsAffected = ps.executeUpdate();
            System.out.println("[UserDAO] Rows affected after INSERT query" + rowsAffected);
            return getBy("email", user.getEmail());
        }
        catch (SQLException e)
        {
            System.err.println("[UserDAO] Error in saving user" + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * PUT operation: Full update - replace all user fields
     * @param user: New User object with all fields to update
     * @return Optional<User>: The updated user if successful, empty if failed.
     */
    @Override
    public Optional<User> updateFull(User user)
    {
        String query = "UPDATE users SET username=?, email=?, password=?, salt=? WHERE id=?";

        try {
            Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getSalt());
            ps.setLong(5, user.getId());
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected < 1)
            {
                System.err.println("[UserDAO] Full update failed: no user found, returning empty object");
                return Optional.empty();
            }
            System.out.println("[UserDAO] Full update successful. Rows affected: " + rowsAffected);
            // Re-fetch and return the updated user
            return get(user.getId());
        }
        catch (SQLException e)
        {
            System.err.println("[UserDAO] Error in updateFull: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * PATCH operation: Partial update - updates only specified fields
     * @param id: ID of user to update
     * @param fields: Map of field names to new values (valid keys: "username", "email", "password")
     * @return boolean: true if update succeeded, false otherwise
     */
    @Override
    public boolean updatePartial(long id, Map<String, Object> fields)
    {
        if (fields == null ||  fields.isEmpty())
        {
            System.err.println("[UserDAO] updatePartial: Fields are empty");
            return false;
        }
        // Check if the fields are allowed, as defined in the whitelist
        if (!isFieldsMapSafe(fields, allowedFields))
        {
            return false;
        }

        // Once sanitized, begin building dynamic SQL
        StringBuilder query = new StringBuilder("UPDATE users SET ");
        List<Object> params = new ArrayList<>();
        // account for first element of the query to use correct syntax
        int fieldCount = 0;
        for (String field : fields.keySet())
        {
            if (fieldCount > 0)
                query.append(", "); // append if not the first parameter
            query.append(field).append(" = ?"); // append ? character to make room for passed parameter in the field map
            params.add(fields.get(field)); // add fields element to the parameters for the actual query
            fieldCount++;
        }
        query.append(" WHERE id = ?"); // end the query
        params.add(id);

        try
        {
            Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement ps = con.prepareStatement(query.toString());

            // Attach the parameters once they're ready into the query
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i)); //

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected < 1)
            {
                System.err.println("[UserDAO] PartialUpdate failed: no user found, returning false");
                return false;
            }

            System.out.println("[UserDAO] PartialUpdate successful. Rows affected: "  + rowsAffected);
            return true;
        }
        catch (SQLException e)
        {
            System.err.println("[UserDAO] Error in updatePartial: " + e.getMessage());
            return false;
        }
    }

    /**
     * DELETE operation for the user implementing prepared statement safety
     * using question marks (?) guardrails for query safety
     * for now, we delete the whole entity instead of marking it as deleted
     * because of educational purposes
     * @param user : user be deleted.
     *
     */
    @Override
    public void delete(User user)
    {
        String query = "DELETE FROM users WHERE id = ?";
        try
        {
            // Prepare the statement using the connection
            Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement ps = con.prepareStatement(query);
            ps.setLong(1, user.getId());
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected < 1)
            {
                System.out.println("[UserDAO] Error in deleting user, no affected rows");
                return;
            }
            System.out.println("[UserDAO] Rows affected after DELETE query" + rowsAffected);
        }
        catch (SQLException e)
        {
            System.err.println("[UserDAO] Error in deleting user" + e.getMessage());
        }
    }

    /**
     * Helper function to map the current resultSet row to a User object
     * @param rs: result set returned by the executed query
     * @return User object constructed from the result set element
     * @throws SQLException: if read fails.
     */
    private User mapRowToUser(ResultSet rs) throws SQLException
    {
        long id = rs.getLong("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String salt = rs.getString("salt");

        return new User((int) id, username, email, password,  salt);
    }

    /**
     * Helper function to sanitize partial (PATCH) update query
     * @param fields fields to be changed, passed in updatePartial() method
     * @param allowedFields map of allowed fields defined on the schema
     * @return true if all fields are valid, false if any field is not allowed
     */
    private static boolean isFieldsMapSafe(Map<String, Object> fields, List<String> allowedFields)
    {
        // Check if all the provided fields are valid
        for (var field : fields.keySet())
        {
            if (!allowedFields.contains(field))
            {
                System.err.println("[UserDAO] updatePartial: Field {" + field + "} is not allowed, aborting operation");
                return false;
            }
        }
        return true;
    }
}
