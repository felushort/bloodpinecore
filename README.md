# BloodpineCore

A custom Minecraft Spigot/Paper plugin for the Bloodpine survival server.

## Features

### Core Systems
- **Player Statistics System** - Track kills, deaths, playtime, and more
- **Rebirth System** - Prestige system with rewards
- **Bounty System** - Place bounties on players
- **Kill Streaks** - Rewards for consecutive kills
- **Token Economy** - Custom token system with shop
- **Dynamic Scoreboard** - Live player statistics display
- **Boost System** - Temporary multipliers for XP, damage, etc.
- **Leaderboards** - Competitive rankings
- **Marked System** - Mark players for PvP tracking

### New Enhanced Features ✨
- **Achievement System** - Unlock 16 achievements and earn token rewards 🎯
- **Daily Rewards** - Login daily for token bonuses with streak multipliers (up to +6 tokens) 🎁
- **Combat Statistics** - Track damage dealt/taken, hits landed, critical hits, and performance ratios 📊
- **Command Cooldowns** - Anti-spam protection for all commands ⏱️
- **Configuration Validation** - Automatic validation on startup with warnings and errors 🛡️

### Performance & Quality Improvements ⚡
- **Async Data Operations** - Non-blocking save/load to prevent lag spikes
- **Display Caching** - Smart caching to skip redundant nametag updates
- **Thread-Safe Operations** - ConcurrentHashMap throughout for safe concurrent access
- **Auto-Save System** - Periodic automatic data persistence (configurable interval)
- **Enhanced Error Handling** - Comprehensive try-catch blocks and logging
- **Input Validation** - Safe parsing and validation of all user input
- **Base Command Class** - Reduced code duplication across all commands

## Building

This plugin uses Maven. To build:

```bash
mvn clean package
```

The compiled JAR will be in the `target/` directory.

## Requirements

- Java 17 or higher
- Spigot or Paper 1.21.4+

## Installation

1. Download the JAR file from releases
2. Place it in your server's `plugins/` folder
3. Restart the server
4. Configure settings in `plugins/BloodpineCore/config.yml`

## Development

Built for Bloodpine Minecraft Survival Server

### Code Quality & Security
- ✅ **CodeQL Security Scan**: 0 vulnerabilities
- ✅ **Code Review**: All issues resolved
- ✅ **Thread-Safe**: ConcurrentHashMap used throughout
- ✅ **Error Handling**: Comprehensive try-catch blocks
- ✅ **Input Validation**: Safe parsing of all user input
- ✅ **Configuration Validation**: Automatic validation on startup

### Performance Optimizations
- Async data operations to prevent lag
- Smart display caching to reduce updates
- Auto-save system for data persistence
- Thread-safe concurrent collections
- Optimized update loops

### Architecture
- **53 Java Classes** (10 new classes added)
- **Base Command Pattern** - Reduces code duplication
- **Manager Pattern** - Clean separation of concerns
- **Event-Driven** - Bukkit event listeners
- **Configuration-Driven** - Extensive config.yml settings

## New Commands

- `/achievements` or `/ach` - View unlocked achievements
- `/daily` or `/dr` - Claim daily login rewards
- `/combatstats` or `/cs` - View detailed combat statistics

## Configuration

New configuration options in `config.yml`:

```yaml
# Daily Rewards
daily-rewards:
  enabled: true
  base-tokens: 3
  max-streak-bonus: 6

# Command Cooldowns (in seconds)
cooldowns:
  bounty: 30
  paytokens: 10
  payhearts: 10
  tshop: 5
  daily: 5

# Auto-save interval in ticks (6000 ticks = 5 minutes)
auto-save-interval: 6000
```

## License

Copyright © 2026 Bloodpine Network
