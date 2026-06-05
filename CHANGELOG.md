# Changelog for EverFurnace: Farmer's Delight (Forge 1.20.1)

All notable changes to EverFurnace: Farmer's Delight will be documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.0] - 2026-06-01
### Added
- Cooking pots now keep cooking while you're away. When you come back, anything
  that would have finished is done and waiting for you.
- Stoves also keep cooking while you're offline.
- Skillets keep cooking while you're away too. If you left a stack of food on a
  hot skillet, several pieces can finish during one trip away — not just one.
- Cooking pots, stoves, and skillets added by other mods — ones built on top of
  Farmer's Delight's own cooking blocks — get the same offline catch-up, not just
  the standard Farmer's Delight blocks.
- When cooking catches up while you were gone, you'll get a little burst of
  particles, a sizzle sound, and a quick message so you know food finished.
- A toggle for the catch-up message: type "/everfurnacefd message off" to hide it
  (or "on" to bring it back, or just "/everfurnacefd message" to check). This is
  your own setting — it only affects you, and it's remembered between sessions.
