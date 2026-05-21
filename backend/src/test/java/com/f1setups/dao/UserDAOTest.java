package com.f1setups.dao;

import com.f1setups.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDAOTest
{
    private final UserDAO userDAO = new UserDAO();

    @AfterEach
    void resetConnectionProvider()
    {
        DatabaseUtil.resetConnectionProvider();
    }

    @Test
    void getReturnsUserWhenRowExists() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM users WHERE id = ?")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("id")).thenReturn(7L);
        when(rs.getString("username")).thenReturn("mreyes");
        when(rs.getString("email")).thenReturn("mreyes@mail.com");
        when(rs.getString("password")).thenReturn("secret");
        when(rs.getString("salt")).thenReturn("salt");

        Optional<User> result = userDAO.get(7L);

        assertTrue(result.isPresent());
        assertEquals(7, result.get().getId());
        assertEquals("mreyes", result.get().getUsername());
        verify(ps).setLong(1, 7L);
    }

    @Test
    void getReturnsEmptyWhenNoRowExists() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM users WHERE id = ?")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Optional<User> result = userDAO.get(999L);

        assertTrue(result.isEmpty());
        verify(ps).setLong(1, 999L);
    }

    @Test
    void getAllReturnsMappedUsers() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM users")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getLong("id")).thenReturn(1L, 2L);
        when(rs.getString("username")).thenReturn("u1", "u2");
        when(rs.getString("email")).thenReturn("u1@mail.com", "u2@mail.com");
        when(rs.getString("password")).thenReturn("p1", "p2");
        when(rs.getString("salt")).thenReturn("s1", "s2");

        var users = userDAO.getAll();

        assertEquals(2, users.size());
        assertEquals("u1", users.get(0).getUsername());
        assertEquals("u2", users.get(1).getUsername());
    }

    @Test
    void saveBindsParamsAndExecutesInsert() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        PreparedStatement psSelect = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("INSERT INTO users (username,email,password, salt) VALUES (?,?,?,?)"))
                .thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(con.prepareStatement("SELECT * FROM users WHERE email = ?")).thenReturn(psSelect);
        when(psSelect.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        userDAO.save(new User(0, "neo", "neo@matrix.com", "pw", "salt"));

        verify(ps).setString(1, "neo");
        verify(ps).setString(2, "neo@matrix.com");
        verify(ps).setString(3, "pw");
        verify(ps).setString(4, "salt");
        verify(ps).executeUpdate();
    }

    @Test
    void updateFullReturnsUpdatedUserWhenRowExists() throws Exception
    {
        Connection conUpdate = mock(Connection.class);
        Connection conSelect = mock(Connection.class);
        PreparedStatement psUpdate = mock(PreparedStatement.class);
        PreparedStatement psSelect = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        AtomicInteger callCount = new AtomicInteger(0);

        DatabaseUtil.setConnectionProvider(() -> callCount.getAndIncrement() == 0 ? conUpdate : conSelect);

        when(conUpdate.prepareStatement("UPDATE users SET username=?, email=?, password=?, salt=? WHERE id=?"))
                .thenReturn(psUpdate);
        when(psUpdate.executeUpdate()).thenReturn(1);

        when(conSelect.prepareStatement("SELECT * FROM users WHERE id = ?")).thenReturn(psSelect);
        when(psSelect.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("id")).thenReturn(3L);
        when(rs.getString("username")).thenReturn("updated");
        when(rs.getString("email")).thenReturn("updated@mail.com");
        when(rs.getString("password")).thenReturn("newpw");
        when(rs.getString("salt")).thenReturn("salt");

        Optional<User> result = userDAO.updateFull(new User(3, "updated", "updated@mail.com", "newpw", "salt"));

        assertTrue(result.isPresent());
        assertEquals("updated", result.get().getUsername());
        verify(psUpdate).setLong(5, 3L);
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

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("UPDATE users SET username = ?, email = ? WHERE id = ?"))
                .thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        boolean result = userDAO.updatePartial(5L, fields);

        assertTrue(result);
        verify(ps).setObject(1, "newName");
        verify(ps).setObject(2, "new@mail.com");
        verify(ps).setObject(3, 5L);
    }

    @Test
    void deleteExecutesById() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("DELETE FROM users WHERE id = ?")).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        userDAO.delete(new User(10, "user", "mail", "pw", "salt"));

        verify(ps).setLong(1, 10L);
        verify(ps).executeUpdate();
    }

    @Test
    void getReturnsEmptyOnSqlException()
    {
        SQLException sqlException = new SQLException("boom");
        DatabaseUtil.setConnectionProvider(() -> { throw sqlException; });

        Optional<User> result = userDAO.get(1L);

        assertTrue(result.isEmpty());
    }
}

