# Compliance Check

This checklist is for RuneLite Plugin Hub submission readiness. It is not legal advice and does not guarantee approval; RuneLite reviewers make the final decision.

## Sources Checked

- RuneLite Plugin Hub README: external plugins are reviewed to ensure they are not malicious and do not break Jagex rules.
- Jagex "Macro and client features not permitted": non-permissible features include direct game-world communication, generated mouse/key input, excessive repeated website requests, busy-world assistance, and unacceptable third-party client features.
- RuneLite Plugin Hub page: Plugin Hub plugins are verified by RuneLite developers for Jagex third-party client rule compliance and malicious-code concerns.

## Current Status

No hard non-compliance found in the current code against the rules reviewed.

Likely compliant with core automation rules:

- No generated mouse input, keyboard input, or OS-level automation.
- No menu entry modification.
- No game object, NPC, player, tile, pathfinding, mining, or interaction automation.
- No automatic world cycling.
- No repeated hopping.
- World selection requires an explicit user click on a row button.
- Refresh is scheduled no more than once per minute.
- Network responses are cached by `ShootingStarService`.
- PvP, high-risk, League, F2P, and P2P filters are display filters only.

Review notes to mention clearly in a Plugin Hub PR:

- The plugin reads OSRSPortal tracker data from the same JSON endpoint used by the public tracker page. OSRSPortal does not currently document this as a public API.
- The plugin can call RuneLite's `Client.hopToWorld` after a row button click. RuneLite's built-in World Hopper plugin already supports user-initiated hopping from its sidebar, so user-clicked hopping is not inherently prohibited. This plugin must not add automatic cycling, repeated retries, or busy-world entry assistance.
- The plugin includes shooting-star world information, but does not monitor or assist entry into busy worlds. Avoid adding queue/full-world retry logic.

Plugin Hub packaging note:

- Direct third-party dependencies should be avoided where possible because Plugin Hub requires dependency verification for dependencies that are not transitive dependencies of `runelite-client`.
- This project currently avoids an explicit `jsoup` dependency; JSON parsing uses Gson from RuneLite's dependency tree.

Recommended submission posture:

- Keep README language explicit that the plugin displays third-party tracker data and performs at most one selected-world action per user click.
- Explain that the row button is equivalent to manually selecting a world from RuneLite's world hopper: one user click, one selected-world action.
- Do not add automatic cycling, hotkeys, repeated hop retries, mining/object interaction, pathfinding, or menu swaps.

Strictest possible fallback mode:

- Replace direct `Client.hopToWorld` calls with clipboard-only `::hop <world>` copying.
- Keep the row button text as the world number, but make the click copy the command instead of hopping.
- This would reduce functionality, but it remains available if reviewers specifically object to direct hopping in an external plugin.

## Code Paths Reviewed

- `ShootingStarHopperPlugin`: schedules refreshes, registers sidebar, applies filters, and delegates row actions.
- `ShootingStarService`: fetches OSRSPortal data, enforces one-minute minimum fetch interval, and caches results.
- `ShootingStarParser`: parses HTML or JSON into display-only data.
- `ShootingStarPanel`: displays data and exposes one row button per selected world.
- `WorldHopHelper`: validates world numbers, filters unsafe world types, copies fallback command, and invokes RuneLite world hopping only from the explicit row action path.

## Submission Notes

Suggested PR wording:

> Shooting Star Hopper displays active shooting stars from OSRSPortal's public Shooting Stars Tracker and lets the user manually select a world from a sidebar table. It does not mine, pathfind, click game objects, generate input, cycle worlds, or automate gameplay. The tracker refresh is rate-limited to once per minute and cached. Each world action requires an explicit row button click.
