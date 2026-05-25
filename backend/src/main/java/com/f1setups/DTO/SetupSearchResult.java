package com.f1setups.DTO;

import com.f1setups.models.Setup;

/**
 * Lightweight search response that keeps the setup payload together with display-only author data.
 */
public class SetupSearchResult
{
    public Setup setup;
    public String username;

    public SetupSearchResult()
    {
        this.setup = null;
        this.username = "";
    }

    public SetupSearchResult(Setup setup, String username)
    {
        this.setup = setup;
        this.username = username;
    }
}
