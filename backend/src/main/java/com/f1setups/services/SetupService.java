package com.f1setups.services;

import com.f1setups.dao.SetupDao;
import com.f1setups.models.Setup;

import java.util.List;
import java.util.Optional;

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
        validateSelection(game, track);

        Setup defaultSetup = setupDao.getDefaultSetup(game, track);
        if (defaultSetup == null)
        {
            throw new Exception("[SetupService] Default setup not found");
        }

        return defaultSetup;
    }

    /**
     * Returns public community setups for a specific game and track.
     * Default setups are excluded in the DAO layer.
     * @param game game version id
     * @param track track id
     * @return matching community setups
     * @throws Exception if the selected ids are invalid
     */
    public List<Setup> getCommunitySetups(int game, int track) throws Exception
    {
        validateSelection(game, track);

        return setupDao.getCommunitySetups(game, track);
    }

    /**
     * Returns setups owned by the authenticated user for a specific game and track.
     * @param authenticatedUserId user id extracted from the auth token
     * @param game game version id
     * @param track track id
     * @return matching user-owned setups
     * @throws Exception if arguments are invalid
     */
    public List<Setup> getUserSetups(int authenticatedUserId, int game, int track) throws Exception
    {
        validateUserId(authenticatedUserId);
        validateSelection(game, track);

        return setupDao.getSetupsByUserAndSelection(authenticatedUserId, game, track);
    }

    /**
     * Returns one setup only when it belongs to the authenticated user.
     * @param authenticatedUserId user id extracted from the auth token
     * @param setupId setup id from the route
     * @return matching user-owned setup
     * @throws Exception if the setup does not exist or is not owned by the user
     */
    public Setup getUserSetup(int authenticatedUserId, int setupId) throws Exception
    {
        validateUserId(authenticatedUserId);
        validateSetupId(setupId);

        Optional<Setup> setup = setupDao.getByIdAndUserId(setupId, authenticatedUserId);
        if (setup.isEmpty())
        {
            throw new Exception("[SetupService] Setup not found for authenticated user");
        }

        return setup.get();
    }

    /**
     * Creates a setup owned by the authenticated user.
     * Any userId sent by the client is overwritten here.
     * @param authenticatedUserId user id extracted from the auth token
     * @param setup setup payload from the request body
     * @return saved setup
     * @throws Exception if validation or persistence fails
     */
    public Setup createUserSetup(int authenticatedUserId, Setup setup) throws Exception
    {
        validateUserId(authenticatedUserId);
        validateSetupPayload(setup);

        setup.setUserId(authenticatedUserId);

        Optional<Setup> savedSetup = setupDao.save(setup);
        if (savedSetup.isEmpty())
        {
            throw new Exception("[SetupService] Failed to create setup");
        }

        return savedSetup.get();
    }

    /**
     * Fully updates a setup only when it belongs to the authenticated user.
     * The route setup id and authenticated user id override any client-supplied values.
     * @param authenticatedUserId user id extracted from the auth token
     * @param setupId setup id from the route
     * @param setup replacement setup payload
     * @return updated setup
     * @throws Exception if validation, ownership check, or persistence fails
     */
    public Setup updateUserSetup(int authenticatedUserId, int setupId, Setup setup) throws Exception
    {
        validateUserId(authenticatedUserId);
        validateSetupId(setupId);
        validateSetupPayload(setup);

        // Ownership is checked before updateFull, which updates by setup id.
        getUserSetup(authenticatedUserId, setupId);

        setup.setId(setupId);
        setup.setUserId(authenticatedUserId);

        Optional<Setup> updatedSetup = setupDao.updateFull(setup);
        if (updatedSetup.isEmpty())
        {
            throw new Exception("[SetupService] Failed to update setup");
        }

        return updatedSetup.get();
    }

    /**
     * Deletes a setup only when it belongs to the authenticated user.
     * @param authenticatedUserId user id extracted from the auth token
     * @param setupId setup id from the route
     * @throws Exception if the setup does not exist, is not owned by the user, or deletion fails
     */
    public void deleteUserSetup(int authenticatedUserId, int setupId) throws Exception
    {
        validateUserId(authenticatedUserId);
        validateSetupId(setupId);

        boolean deleted = setupDao.deleteByIdAndUserId(setupId, authenticatedUserId);
        if (!deleted)
        {
            throw new Exception("[SetupService] Failed to delete setup for authenticated user");
        }
    }

    private void validateSelection(int game, int track) throws Exception
    {
        if (game <= 0 || track <= 0)
        {
            throw new Exception("[SetupService] Game and track ids must be positive");
        }
    }

    private void validateUserId(int authenticatedUserId) throws Exception
    {
        if (authenticatedUserId <= 0)
        {
            throw new Exception("[SetupService] Authenticated user id must be positive");
        }
    }

    private void validateSetupId(int setupId) throws Exception
    {
        if (setupId <= 0)
        {
            throw new Exception("[SetupService] Setup id must be positive");
        }
    }

    private void validateSetupPayload(Setup setup) throws Exception
    {
        if (setup == null)
        {
            throw new Exception("[SetupService] Setup payload cannot be empty");
        }

        validateSelection(setup.getGameVersionId(), setup.getTrackId());

        if (setup.getTeamId() <= 0)
        {
            throw new Exception("[SetupService] Team id must be positive");
        }

        if (setup.getTitle() == null || setup.getTitle().isBlank())
        {
            throw new Exception("[SetupService] Setup title cannot be empty");
        }

        if (setup.getSessionType() == null || setup.getControllerType() == null)
        {
            throw new Exception("[SetupService] Session and controller type are required");
        }

        if (setup.getWetWeather() == null)
        {
            throw new Exception("[SetupService] Weather flag is required");
        }
    }
}
