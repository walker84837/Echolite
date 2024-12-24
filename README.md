# MineCord

MineCord is a Minecraft plugin that integrates Discord and Minecraft chat, allowing messages to be sent between the two platforms. This plugin uses the JDA (Java Discord API) library to connect to Discord and listen for messages.

## Features

- Sends Minecraft chat messages to a specified Discord channel.
- Broadcasts Discord messages to all players in the Minecraft server.
- Configurable through a simple configuration file.
- Supports both standard and Folia (PaperMC) server implementations.

## Requirements

- Minecraft server running Paper, Spigot, or Folia.
- JDA library (included in the project).

## Installation

1. Download the latest version of the MineCord plugin JAR file.
2. Place the JAR file into the `plugins` directory of your Minecraft server.
3. Start the server to generate the default configuration file.
4. Edit the `config.yml` file located in the `plugins/MineCord` directory to include your Discord bot token, channel ID, and message formats.

## Configuration

The configuration file (`config.yml`) should look like this:

```yaml
discord:
  token: "YOUR_BOT_TOKEN"
  channel-id: "YOUR_CHANNEL_ID"
message:
  discord: "$display_name ($handle): $message"
  minecraft: "$user_name: $message"
```

### Configuration Parameters

- `discord.token`: The token for your Discord bot.
- `discord.channel-id`: The ID of the Discord channel where Minecraft messages will be sent.
- `message.discord`: The format for messages sent to Discord. You can use placeholders:
  - `$display_name`: The display name of the Discord user.
  - `$handle`: The username of the Discord user.
  - `$message`: The content of the Discord message.
- `message.minecraft`: The format for messages sent to Minecraft. You can use placeholders:
  - `$user_name`: The name of the Minecraft player.
  - `$message`: The content of the Minecraft message.

## Usage

Once the plugin is installed and configured, it will automatically start sending messages between Minecraft and Discord. 

- When a player sends a message in Minecraft, it will be sent to the specified Discord channel.
- When a message is sent in the specified Discord channel, it will be broadcast to all players in the Minecraft server.

## Commands

Currently, MineCord does not provide any commands. All functionality is handled automatically based on chat events.

## Troubleshooting

- Ensure that your Discord bot has permission to send messages in the specified channel.
- Check the server logs for any error messages related to the plugin.
- Make sure the bot token and channel ID are correctly set in the configuration file.

## License

This project is licensed under the Apache-2.0 License. See the [LICENSE](LICENSE) file for more details.

## Contributing

Contributions are welcome! If you have suggestions for improvements or new features, feel free to open an issue or submit a pull request.

## Acknowledgments

- [JDA](https://github.com/DV8FromTheWorld/JDA) for the Discord API integration.
- [PaperMC](https://papermc.io/) for the server implementation.

## Contact

For any questions or support, please contact the project maintainer or open an issue on the GitHub repository.
