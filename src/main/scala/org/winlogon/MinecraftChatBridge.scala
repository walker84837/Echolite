package org.winlogon

import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.event.player.AsyncPlayerChatEvent

class MinecraftChatBridge(config: Configuration, discordBotManager: DiscordBotManager) extends Listener {

  @EventHandler
  def onPlayerChat(event: AsyncPlayerChatEvent): Unit = {
    val playerMessage = "&[a-zA-Z0-9]".r replaceAllIn (event.getMessage, "")
    val message = config.minecraftMessage
      .replace("$user_name", event.getPlayer.getName)
      .replace("$message", playerMessage.trim)
    discordBotManager.sendMessageToDiscord(message)
  }
}

