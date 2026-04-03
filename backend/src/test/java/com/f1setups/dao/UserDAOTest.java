package com.f1setups.dao;

import com.f1setups.models.User;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserDAOTest
{
    private final UserDAO userDAO = new UserDAO();

    @Test
    void getReturnsUserWhenRowExists() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class))
        {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(con);

            when(con.prepareStatement("SELECT * FROM users WHERE id = ?")).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getLong("id")).thenReturn(7L);
            when(rs.getString("username")).thenReturn("mreyes");
            when(rs.getString("email")).thenReturn("mreyes@mail.com");
            when(rs.getString("password")).thenReturn("secret");

            Optional<User> result = userDAO.get(7L);

            assertTrue(result.isPresent());
            assertEquals(7, result.get().getId());
            assertEquals("mreyes", result.get().getUsername());
            verify(ps).setLong(1, 7L);
        }
    }

    @Test
    void getReturnsEmptyWhenNoRowExists() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class))
        {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(con);

            when(con.prepareStatement("SELECT * FROM users WHERE id = ?")).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            Optional<User> result = userDAO.get(999L);

            assertTrue(result.isEmpty());
            verify(ps).setLong(1, 999L);
        }
    }

    @Test
    void getAllReturnsMappedUsers() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class))
        {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(con);

            when(con.prepareStatement("SELECT * FROM users")).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, true, false);
            when(rs.getLong("id")).thenReturn(1L, 2L);
            when(rs.getString("username")).thenReturn("u1", "u2");
            when(rs.getString("email")).thenReturn("u1@mail.com", "u2@mail.com");
            when(rs.getString("password")).thenReturn("p1", "p2");

            var users = userDAO.getAll();

            assertEquals(2, users.size());
            assertEquals("u1", users.get(0).getUsername());
            assertEquals("u2", users.get(1).getUsername());
        }
    }

    @Test
    void saveBindsParamsAndExecutesInsert() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class))
        {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(con);

            when(con.prepareStatement("INSERT INTO users (username,email,password) VALUES (?,?,?)"))
                    .thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            userDAO.save(new User(0, "neo", "neo@matrix.com", "pw"));

            verify(ps).setString(1, "neo");
            verify(ps).setString(2, "neo@matrix.com");
            verify(ps).setString(3, "pw");
            verify(ps).executeUpdate();
        }
    }

    @Test
    void updateFullReturnsUpdatedUserWhenRowExists() throws Exception
    {
        Connection conUpdate = mock(Connection.class);
        Connection conSelect = mock(Connection.class);
        PreparedStatement psUpdate = mock(PreparedStatement.class);
        PreparedStatement psSelect = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class))
        {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(conUpdate, conSelect);

            when(conUpdate.prepareStatement("UPDATE users SET username=?, email=?, password=? WHERE id=?"))
                    .thenReturn(psUpdate);
            when(psUpdate.executeUpdate()).thenReturn(1);

            when(conSelect.prepareStatement("SELECT * FROM users WHERE id = ?")).thenReturn(psSelect);
            when(psSelect.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getLong("id")).thenReturn(3L);
            when(rs.getString("username")).thenReturn("updated");
            when(rs.getString("email")).thenReturn("updated@mail.com");
            when(rs.getString("password")).thenReturn("newpw");

            Optional<User> result = userDAO.updateFull(new User(3, "updated", "updated@mail.com", "newpw"));

            assertTrue(result.isPresent());
            assertEquals("updated", result.get().getUsername());
            verify(psUpdate).setLong(4, 3L);
        }
    }

    @Test
    void updatePartialReturnsFalseForEmptyFields() {
        assertFalse(userDAO.updatePartial(1L, Map.of()));
    }

    @Test
    void updatePartialReturnsFalseForUnsafeField() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("role", "admin");

        assertFalse(userDAO.updatePartial(1L, fields));
    }

    @Test
    void updatePartialBuildsDynamicSqlAndUpdates() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("username", "newName");
        fields.put("email", "new@mail.com");

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class))
        {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(con);

            when(con.prepareStatement("UPDATE users SET username = ?, email = ? WHERE id = ?"))
                    .thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            boolean result = userDAO.updatePartial(5L, fields);

            assertTrue(result);
            verify(ps).setObject(1, "newName");
            verify(ps).setObject(2, "new@mail.com");
            verify(ps).setObject(3, 5L);
        }
    }

    @Test
    void deleteExecutesById() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class))
        {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(con);

            when(con.prepareStatement("DELETE FROM users WHERE id = ?")).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            userDAO.delete(new User(10, "user", "mail", "pw"));

            verify(ps).setLong(1, 10L);
            verify(ps).executeUpdate();
        }
    }

    @Test
    void getReturnsEmptyOnSqlException()
    {
        SQLException sqlException = new SQLException("boom");
        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class))
        {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenThrow(sqlException);

            Optional<User> result = userDAO.get(1L);

            assertTrue(result.isEmpty());
        }
    }
}



