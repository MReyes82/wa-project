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

    public List<Setup> getDefaultSetup()
    {
        return setupDao.getAll();
    }
}
