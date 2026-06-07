package com.shootingstarhopper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import net.runelite.api.Client;
import net.runelite.api.World;
import net.runelite.api.WorldType;
import net.runelite.client.callback.ClientThread;

@Singleton
public class WorldHopHelper
{
    private final Client client;
    private final ClientThread clientThread;

    @Inject
    public WorldHopHelper(Client client, ClientThread clientThread)
    {
        this.client = client;
        this.clientThread = clientThread;
    }

    public String copyHopCommand(int world)
    {
        validateWorldNumber(world);
        String command = "::hop " + world;
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(new StringSelection(command), null);
        return command;
    }

    public void hopManually(int world, ShootingStarHopperConfig config)
    {
        validateWorldNumber(world);
        Optional<World> targetWorld = findWorld(world);
        if (!targetWorld.isPresent() || isHiddenWorld(targetWorld.get(), config))
        {
            copyHopCommand(world);
            return;
        }

        clientThread.invokeLater(() ->
        {
            client.openWorldHopper();
            client.hopToWorld(targetWorld.get());
        });
    }

    public boolean isVisibleWorld(int world, ShootingStarHopperConfig config)
    {
        Optional<World> targetWorld = findWorld(world);
        return !targetWorld.isPresent() || !isHiddenWorld(targetWorld.get(), config);
    }

    private Optional<World> findWorld(int world)
    {
        World[] worldList = client.getWorldList();
        if (worldList == null)
        {
            return Optional.empty();
        }

        return Arrays.stream(worldList)
            .filter(candidate -> candidate.getId() == world)
            .findFirst();
    }

    private static boolean isHiddenWorld(World world, ShootingStarHopperConfig config)
    {
        EnumSet<WorldType> types = world.getTypes();
        if (types == null)
        {
            return false;
        }

        if (config.hidePvPWorlds() && WorldType.isPvpWorld(types))
        {
            return true;
        }
        if (config.hideHighRiskWorlds() && types.contains(WorldType.HIGH_RISK))
        {
            return true;
        }
        if (config.hideLeagueWorlds() && types.contains(WorldType.SEASONAL))
        {
            return true;
        }

        boolean members = types.contains(WorldType.MEMBERS);
        if (config.worldMembershipFilter() == WorldMembershipFilter.F2P_ONLY && members)
        {
            return true;
        }
        return config.worldMembershipFilter() == WorldMembershipFilter.P2P_ONLY && !members;
    }

    private static void validateWorldNumber(int world)
    {
        if (world < 300 || world > 799)
        {
            throw new IllegalArgumentException("Invalid OSRS world: " + world);
        }
    }
}
