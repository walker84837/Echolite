// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent.QuitReason
import org.bukkit.event.player.{PlayerJoinEvent, PlayerQuitEvent}
import org.bukkit.event.{EventHandler, Listener, EventPriority}

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import io.papermc.paper.event.player.AsyncChatEvent

class MinecraftChatBridge(config: Configuration, discordBotManager: DiscordBotManager) extends Listener {
    extension (string: String) {
        def getDiscordCompatible(): String = {
            val result = if (string.isEmpty || !string.contains('_')) {
                string
            } else {
                "(?<!_)_(?!_)".r replaceAllIn(string, "\\\\_")
            }
            result
        }
    }
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
            discordBotManager.sendMessageToDiscord(s"**${player.getName.getDiscordCompatible()}** has joined the server!")
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
        val deathReason = discordMessage.replaceFirst(s"$playerName ", "")

        discordBotManager.sendMessageToDiscord(s"**${playerName.getDiscordCompatible()}** $deathReason")
    }

    @EventHandler
    def onPlayerQuit(event: PlayerQuitEvent): Unit = {
        if (!config.sendPlayerJoinMessages) {
            return
        }

        val quitReason = event.getReason()
        val displayName = event.getPlayer().getName
        val formattedPlayerName = s"**${displayName.getDiscordCompatible()}**"
        val message = quitReason match {
            case QuitReason.DISCONNECTED => "has left the server!"
            case QuitReason.TIMED_OUT => "has been kicked due to an unexpected error."
            case QuitReason.ERRONEOUS_STATE => "has been timed out."
            case QuitReason.KICKED => "has been kicked."
            case null => "has been kicked for an unknown reason."
        }
        discordBotManager.sendMessageToDiscord(s"$formattedPlayerName $message")
    }
}
