package com.f1setups.services;

import com.f1setups.dao.SetupDao;
import com.f1setups.models.Setup;

import java.util.List;

public class SetupService
{
    private SetupDao setupDao; // instance of the Dao passed in the constructor

    public SetupService(SetupDao setupDao)
    {
        this.setupDao = setupDao;
    }

    /**
     * Returns the default setup registered for a specific game and track,
     * if it exists, otherwise it throws an exception.
     * @param game game to be used
     * @param track track to be used
     * @return
     * @throws Exception
     */
    public Setup getDefaultSetup(int game, int track) throws Exception
    {
        return setupDao.getDefaultSetup(game, track);
    }
}
