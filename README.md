# Bank Standing

The skill every OSRS player has secretly been training for years, now with a level.

Stand near a bank and do nothing. After a short while you start earning Bank Standing XP,
one drop at a time, all the way to 99. Reach the top and you'll wear a cape while you stand to
prove it.

> **Status:** in development. Not yet on the Plugin Hub.

## How it works

- Stand next to any bank: booths, chests, deposit boxes, bankers. As you level up you can
  stand further away, up to 9 tiles at 99.
- Stay idle for 30 seconds and XP starts dropping. Higher levels start sooner.
- Walk, use the bank, or gain XP in any real skill and your stand ends. The timer starts
  again from zero.
- Chatting, looking around and checking your tabs are all fine. Bank standing is a social
  skill.

The game's own logout timer still applies, so you will need to stay at your keyboard.

## What you get

- A Bank Standing skill tile you can place anywhere on screen. Click it to see what each
  level unlocks.
- XP drops and level-up messages, just like a real skill
- A side panel with your level, XP, XP per hour, session and daily standing time, and how
  long you've spent at each bank
- Unlocks as you level: a rug to stand on, a seat, titles, idle fidgets, bankers who
  notice you, a quick look in the bank that doesn't end your stand, and at 99 a cape and a
  cape emote. They're only visible to you, and each one can be turned off in the settings.
- XP that keeps counting past 99, all the way to 200m

Progress is saved separately for each of your accounts.

## Build

```
./gradlew build      # requires JDK 17; the plugin targets Java 11
```

`./gradlew run` starts a development client. It adds a **Bank Standing (dev)** sidebar
panel for previewing hitsplats, effects, the cape and the standing unlocks, and for setting your level, and takes a
few testing options. None of this works outside developer mode:

| Option | Effect |
|---|---|
| `-PxpMultiplier=N` | Every drop is worth N times as much |
| `-PsetXp=N` | Sets your saved XP to N on the first login |
| `-PresetProgress` | Wipes your saved progress on the first login |
| `-PcelebrateEvery=N` | Replays the 99 celebration every N seconds |
| `-PrareEvery=N` | Every Nth drop uses the special hitsplat |
| `-PpreviewSplats=A,B,C` | Each drop shows the next of these sprite IDs, named in chat |

## Credits

Thanks to the MERC clan, Shalli, FkXP, someusert8, Cnorthor and Zerek.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
