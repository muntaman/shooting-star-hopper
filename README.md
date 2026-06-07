# Shooting Star Hopper

Shooting Star Hopper is a RuneLite Plugin Hub style external plugin for Old School RuneScape. It displays active shooting stars from OSRSPortal's Shooting Stars Tracker and helps the player manually select a world.

## Compliance Notes

This plugin is intentionally assistive only:

- It does not automate gameplay.
- It does not automatically cycle worlds.
- It does not click, mine, pathfind, or interact with game objects.
- World hopping is only attempted after an explicit user button click.
- If RuneLite cannot safely find or use the selected world, it copies `::hop <world>` to the clipboard instead.
- The OSRSPortal tracker is refreshed at most once per minute and results are cached.

## Features

- Sidebar panel listing active stars with tier, world, location, and time.
- Manual refresh button backed by a one-minute service-side rate limit.
- Configurable minimum tier and region filtering.
- Optional hiding for PvP, high risk, and League/seasonal worlds when RuneLite world metadata is available.
- Per-row manual hop buttons with a clipboard fallback when a direct hop is unavailable.
- Optional filtering for all worlds, free-to-play worlds, or members worlds.
- Modular fetch and parse classes so a future OSRSPortal JSON API can replace the HTML parser.

## Data Source

The initial source is:

`https://osrsportal.com/shooting-stars-tracker`

OSRSPortal's API page currently indicates that API endpoints are coming soon. `ShootingStarService` currently reads the same active-star JSON used by the public tracker page, while keeping fetching and parsing separate so this can be replaced with a documented API later.

## Development

Requirements:

- Java 11
- IntelliJ IDEA Community Edition or another Gradle-capable Java IDE
- RuneLite external plugin development environment

Useful Gradle tasks:

```bash
./gradlew test
./gradlew run
./gradlew shadowJar
```

On Windows without a checked-in Gradle wrapper, use your installed Gradle:

```powershell
gradle test
gradle run
```

The `run` task launches RuneLite in developer mode with this external plugin loaded.

## Project Structure

- `ShootingStarHopperPlugin` registers the sidebar panel and schedules refreshes.
- `ShootingStarHopperConfig` contains user-facing configuration.
- `ShootingStar` is the immutable data model.
- `ShootingStarService` fetches and caches tracker data.
- `ShootingStarParser` parses HTML rows and ignores malformed data.
- `ShootingStarPanel` renders the compact Swing sidebar table and row hop buttons.
- `WorldHopHelper` validates selected worlds and either invokes RuneLite's explicit world-hop API from the client thread or copies the hop command as a fallback.

## Testing

Parser tests use static sample HTML only and do not call the live OSRSPortal website.
