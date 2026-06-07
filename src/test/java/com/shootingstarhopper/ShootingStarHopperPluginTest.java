package com.shootingstarhopper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ShootingStarHopperPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ShootingStarHopperPlugin.class);
        RuneLite.main(args);
    }
}
