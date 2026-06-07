package com.shootingstarhopper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ShootingStarParser
{
    private static final Pattern ROW_PATTERN = Pattern.compile("<tr\\b[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CELL_PATTERN = Pattern.compile("<td\\b[^>]*>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern WORLD_PATTERN = Pattern.compile("(?:world|w)?\\s*(\\d{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIER_PATTERN = Pattern.compile("(?:\\b(?:tier|size)\\s*[:#-]?\\s*)?\\bT?([1-9])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_AGO_PATTERN = Pattern.compile("(\\d+)\\s*(second|minute|hour|day)s?\\s*ago", Pattern.CASE_INSENSITIVE);

    private final Clock clock;

    public ShootingStarParser()
    {
        this(Clock.systemUTC());
    }

    ShootingStarParser(Clock clock)
    {
        this.clock = clock;
    }

    public List<ShootingStar> parse(String html)
    {
        if (html == null || html.trim().isEmpty())
        {
            return new ArrayList<>();
        }

        String raw = html.trim();
        if (raw.startsWith("["))
        {
            return parseJson(raw);
        }

        return parseHtml(raw);
    }

    private List<ShootingStar> parseHtml(String html)
    {
        List<ShootingStar> stars = new ArrayList<>();
        Matcher rowMatcher = ROW_PATTERN.matcher(html);
        while (rowMatcher.find())
        {
            String rowHtml = rowMatcher.group(1);
            parseRow(extractCells(rowHtml), stripTags(rowHtml)).ifPresent(stars::add);
        }
        return stars;
    }

    public List<ShootingStar> parseJson(String json)
    {
        List<ShootingStar> stars = new ArrayList<>();
        try
        {
            JsonElement root = new JsonParser().parse(json);
            if (!root.isJsonArray())
            {
                return stars;
            }

            JsonArray array = root.getAsJsonArray();
            for (JsonElement element : array)
            {
                if (!element.isJsonObject())
                {
                    continue;
                }

                parseJsonObject(element.getAsJsonObject()).ifPresent(stars::add);
            }
        }
        catch (RuntimeException ignored)
        {
            return new ArrayList<>();
        }
        return stars;
    }

    Optional<ShootingStar> parseRow(List<String> values, String rowText)
    {
        if (values.size() < 3)
        {
            return Optional.empty();
        }

        Optional<Integer> world = parseFirst(values, 1, 0, ShootingStarParser::parseWorld);
        Optional<Integer> tier = parseFirst(values, 2, 1, ShootingStarParser::parseTier);

        if (!world.isPresent() || !tier.isPresent())
        {
            return Optional.empty();
        }

        String time = valueAt(values, 0);
        String location = bestValue(values, 3, 2);
        String region = bestValue(values, 4, 3);
        String scoutedBy = bestValue(values, 5, values.size() - 1);
        boolean leagues = rowText.toLowerCase(Locale.ROOT).contains("league");

        return Optional.of(new ShootingStar(
            parseReportedAt(time).orElse(null),
            time,
            world.get(),
            tier.get(),
            location,
            region,
            scoutedBy,
            leagues
        ));
    }

    static Optional<Integer> parseWorld(String value)
    {
        Matcher matcher = WORLD_PATTERN.matcher(clean(value));
        if (!matcher.find())
        {
            return Optional.empty();
        }

        int world = Integer.parseInt(matcher.group(1));
        return isValidWorld(world) ? Optional.of(world) : Optional.empty();
    }

    static Optional<Integer> parseTier(String value)
    {
        Matcher matcher = TIER_PATTERN.matcher(clean(value));
        if (!matcher.find())
        {
            return Optional.empty();
        }

        int tier = Integer.parseInt(matcher.group(1));
        return tier >= 1 && tier <= 9 ? Optional.of(tier) : Optional.empty();
    }

    private Optional<ShootingStar> parseJsonObject(JsonObject object)
    {
        Optional<Integer> world = optionalInt(object, "world");
        Optional<Integer> tier = optionalInt(object, "tier");
        if (!world.isPresent() || !tier.isPresent() || !isValidWorld(world.get()) || tier.get() < 1 || tier.get() > 9)
        {
            return Optional.empty();
        }

        String timeAgo = optionalString(object, "time")
            .map(value -> value.matches("\\d+") ? value + "m ago" : value)
            .orElse("");

        return Optional.of(new ShootingStar(
            parseReportedAt(timeAgo).orElse(null),
            timeAgo,
            world.get(),
            tier.get(),
            optionalString(object, "loc").orElse(""),
            optionalString(object, "region").orElse(""),
            optionalString(object, "scout").orElse(""),
            optionalBoolean(object, "leagues").orElse(false)
        ));
    }

    private static Optional<Integer> optionalInt(JsonObject object, String key)
    {
        if (!object.has(key) || object.get(key).isJsonNull())
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of(object.get(key).getAsInt());
        }
        catch (RuntimeException ex)
        {
            return parseWorld(object.get(key).getAsString());
        }
    }

    private static Optional<String> optionalString(JsonObject object, String key)
    {
        if (!object.has(key) || object.get(key).isJsonNull())
        {
            return Optional.empty();
        }
        return Optional.of(clean(object.get(key).getAsString()));
    }

    private static Optional<Boolean> optionalBoolean(JsonObject object, String key)
    {
        if (!object.has(key) || object.get(key).isJsonNull())
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of(object.get(key).getAsBoolean());
        }
        catch (RuntimeException ex)
        {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseFirst(
        List<String> values,
        int preferredIndex,
        int fallbackStartIndex,
        Function<String, Optional<Integer>> parser
    )
    {
        Optional<Integer> preferred = parser.apply(valueAt(values, preferredIndex));
        if (preferred.isPresent())
        {
            return preferred;
        }

        for (int i = Math.max(0, fallbackStartIndex); i < values.size(); i++)
        {
            if (i == preferredIndex)
            {
                continue;
            }

            Optional<Integer> parsed = parser.apply(values.get(i));
            if (parsed.isPresent())
            {
                return parsed;
            }
        }

        return Optional.empty();
    }

    public static List<ShootingStar> filterStars(
        List<ShootingStar> stars,
        int minimumTier,
        String preferredRegion,
        boolean hideLeagueWorlds
    )
    {
        List<ShootingStar> filtered = new ArrayList<>();
        String regionFilter = clean(preferredRegion).toLowerCase(Locale.ROOT);
        int safeMinimumTier = Math.max(1, minimumTier);

        for (ShootingStar star : stars)
        {
            if (star.getTier() < safeMinimumTier)
            {
                continue;
            }
            if (hideLeagueWorlds && star.isLeagues())
            {
                continue;
            }
            if (!regionFilter.isEmpty() && !star.getRegion().toLowerCase(Locale.ROOT).contains(regionFilter))
            {
                continue;
            }
            filtered.add(star);
        }

        return filtered;
    }

    private Optional<Instant> parseReportedAt(String value)
    {
        Matcher matcher = TIME_AGO_PATTERN.matcher(clean(value));
        if (!matcher.find())
        {
            return Optional.empty();
        }

        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);
        Duration duration;
        switch (unit)
        {
            case "second":
                duration = Duration.ofSeconds(amount);
                break;
            case "minute":
                duration = Duration.ofMinutes(amount);
                break;
            case "hour":
                duration = Duration.ofHours(amount);
                break;
            case "day":
                duration = Duration.ofDays(amount);
                break;
            default:
                return Optional.empty();
        }

        return Optional.of(Instant.now(clock).minus(duration));
    }

    private static boolean isValidWorld(int world)
    {
        return world >= 300 && world <= 799;
    }

    private static String bestValue(List<String> values, int preferredIndex, int fallbackIndex)
    {
        String preferred = valueAt(values, preferredIndex);
        if (!preferred.isEmpty())
        {
            return preferred;
        }
        return valueAt(values, fallbackIndex);
    }

    private static String valueAt(List<String> values, int index)
    {
        return index >= 0 && index < values.size() ? values.get(index) : "";
    }

    private static String clean(String value)
    {
        return value == null ? "" : decodeHtml(value).replace('\u00a0', ' ').trim().replaceAll("\\s+", " ");
    }

    private static List<String> extractCells(String rowHtml)
    {
        List<String> values = new ArrayList<>();
        Matcher cellMatcher = CELL_PATTERN.matcher(rowHtml);
        while (cellMatcher.find())
        {
            values.add(clean(stripTags(cellMatcher.group(1))));
        }
        return values;
    }

    private static String stripTags(String html)
    {
        return TAG_PATTERN.matcher(html).replaceAll(" ");
    }

    private static String decodeHtml(String value)
    {
        return value
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'");
    }
}
