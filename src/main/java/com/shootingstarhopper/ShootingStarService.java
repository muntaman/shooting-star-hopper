package com.shootingstarhopper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
public class ShootingStarService
{
    static final String TRACKER_URL = "https://osrsportal.com/shooting-stars-tracker";
    static final String ACTIVE_STARS_URL = "https://osrsportal.com/activestars-foxtrot";
    private static final Duration MINIMUM_FETCH_INTERVAL = Duration.ofMinutes(1);
    private static final String USER_AGENT = "Mozilla/5.0 RuneLite Shooting Star Hopper/0.1 (+https://runelite.net/plugin-hub)";

    private final OkHttpClient client;
    private final ShootingStarParser parser;

    private Instant lastFetchTime;
    private List<ShootingStar> cachedStars = Collections.emptyList();
    private String lastRawHtml = "";

    @Inject
    public ShootingStarService(OkHttpClient client, ShootingStarParser parser)
    {
        this.client = client.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build();
        this.parser = parser;
    }

    public synchronized List<ShootingStar> fetchStars() throws IOException
    {
        if (!canFetchNow())
        {
            return cachedCopy();
        }

        String rawData = fetchRawActiveStars();
        List<ShootingStar> parsedStars = parser.parse(rawData);
        lastFetchTime = Instant.now();
        lastRawHtml = rawData;
        cachedStars = Collections.unmodifiableList(new ArrayList<>(parsedStars));
        return cachedCopy();
    }

    synchronized List<ShootingStar> parseRawHtml(String html)
    {
        lastRawHtml = html == null ? "" : html;
        cachedStars = Collections.unmodifiableList(new ArrayList<>(parser.parse(lastRawHtml)));
        lastFetchTime = Instant.now();
        return cachedCopy();
    }

    public synchronized Instant getLastFetchTime()
    {
        return lastFetchTime;
    }

    public synchronized String getLastRawHtml()
    {
        return lastRawHtml;
    }

    private boolean canFetchNow()
    {
        return lastFetchTime == null || Duration.between(lastFetchTime, Instant.now()).compareTo(MINIMUM_FETCH_INTERVAL) >= 0;
    }

    private String fetchRawActiveStars() throws IOException
    {
        Request request = new Request.Builder()
            .url(ACTIVE_STARS_URL)
            .header("User-Agent", USER_AGENT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "https://osrsportal.com")
            .header("Referer", TRACKER_URL)
            .header("Authorization", "Bearer " + createActiveStarsToken())
            .get()
            .build();

        Call call = client.newCall(request);
        try (Response response = call.execute())
        {
            if (!response.isSuccessful())
            {
                throw new IOException("OSRSPortal returned HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null)
            {
                throw new IOException("OSRSPortal returned an empty response");
            }

            return body.string();
        }
    }

    private static String createActiveStarsToken()
    {
        long exp = Instant.now().plus(Duration.ofMinutes(5)).getEpochSecond();
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = String.format(Locale.ROOT, "{\"data\":\"osrs_stars\",\"exp\":%d}", exp);
        return base64(header) + "." + base64(payload) + ".";
    }

    private static String base64(String value)
    {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private List<ShootingStar> cachedCopy()
    {
        return new ArrayList<>(cachedStars);
    }
}
