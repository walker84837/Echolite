# MineCord

MineCord is a Minecraft plugin that integrates Discord and Minecraft chat, allowing messages to be sent between the two platforms.

## Features

- Sends Minecraft chat messages to a specified Discord channel.
- Broadcasts Discord messages to all players in the Minecraft server.
- Configurable through a simple configuration file.
- Supports both standard and Folia (PaperMC) server implementations.

## Requirements

- Minecraft server running a Paper server, or any fork, such as Folia.
- JDA library (included in the project).

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

- `message.discord`: The format for messages sent to Discord. You can use placeholders:
  - `$display_name`: The display name of the Discord user.
  - `$handle`: The username of the Discord user.
  - `$message`: The content of the Discord message.
- `message.minecraft`: The format for messages sent to Minecraft. You can use placeholders:
  - `$user_name`: The name of the Minecraft player.
  - `$message`: The content of the Minecraft message.

## Troubleshooting

- Ensure that your Discord bot has permission to send messages in the specified channel.
- Check the server logs for any error messages related to the plugin.
- Make sure the bot token and channel ID are correctly set in the configuration file.

## License

This project is licensed under the Apache-2.0 License. See the [LICENSE](LICENSE) file for more details.

## Contributing

Contributions are welcome! If you have suggestions for improvements or new features, feel free to open an issue or submit a pull request.

### Roadmap

- [ ] Add support for two-way private messages between Discord and Minecraft.

## Acknowledgments

- [JDA](https://github.com/discord-jda/JDA) for the Discord API integration.

## Contact

For any questions or support, please contact me or open an issue on this GitHub repository.
