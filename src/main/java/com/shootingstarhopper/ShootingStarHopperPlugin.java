package com.shootingstarhopper;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@PluginDescriptor(
    name = "Shooting Star Hopper",
    description = "Displays OSRSPortal shooting stars and assists manual world selection",
    tags = {"shooting", "stars", "mining", "world", "hopper"}
)
public class ShootingStarHopperPlugin extends Plugin
{
    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ScheduledExecutorService executorService;

    @Inject
    private ShootingStarHopperConfig config;

    @Inject
    private ShootingStarService starService;

    @Inject
    private WorldHopHelper worldHopHelper;

    private ShootingStarPanel panel;
    private NavigationButton navigationButton;
    private ScheduledFuture<?> refreshTask;

    @Provides
    ShootingStarHopperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ShootingStarHopperConfig.class);
    }

    @Override
    protected void startUp()
    {
        panel = new ShootingStarPanel(
            config,
            this::copyHopCommand,
            this::hopManually,
            this::requestRefresh
        );

        navigationButton = NavigationButton.builder()
            .tooltip("Shooting Star Hopper")
            .icon(createIcon())
            .priority(6)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navigationButton);
        scheduleRefresh();
        requestRefresh();
    }

    @Override
    protected void shutDown()
    {
        cancelRefresh();
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
        }
        navigationButton = null;
        panel = null;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!ShootingStarHopperConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }

        if (panel != null)
        {
            panel.refreshActionMode();
        }
        scheduleRefresh();
        requestRefresh();
    }

    public void requestRefresh()
    {
        ShootingStarPanel currentPanel = panel;
        if (currentPanel != null)
        {
            SwingUtilities.invokeLater(currentPanel::showRefreshing);
        }
        executorService.submit(this::refreshStars);
    }

    private void refreshStars()
    {
        try
        {
            List<ShootingStar> stars = starService.fetchStars();
            Instant updatedAt = starService.getLastFetchTime();
            if (updatedAt == null)
            {
                updatedAt = Instant.now();
            }

            ShootingStarPanel currentPanel = panel;
            Instant finalUpdatedAt = updatedAt;
            clientThread.invoke(() ->
            {
                List<ShootingStar> filteredStars = filterStars(stars);
                SwingUtilities.invokeLater(() ->
                {
                    if (currentPanel != null)
                    {
                        currentPanel.updateStars(filteredStars, finalUpdatedAt);
                    }
                });
            });
        }
        catch (Exception ex)
        {
            log.warn("Unable to refresh shooting stars", ex);
            ShootingStarPanel currentPanel = panel;
            SwingUtilities.invokeLater(() ->
            {
                if (currentPanel != null)
                {
                    currentPanel.showError(ex.getMessage() == null ? "Unable to refresh" : ex.getMessage());
                }
            });
        }
    }

    private List<ShootingStar> filterStars(List<ShootingStar> stars)
    {
        List<ShootingStar> filtered = ShootingStarParser.filterStars(
            stars,
            config.minimumTier(),
            config.preferredRegion(),
            config.hideLeagueWorlds()
        );

        List<ShootingStar> visible = new ArrayList<>();
        for (ShootingStar star : filtered)
        {
            if (worldHopHelper.isVisibleWorld(star.getWorld(), config))
            {
                visible.add(star);
            }
        }
        return visible;
    }

    private void copyHopCommand(ShootingStar star)
    {
        try
        {
            String command = worldHopHelper.copyHopCommand(star.getWorld());
            showStatus("Copied " + command);
        }
        catch (Exception ex)
        {
            showStatus("Unable to copy hop command: " + ex.getMessage());
        }
    }

    private void hopManually(ShootingStar star)
    {
        try
        {
            worldHopHelper.hopManually(star.getWorld(), config);
            showStatus("Selected world " + star.getWorld());
        }
        catch (Exception ex)
        {
            showStatus("Unable to hop: " + ex.getMessage());
        }
    }

    private void showStatus(String message)
    {
        ShootingStarPanel currentPanel = panel;
        if (currentPanel != null)
        {
            SwingUtilities.invokeLater(() -> currentPanel.showStatus(message));
        }
    }

    private void scheduleRefresh()
    {
        cancelRefresh();
        if (!config.enableAutoRefresh())
        {
            return;
        }

        long interval = Math.max(60, config.refreshIntervalSeconds());
        refreshTask = executorService.scheduleWithFixedDelay(this::refreshStars, interval, interval, TimeUnit.SECONDS);
    }

    private void cancelRefresh()
    {
        if (refreshTask != null)
        {
            refreshTask.cancel(false);
            refreshTask = null;
        }
    }

    private static BufferedImage createIcon()
    {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(38, 42, 50));
        graphics.fillOval(1, 1, 14, 14);
        graphics.setColor(new Color(244, 204, 92));
        int[] x = {8, 10, 15, 11, 12, 8, 4, 5, 1, 6};
        int[] y = {1, 6, 6, 9, 14, 11, 14, 9, 6, 6};
        graphics.fillPolygon(x, y, x.length);
        graphics.dispose();
        return image;
    }
}
