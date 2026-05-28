package com.f1setups.dao;

import com.f1setups.models.ControllerType;
import com.f1setups.models.SessionType;
import com.f1setups.models.Setup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SetupDaoTest
{
    private final SetupDao setupDao = new SetupDao();

    @AfterEach
    void resetConnectionProvider()
    {
        DatabaseUtil.resetConnectionProvider();
    }

    @Test
    void getReturnsSetupWhenRowExists() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM setup WHERE id = ?")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("id")).thenReturn(12);
        when(rs.getString("session_type")).thenReturn("PRACTICE");
        when(rs.getString("controller_type")).thenReturn("GAMEPAD");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(createdAt));

        Optional<Setup> result = setupDao.get(12);

        assertTrue(result.isPresent());
        assertEquals(12, result.get().getId());
        assertEquals(SessionType.PRACTICE, result.get().getSessionType());
        verify(ps).setLong(1, 12L);
    }

    @Test
    void getReturnsEmptyWhenNoRowExists() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM setup WHERE id = ?")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Optional<Setup> result = setupDao.get(999L);

        assertTrue(result.isEmpty());
        verify(ps).setLong(1, 999L);
    }

    @Test
    void getByReturnsEmptyForInvalidField()
    {
        Optional<Setup> result = setupDao.getBy("not_a_column", "value");

        assertTrue(result.isEmpty());
    }

    @Test
    void getByBindsValueAndReturnsSetup() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 1, 0, 0);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM setup WHERE team_id = ?")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("session_type")).thenReturn("PRACTICE");
        when(rs.getString("controller_type")).thenReturn("GAMEPAD");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(createdAt));

        Optional<Setup> result = setupDao.getBy("team_id", "3");

        assertTrue(result.isPresent());
        verify(ps).setString(1, "3");
    }

    @Test
    void getAllReturnsMappedSetups() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM setup")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getInt("id")).thenReturn(1, 2);
        when(rs.getString("session_type")).thenReturn("PRACTICE", "RACE");
        when(rs.getString("controller_type")).thenReturn("GAMEPAD", "WHEEL");
        when(rs.getTimestamp("created_at"))
                .thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)),
                        Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 0, 0)));

        var setups = setupDao.getAll();

        assertEquals(2, setups.size());
        assertEquals(1, setups.get(0).getId());
        assertEquals(2, setups.get(1).getId());
    }

    @Test
    void saveSetsGeneratedId() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet generatedKeys = mock(ResultSet.class);
        Setup setup = createSetup(0);

        String query = "INSERT INTO setup (user_id, game_version_id, track_id, team_id, title, annotation, " +
                "session_type, controller_type, is_wet_weather, front_wing, rear_wing, " +
                "diff_on_throttle, diff_off_throttle, engine_braking, front_camber, rear_camber, " +
                "front_toe, rear_toe, front_suspension, rear_suspension, front_anti_roll_bar, " +
                "rear_anti_roll_bar, front_ride_height, rear_ride_height, brake_pressure, brake_bias, " +
                "front_right_pressure, front_left_pressure, rear_right_pressure, rear_left_pressure) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(true);
        when(generatedKeys.getInt(1)).thenReturn(99);

        Optional<Setup> result = setupDao.save(setup);

        assertTrue(result.isPresent());
        assertEquals(99, setup.getId());
        verify(ps).setInt(1, setup.getUserId());
        verify(ps).setString(5, setup.getTitle());
        verify(ps).executeUpdate();
    }

    @Test
    void updateFullReturnsUpdatedSetup() throws Exception
    {
        Connection conUpdate = mock(Connection.class);
        Connection conSelect = mock(Connection.class);
        PreparedStatement psUpdate = mock(PreparedStatement.class);
        PreparedStatement psSelect = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        Setup setup = createSetup(7);
        AtomicInteger callCount = new AtomicInteger(0);

        String query = "UPDATE setup SET user_id=?, game_version_id=?, track_id=?, team_id=?, title=?, annotation=?, " +
                "session_type=?, controller_type=?, is_wet_weather=?, front_wing=?, rear_wing=?, " +
                "diff_on_throttle=?, diff_off_throttle=?, engine_braking=?, front_camber=?, rear_camber=?, " +
                "front_toe=?, rear_toe=?, front_suspension=?, rear_suspension=?, front_anti_roll_bar=?, " +
                "rear_anti_roll_bar=?, front_ride_height=?, rear_ride_height=?, brake_pressure=?, brake_bias=?, " +
                "front_right_pressure=?, front_left_pressure=?, rear_right_pressure=?, rear_left_pressure=? " +
                "WHERE id=?";

        DatabaseUtil.setConnectionProvider(() -> callCount.getAndIncrement() == 0 ? conUpdate : conSelect);

        when(conUpdate.prepareStatement(query)).thenReturn(psUpdate);
        when(psUpdate.executeUpdate()).thenReturn(1);

        when(conSelect.prepareStatement("SELECT * FROM setup WHERE id = ?")).thenReturn(psSelect);
        when(psSelect.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("id")).thenReturn(7);
        when(rs.getString("session_type")).thenReturn("PRACTICE");
        when(rs.getString("controller_type")).thenReturn("GAMEPAD");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 3, 1, 0, 0)));

        Optional<Setup> result = setupDao.updateFull(setup);

        assertTrue(result.isPresent());
        verify(psUpdate).setInt(31, 7);
    }

    @Test
    void updatePartialReturnsFalseForEmptyFields()
    {
        assertFalse(setupDao.updatePartial(1L, Map.of()));
    }

    @Test
    void updatePartialReturnsFalseForUnsafeField()
    {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("not_a_column", "value");

        assertFalse(setupDao.updatePartial(1L, fields));
    }

    @Test
    void updatePartialBuildsDynamicSqlAndUpdates() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("title", "New title");
        fields.put("brake_bias", 60);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("UPDATE setup SET title = ?, brake_bias = ? WHERE id = ?"))
                .thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        boolean result = setupDao.updatePartial(5L, fields);

        assertTrue(result);
        verify(ps).setObject(1, "New title");
        verify(ps).setObject(2, 60);
        verify(ps).setObject(3, 5L);
    }

    @Test
    void deleteExecutesById() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        Setup setup = createSetup(10);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("DELETE FROM setup WHERE id = ?")).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        setupDao.delete(setup);

        verify(ps).setInt(1, 10);
        verify(ps).executeUpdate();
    }

    @Test
    void getCommunitySetupsExcludesDefaultUserAndFiltersSelection() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT s.*, u.username AS username FROM setup s " +
                "INNER JOIN users u ON u.id = s.user_id " +
                "WHERE s.user_id <> ? AND s.game_version_id = ? AND s.track_id = ? " +
                "ORDER BY s.created_at DESC, s.id DESC"))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(15);
        when(rs.getString("session_type")).thenReturn("RACE");
        when(rs.getString("controller_type")).thenReturn("WHEEL");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 4, 1, 0, 0)));

        var setups = setupDao.getCommunitySetups(4, 19);

        assertEquals(1, setups.size());
        assertEquals(15, setups.get(0).setup.getId());
        verify(ps).setInt(1, 1);
        verify(ps).setInt(2, 4);
        verify(ps).setInt(3, 19);
    }

    @Test
    void getSetupsByUserAndSelectionFiltersByOwnerAndSelection() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM setup WHERE user_id = ? AND game_version_id = ? AND track_id = ?"))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(22);
        when(rs.getString("session_type")).thenReturn("QUALIFYING");
        when(rs.getString("controller_type")).thenReturn("GAMEPAD");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 4, 2, 0, 0)));

        var setups = setupDao.getSetupsByUserAndSelection(9, 4, 19);

        assertEquals(1, setups.size());
        assertEquals(22, setups.get(0).getId());
        verify(ps).setInt(1, 9);
        verify(ps).setInt(2, 4);
        verify(ps).setInt(3, 19);
    }

    @Test
    void getByIdAndUserIdReturnsOnlyOwnedSetup() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM setup WHERE id = ? AND user_id = ?")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("id")).thenReturn(31);
        when(rs.getString("session_type")).thenReturn("TIME_TRIAL");
        when(rs.getString("controller_type")).thenReturn("WHEEL");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 4, 3, 0, 0)));

        Optional<Setup> result = setupDao.getByIdAndUserId(31, 9);

        assertTrue(result.isPresent());
        assertEquals(31, result.get().getId());
        verify(ps).setInt(1, 31);
        verify(ps).setInt(2, 9);
    }

    @Test
    void getByIdAndUserIdReturnsEmptyWhenSetupIsNotOwned() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("SELECT * FROM setup WHERE id = ? AND user_id = ?")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Optional<Setup> result = setupDao.getByIdAndUserId(31, 9);

        assertTrue(result.isEmpty());
        verify(ps).setInt(1, 31);
        verify(ps).setInt(2, 9);
    }

    @Test
    void deleteByIdAndUserIdDeletesOnlyOwnedSetup() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("DELETE FROM setup WHERE id = ? AND user_id = ?")).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        boolean deleted = setupDao.deleteByIdAndUserId(44, 9);

        assertTrue(deleted);
        verify(ps).setInt(1, 44);
        verify(ps).setInt(2, 9);
    }

    @Test
    void deleteByIdAndUserIdReturnsFalseWhenSetupIsNotOwned() throws Exception
    {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        DatabaseUtil.setConnectionProvider(() -> con);

        when(con.prepareStatement("DELETE FROM setup WHERE id = ? AND user_id = ?")).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(0);

        boolean deleted = setupDao.deleteByIdAndUserId(44, 9);

        assertFalse(deleted);
        verify(ps).setInt(1, 44);
        verify(ps).setInt(2, 9);
    }

    @Test
    void getReturnsEmptyOnSqlException()
    {
        SQLException sqlException = new SQLException("boom");
        DatabaseUtil.setConnectionProvider(() -> { throw sqlException; });

        Optional<Setup> result = setupDao.get(1L);

        assertTrue(result.isEmpty());
    }

    private Setup createSetup(int id)
    {
        return new Setup(
                id,
                1,
                2,
                3,
                4,
                "Title",
                "Notes",
                SessionType.PRACTICE,
                ControllerType.GAMEPAD,
                false,
                LocalDateTime.of(2026, 1, 1, 0, 0),
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
}
