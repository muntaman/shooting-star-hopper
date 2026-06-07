package com.shootingstarhopper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class ShootingStarParserTest
{
    private final ShootingStarParser parser = new ShootingStarParser(
        Clock.fixed(Instant.parse("2026-06-07T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    public void parsesSampleRows()
    {
        String html = "<table><tbody>"
            + "<tr><th>Time</th><th>World</th><th>Size</th><th>Location</th><th>Region</th><th>Scouted by</th></tr>"
            + "<tr><td>5 minutes ago</td><td>W523</td><td>T7</td><td>Crafting Guild</td><td>Asgarnia</td><td>Alice</td></tr>"
            + "<tr><td>1 hour ago</td><td>World 444</td><td>Size: T9</td><td>Prifddinas</td><td>Tirannwn</td><td>Bob</td></tr>"
            + "</tbody></table>";

        List<ShootingStar> stars = parser.parse(html);

        assertEquals(2, stars.size());
        assertEquals(523, stars.get(0).getWorld());
        assertEquals(7, stars.get(0).getTier());
        assertEquals("Crafting Guild", stars.get(0).getLocation());
        assertEquals("Asgarnia", stars.get(0).getRegion());
        assertEquals("Alice", stars.get(0).getScoutedBy());
        assertEquals(Instant.parse("2026-06-07T11:55:00Z"), stars.get(0).getReportedAt());
        assertEquals(444, stars.get(1).getWorld());
        assertEquals(9, stars.get(1).getTier());
    }

    @Test
    public void ignoresMalformedRows()
    {
        String html = "<table><tbody>"
            + "<tr><td>5 minutes ago</td><td>not a world</td><td>T7</td><td>Crafting Guild</td></tr>"
            + "<tr><td>5 minutes ago</td><td>W523</td><td>bad tier</td><td>Crafting Guild</td></tr>"
            + "<tr><td>5 minutes ago</td><td>W523</td></tr>"
            + "<tr><td>2 minutes ago</td><td>W302</td><td>T4</td><td>Yanille</td><td>Kandarin</td><td>Scout</td></tr>"
            + "</tbody></table>";

        List<ShootingStar> stars = parser.parse(html);

        assertEquals(1, stars.size());
        assertEquals(302, stars.get(0).getWorld());
        assertEquals(4, stars.get(0).getTier());
    }

    @Test
    public void parsesOsrsPortalJsonRows()
    {
        String json = "["
            + "{\"time\":12,\"world\":523,\"tier\":7,\"loc\":\"Crafting Guild\",\"region\":\"Asgarnia\",\"scout\":\"Alice\"},"
            + "{\"time\":\"3\",\"world\":444,\"tier\":9,\"loc\":\"Prifddinas\",\"region\":\"Tirannwn\",\"scout\":\"Bob\"}"
            + "]";

        List<ShootingStar> stars = parser.parse(json);

        assertEquals(2, stars.size());
        assertEquals("12m ago", stars.get(0).getTimeAgo());
        assertEquals(523, stars.get(0).getWorld());
        assertEquals(7, stars.get(0).getTier());
        assertEquals("Crafting Guild", stars.get(0).getLocation());
        assertEquals("3m ago", stars.get(1).getTimeAgo());
    }

    @Test
    public void parsesTierWithoutConfusingWorldNumbers()
    {
        Optional<Integer> worldTier = ShootingStarParser.parseTier("World 523");
        Optional<Integer> t7 = ShootingStarParser.parseTier("T7");
        Optional<Integer> tier9 = ShootingStarParser.parseTier("Tier: 9");
        Optional<Integer> size8 = ShootingStarParser.parseTier("Size - T8");

        assertFalse(worldTier.isPresent());
        assertEquals(Integer.valueOf(7), t7.get());
        assertEquals(Integer.valueOf(9), tier9.get());
        assertEquals(Integer.valueOf(8), size8.get());
    }

    @Test
    public void parsesWorldsAndRejectsInvalidNumbers()
    {
        assertEquals(Integer.valueOf(523), ShootingStarParser.parseWorld("W523").get());
        assertEquals(Integer.valueOf(444), ShootingStarParser.parseWorld("World 444").get());
        assertFalse(ShootingStarParser.parseWorld("W2").isPresent());
        assertFalse(ShootingStarParser.parseWorld("World 999").isPresent());
    }

    @Test
    public void filtersByTierRegionAndLeagueWorlds()
    {
        List<ShootingStar> stars = Arrays.asList(
            new ShootingStar(null, "now", 301, 3, "Varrock", "Misthalin", "A", false),
            new ShootingStar(null, "now", 302, 7, "Crafting Guild", "Asgarnia", "B", false),
            new ShootingStar(null, "now", 303, 8, "Leagues area", "Asgarnia League", "C", true)
        );

        List<ShootingStar> filtered = ShootingStarParser.filterStars(stars, 7, "asgarnia", true);

        assertEquals(1, filtered.size());
        assertEquals(302, filtered.get(0).getWorld());
        assertTrue(filtered.get(0).getRegion().contains("Asgarnia"));
    }
}
