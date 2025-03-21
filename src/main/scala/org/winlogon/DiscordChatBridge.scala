package org.winlogon

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.ChatColor
import org.bukkit.event.Listener
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext

class DiscordChatBridge(plugin: JavaPlugin, config: Configuration) extends ListenerAdapter  {
  private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
  private val minecraftSerializer = MinecraftSerializer.INSTANCE

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    if (!event.getChannel.getId.equals(config.channelId) || event.getAuthor.isBot)
      return

    val roles = event.getMember.getRoles
    val userRole = if (!roles.isEmpty) {
      Some(roles.get(0).getName())
    } else {
      None
    }

    val msg = config.discordMessage
      .replace("$display_name", event.getAuthor.getEffectiveName)
      .replace("$handle", event.getAuthor.getName)
      .replace("$role", userRole.getOrElse(config.defaultRole))
      .replace("$message", event.getMessage.getContentDisplay)

    val discordMessage = minecraftSerializer.serialize(msg)
    val message: String = legacySerializer.serialize(discordMessage)

    if (!isFolia(plugin)) {
      new BukkitRunnable {
        override def run(): Unit = Bukkit.broadcastMessage(message)
      }.runTask(plugin)
    } else {
      Bukkit.getGlobalRegionScheduler.execute(
        plugin,
        new Runnable {
          override def run(): Unit = Bukkit.broadcastMessage(message)
        }
      )
    }
  }

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    val players = Bukkit.getOnlinePlayers.asScala
    event.getName match {
      case "list" =>
        val playerNames = if (players.isEmpty) {
          "No players are currently online."
        } else {
          players.map(_.getName).mkString(", ")
        }
        event
          .reply(s"Online Players: $playerNames")
          .setEphemeral(true)
          .queue()
      case "msg" =>
        val playerName = event.getOption("player").getAsString
        val message = event.getOption("message").getAsString
        val player = Bukkit.getPlayer(playerName)

        messageCommand(message, player, event)
      case _ => // Ignore unknown commands
    }
  }

  private def messageCommand(message: String, player: Player, event: SlashCommandInteractionEvent): Unit = {
    val playerName = player.getName

    if (player == null || !player.isOnline) {
      event
        .reply(s"Player '$playerName' is not online or does not exist.")
        .setEphemeral(true)
        .queue()
        return
    }

    val formattedMessage = ChatColor.translateAlternateColorCodes(
      '&', s"&8(&3${event.getUser.getName}&7 -> &2you&8)&7 $message"
    )

    if (!isFolia(plugin)) {
      new BukkitRunnable {
        override def run(): Unit = player.sendMessage(formattedMessage)
      }.runTask(plugin)
    } else {
      Bukkit.getGlobalRegionScheduler.execute(plugin, new Runnable {
        override def run(): Unit = player.sendMessage(formattedMessage)
      })
    }

    event
      .reply(s"Message sent to $playerName!")
      .setEphemeral(true)
      .queue()
  }

  private def isFolia(plugin: JavaPlugin): Boolean = {
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
      true
    } catch {
      case _: ClassNotFoundException => false
    }
  }
}

