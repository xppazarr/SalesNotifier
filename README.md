# SalesNotifier

A Fabric mod for Minecraft 1.21.11 that sends Discord webhook notifications when Black Market restocks or Merchant replenishes items.

## Features

- **Black Market Detection**: Automatically detects when the Black Market restocks and sends a notification
- **Merchant Detection**: Detects when NPCs are back in stock and monitors for replenishment details
- **Discord Webhooks**: Sends rich embed notifications to your Discord server
- **In-Game Configuration**: Easy-to-use commands to configure the mod

## Installation

1. Download the mod JAR file
2. Place it in your `.minecraft/mods` folder
3. Launch Minecraft with Fabric

## Configuration

### Setting up Discord Webhook

1. Create a webhook in your Discord server (Server Settings → Integrations → Webhooks)
2. Copy the webhook URL
3. In-game, use the command: `/salesnotifier webhook <your-webhook-url>`

### Commands

- `/salesnotifier webhook <url>` - Set Discord webhook URL
- `/salesnotifier toggle <true/false>` - Enable/disable all notifications
- `/salesnotifier blackmarket <true/false>` - Toggle Black Market notifications
- `/salesnotifier merchant <true/false>` - Toggle Merchant notifications
- `/salesnotifier status` - Show current configuration
- `/salesnotifier` - Show help

### Configuration File

Settings are saved to `.minecraft/config/salesnotifier.json`

## Notification Types

### Black Market
When the Black Market restocks, you'll receive:
```
🛒 Black Market Restocked!
The Black Market is now available for shopping!
```

### Merchant
When NPCs are back in stock:
```
🛍️ Merchant Restocked!
Merchant stocks have been replenished!
+ [Mythic] Avatar x2
etc.
```

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.4+
- Fabric API 0.140.2+

## Building from Source

```bash
./gradlew build
```

The built mod will be in `build/libs/`

## License

This project is licensed under CC0-1.0.

