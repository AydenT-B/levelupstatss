# Level Up Stats (NeoForge 1.21.1)

Spend your normal XP levels on character stats. Press **K** (rebindable under Controls > Level Up Stats) to open the menu.

## Stats
| Stat | Effect per level |
|---|---|
| Speed | +4% movement speed (attribute change, not a potion) |
| Jump Power | +5% jump strength, plus softer landings |
| Strength | +0.5 melee damage |
| Luck | +1 Fortune and Looting for every 2 levels (rounded up), no tool needed, stacks with enchanted gear; also +0.5 Luck attribute |
| Constitution | +1 max heart |
| Intelligence | XP orbs worth +0.5x more each level (level 2 = x2) |

## Cost and cap
- The Nth point in a stat costs N XP levels (1st = 1, 2nd = 2, ...).
- Every stat starts capped at level 10. Once all six stats are at the cap, the **Prestige** button unlocks and raises the cap by 5 (stats are kept).

## Building
1. Install JDK 21.
2. Add a Gradle wrapper (copy `gradlew`, `gradlew.bat` and the `gradle/` folder from the official NeoForge MDK, or run `gradle wrapper --gradle-version 8.10.2`).
3. `./gradlew runClient` to test, `./gradlew build` for the jar in `build/libs/`.

## Tuning
All balance numbers are constants in `src/main/java/com/levelupstats/StatEffects.java`; cap values are in `PlayerStats.java`.

## Notes
- Looting with an empty hand uses a simplified bonus (extra count on stackable drops). With any item in hand it uses the real Looting path, including rare drop chances.
- Speed also slightly widens the field of view, as vanilla speed changes do.
