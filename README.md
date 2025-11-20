# Crackt (Fabric)

Vein-aware ore cracking for Minecraft 1.21.10 on Fabric. Swing a pick at any ore; a shrinking core appears, progress is shared, and the entire connected vein pops once you've contributed enough hits.

## Features
- Shared effort: crack progress is tracked per vein, so multiple players can contribute swings on the same ore.
- Fair durability: every swing costs durability; cracking only breaks as many ores as your pick has durability for.
- Clean drops: final breaking uses the original ore state for correct drops.
- Cluster-aware visuals: the core shrinks to 50% scale and shifts toward the vein mass as you work.
- Client-light: no config screens; only a small BER and payload for syncing the shrinking core.

## Requirements
- Minecraft `1.21.10`
- Fabric Loader `>=0.17.3`
- Fabric API `0.138.3+1.21.10` (or newer for this MC version)
- Java 21

## Install (players)
1. Install Fabric Loader for 1.21.10.
2. Place `crackt-<version>.jar` (from `build/libs`) and Fabric API in your `mods/` folder.
3. Launch the game; no config screen is needed.

## Usage
- Break any log with an axe; progress is tracked per tree.
- Keep swinging the same log until the quota is met; the rest of the tree is felled automatically.
- If your axe breaks mid-timber, only the logs you could afford are felled—finish the remainder with a fresh tool.
- Hold `Shift` while breaking to disable timbering for that action.
- Player-placed log piles without leaves nearby will not be felled.

## Building from source
```sh
./gradlew build
```
Outputs are under `build/libs/` (`-dev` jars are for development, the remapped jar is for players/servers).

## Development notes
- Uses official Mojang mappings; see `AGENTS.md` for cache and inspection tips.
- Ore cracking logic lives in `src/main/java/mod/crackt/OreCracker.java`.

## Known limits
- Hard cap of 256 logs per tree scan.
- No configuration file yet; behavior is fixed (shift-to-skip, leaf check, caps).
