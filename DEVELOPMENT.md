# Developing Bank Standing

> **Please don't spoil it:** the code holds secret phrases (as hashes), bonuses and
> collection log entries. Please don't publish them.

## Build

```
./gradlew build      # requires JDK 17; the plugin targets Java 11
```

## Development client

`./gradlew run` starts a development client. It adds a **Bank Standing (dev)** sidebar
panel for previewing hitsplats, effects, the cape, the standing unlocks and the collection
log, and for setting your level. It also takes a few testing options. None of this works
outside developer mode:

| Option | Effect |
|---|---|
| `-PxpMultiplier=N` | Every drop is worth N times as much |
| `-PsetXp=N` | Sets your saved XP to N on the first login |
| `-PresetProgress` | Wipes your saved progress on the first login |
| `-PcelebrateEvery=N` | Replays the 99 celebration every N seconds |
| `-PrareEvery=N` | Every Nth drop uses the special hitsplat |
| `-PpreviewSplats=A,B,C` | Each drop shows the next of these sprite IDs, named in chat |

## Releasing

1. Bump `version` in `runelite-plugin.properties` and `BankStandingPanel.VERSION` (a test
   checks they match).
2. Add the player-visible changes to `CHANGELOG.md`. Leave the secrets out.
3. Point the Plugin Hub's `plugins/bank-standing` file at the new commit.
