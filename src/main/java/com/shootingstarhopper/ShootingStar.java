package com.shootingstarhopper;

import java.time.Instant;
import java.util.Objects;

public final class ShootingStar
{
    private final Instant reportedAt;
    private final String timeAgo;
    private final int world;
    private final int tier;
    private final String location;
    private final String region;
    private final String scoutedBy;
    private final boolean leagues;

    public ShootingStar(
        Instant reportedAt,
        String timeAgo,
        int world,
        int tier,
        String location,
        String region,
        String scoutedBy,
        boolean leagues
    )
    {
        this.reportedAt = reportedAt;
        this.timeAgo = clean(timeAgo);
        this.world = world;
        this.tier = tier;
        this.location = clean(location);
        this.region = clean(region);
        this.scoutedBy = clean(scoutedBy);
        this.leagues = leagues;
    }

    public Instant getReportedAt()
    {
        return reportedAt;
    }

    public String getTimeAgo()
    {
        return timeAgo;
    }

    public int getWorld()
    {
        return world;
    }

    public int getTier()
    {
        return tier;
    }

    public String getLocation()
    {
        return location;
    }

    public String getRegion()
    {
        return region;
    }

    public String getScoutedBy()
    {
        return scoutedBy;
    }

    public boolean isLeagues()
    {
        return leagues;
    }

    private static String clean(String value)
    {
        return value == null ? "" : value.trim();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ShootingStar))
        {
            return false;
        }
        ShootingStar that = (ShootingStar) o;
        return world == that.world
            && tier == that.tier
            && leagues == that.leagues
            && Objects.equals(reportedAt, that.reportedAt)
            && Objects.equals(timeAgo, that.timeAgo)
            && Objects.equals(location, that.location)
            && Objects.equals(region, that.region)
            && Objects.equals(scoutedBy, that.scoutedBy);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(reportedAt, timeAgo, world, tier, location, region, scoutedBy, leagues);
    }
}
