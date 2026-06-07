package com.shootingstarhopper;

public enum WorldMembershipFilter
{
    ALL("All"),
    F2P_ONLY("F2P only"),
    P2P_ONLY("P2P only");

    private final String displayName;

    WorldMembershipFilter(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
