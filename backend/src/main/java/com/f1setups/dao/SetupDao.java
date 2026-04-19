package com.f1setups.dao;

import com.f1setups.models.Setup;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SetupDao implements Dao<Setup>
{
    private final String URL = "jdbc:mysql://localhost:3306/f1setups";
    private final String USERNAME = "root";
    private final String PASSWORD = "password";
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

    @Override
    public Optional<Setup> get()
    {

    }
    @Override
    public Optional<Setup> getBy(String field, String value)
    {

    }
    @Override
    public List<Setup> getAll()
    {

    }
    @Override
    public Optional<Setup> save(@NotNull Setup setup)
    {

    }
    @Override
    public Optional<Setup> updateFull(Setup setup)
    {

    }
    @Override
    public boolean updatePartial(long id, Map<String, Object> fields)
    {

    }
    @Override
    public void delete(Setup setup)
    {

    }
}
