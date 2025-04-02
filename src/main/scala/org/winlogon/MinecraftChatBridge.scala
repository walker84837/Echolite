package org.winlogon

import org.bukkit.event.player.PlayerQuitEvent.QuitReason
import org.bukkit.event.player.{PlayerJoinEvent, PlayerQuitEvent}
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.{EventHandler, Listener, EventPriority}

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.Component
import io.papermc.paper.event.player.AsyncChatEvent

class MinecraftChatBridge(config: Configuration, discordBotManager: DiscordBotManager) extends Listener {
  private val plainTextSerializer = PlainTextComponentSerializer.plainText()

  @EventHandler(EventPriority.LOW)
  def onPlayerChat(event: AsyncChatEvent): Unit = {
    if (event.isCancelled) return

    val msg = plainTextSerializer.serialize(event.message())

    val playerMessage = "&[a-zA-Z0-9]".r replaceAllIn (msg, "")
    val miniMessageFormat = """<[^>]*>""".r replaceAllIn (playerMessage, "")
    val message = config.minecraftMessage
      .replace("$user_name", event.getPlayer.getName)
      .replace("$message", miniMessageFormat.trim)
    discordBotManager.sendMessageToDiscord(message)
  }

  @EventHandler
  def onPlayerJoin(event: PlayerJoinEvent): Unit = {
    if (config.sendPlayerJoinMessages) {
        val player = event.getPlayer()
        discordBotManager.sendMessageToDiscord(s"**${player.getName}** has joined the server!")
    }
  }

  @EventHandler(ignoreCancelled = true)
  def onPlayerDeath(event: PlayerDeathEvent): Unit = {
    if (!config.sendPlayerDeathMessages) {
      return
    }

    val deadPlayer = event.getPlayer()
    val playerName = deadPlayer.getName
    val minecraftDeathMessage: Component = Option(event.deathMessage()).getOrElse(Component.text("died."))
    val discordMessage = plainTextSerializer.serialize(minecraftDeathMessage)

    discordBotManager.sendMessageToDiscord(s"**${playerName}** ${discordMessage.replaceFirst(s"$playerName ", "")}")
  }

  @EventHandler
  def onPlayerQuit(event: PlayerQuitEvent): Unit = {
    if (!config.sendPlayerJoinMessages) {
      return
    }

    val quitReason = event.getReason()
    val player = event.getPlayer()
    val initialString = s"**${player.getName}**"
    quitReason match {
      case QuitReason.DISCONNECTED => discordBotManager.sendMessageToDiscord(s"$initialString has left the server!")
      case QuitReason.TIMED_OUT | QuitReason.ERRONEOUS_STATE => {
        discordBotManager.sendMessageToDiscord(
          s"$initialString has been has been disconnected due to a connection timeout or an unexpected error."
        )
      }
      case QuitReason.KICKED => discordBotManager.sendMessageToDiscord(s"$initialString has been kicked.")
      case null => discordBotManager.sendMessageToDiscord(s"$initialString has been kicked for an unknown reason.")
    }
  }
}
