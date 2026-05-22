package com.f1setups.services;

import com.f1setups.dao.SetupDao;
import com.f1setups.models.ControllerType;
import com.f1setups.models.SessionType;
import com.f1setups.models.Setup;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SetupServiceTest
{
    @Test
    void createUserSetupOverwritesClientSuppliedUserId() throws Exception
    {
        SetupDao setupDao = mock(SetupDao.class);
        SetupService setupService = new SetupService(setupDao);
        Setup setup = createSetup(0, 999);

        when(setupDao.save(setup)).thenReturn(Optional.of(setup));

        Setup savedSetup = setupService.createUserSetup(7, setup);

        assertEquals(7, savedSetup.getUserId());
        verify(setupDao).save(setup);
    }

    @Test
    void updateUserSetupChecksOwnershipAndOverwritesIdAndUserId() throws Exception
    {
        SetupDao setupDao = mock(SetupDao.class);
        SetupService setupService = new SetupService(setupDao);
        Setup existingSetup = createSetup(25, 7);
        Setup updatePayload = createSetup(999, 999);

        when(setupDao.getByIdAndUserId(25, 7)).thenReturn(Optional.of(existingSetup));
        when(setupDao.updateFull(updatePayload)).thenReturn(Optional.of(updatePayload));

        Setup updatedSetup = setupService.updateUserSetup(7, 25, updatePayload);

        assertEquals(25, updatedSetup.getId());
        assertEquals(7, updatedSetup.getUserId());
        verify(setupDao).getByIdAndUserId(25, 7);
        verify(setupDao).updateFull(updatePayload);
    }

    @Test
    void updateUserSetupFailsWhenSetupIsNotOwnedByAuthenticatedUser()
    {
        SetupDao setupDao = mock(SetupDao.class);
        SetupService setupService = new SetupService(setupDao);
        Setup updatePayload = createSetup(25, 999);

        when(setupDao.getByIdAndUserId(25, 7)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> setupService.updateUserSetup(7, 25, updatePayload));
        verify(setupDao).getByIdAndUserId(25, 7);
        verify(setupDao, never()).updateFull(any());
    }

    @Test
    void deleteUserSetupUsesAuthenticatedUserIdForOwnership() throws Exception
    {
        SetupDao setupDao = mock(SetupDao.class);
        SetupService setupService = new SetupService(setupDao);

        when(setupDao.deleteByIdAndUserId(25, 7)).thenReturn(true);

        setupService.deleteUserSetup(7, 25);

        verify(setupDao).deleteByIdAndUserId(25, 7);
    }

    @Test
    void deleteUserSetupFailsWhenSetupIsNotOwnedByAuthenticatedUser()
    {
        SetupDao setupDao = mock(SetupDao.class);
        SetupService setupService = new SetupService(setupDao);

        when(setupDao.deleteByIdAndUserId(25, 7)).thenReturn(false);

        assertThrows(Exception.class, () -> setupService.deleteUserSetup(7, 25));
        verify(setupDao).deleteByIdAndUserId(25, 7);
    }

    private Setup createSetup(int id, int userId)
    {
        return new Setup(
                id,
                userId,
                4,
                19,
                3,
                "Monza Race Setup",
                "Stable rear end",
                SessionType.RACE,
                ControllerType.WHEEL,
                false,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                20,
                25,
                60,
                55,
                40,
                2.5f,
                1.0f,
                0.1f,
                0.2f,
                30,
                20,
                12,
                8,
                25,
                35,
                100,
                56,
                23.0f,
                23.0f,
                21.5f,
                21.5f
        );
    }
}
