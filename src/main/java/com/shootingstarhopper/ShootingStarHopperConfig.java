package com.shootingstarhopper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(ShootingStarHopperConfig.GROUP)
public interface ShootingStarHopperConfig extends Config
{
    String GROUP = "shootingStarHopper";

    @ConfigItem(
        keyName = "enableAutoRefresh",
        name = "Auto refresh",
        description = "Refresh the star list automatically. The minimum interval is one minute.",
        position = 0
    )
    default boolean enableAutoRefresh()
    {
        return true;
    }

    @Range(min = 60)
    @ConfigItem(
        keyName = "refreshIntervalSeconds",
        name = "Refresh interval",
        description = "Seconds between automatic refreshes. Values below 60 are treated as 60.",
        position = 1
    )
    default int refreshIntervalSeconds()
    {
        return 60;
    }

    @Range(min = 1, max = 9)
    @ConfigItem(
        keyName = "minimumTier",
        name = "Minimum tier",
        description = "Hide stars below this tier.",
        position = 2
    )
    default int minimumTier()
    {
        return 1;
    }

    @ConfigItem(
        keyName = "hidePvPWorlds",
        name = "Hide PvP worlds",
        description = "Hide PvP worlds when RuneLite world metadata is available.",
        position = 3
    )
    default boolean hidePvPWorlds()
    {
        return true;
    }

    @ConfigItem(
        keyName = "hideHighRiskWorlds",
        name = "Hide high risk worlds",
        description = "Hide high risk worlds when RuneLite world metadata is available.",
        position = 4
    )
    default boolean hideHighRiskWorlds()
    {
        return true;
    }

    @ConfigItem(
        keyName = "hideLeagueWorlds",
        name = "Hide League worlds",
        description = "Hide seasonal or League worlds.",
        position = 5
    )
    default boolean hideLeagueWorlds()
    {
        return true;
    }

    @ConfigItem(
        keyName = "worldMembershipFilter",
        name = "World type",
        description = "Show all worlds, only free-to-play worlds, or only members worlds.",
        position = 6
    )
    default WorldMembershipFilter worldMembershipFilter()
    {
        return WorldMembershipFilter.ALL;
    }

    @ConfigItem(
        keyName = "preferredRegion",
        name = "Preferred region",
        description = "Optional region text filter. Leave blank to show all regions.",
        position = 7
    )
    default String preferredRegion()
    {
        return "";
    }
}
