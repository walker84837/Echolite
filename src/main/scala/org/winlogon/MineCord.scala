package org.winlogon

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.util.matching.Regex
import net.dv8tion.jda.api.{JDABuilder, JDA}
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.bukkit.Bukkit
import org.bukkit.event.{Listener, EventHandler}
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

case class Configuration(
  token: String,
  channelId: String,
  discordMessage: String,
  minecraftMessage: String,
)

class MineCord extends JavaPlugin with Listener {
  private var jda: Option[JDA] = None
  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private var config: Configuration = _

  private def isFolia: Boolean = {
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
      true
    } catch {
      case _: ClassNotFoundException => false
    }
  }

  private def loadConfig(): Configuration = {
    Configuration(
      getConfig.getString("discord.token"),
      getConfig.getString("discord.channel-id"),
      getConfig.getString("message.discord"),
      getConfig.getString("message.minecraft"),
    )
  }

  private def validateConfig(config: Configuration): Boolean = {
    if (config.token.isEmpty || config.channelId.isEmpty || 
        config.channelId == "CHANNEL_ID" || config.token == "BOT_TOKEN") {
      getLogger.severe("The Discord bot isn't configured for use in this server. Check the config file.")
      false
    } else {
      true
    }
  }

  override def onEnable(): Unit = {
    saveDefaultConfig()
    config = loadConfig()

    if (!validateConfig(config)) {
      getServer.getPluginManager.disablePlugin(this)
      return
    }

    try {
      getLogger.info("Starting Discord bot")
      jda = Some(JDABuilder.createDefault(config.token)
        .addEventListeners(new DiscordListener(config.channelId))
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .build()
      )
    } catch {
      case e: Exception =>
        getLogger.severe(s"Failed to initialize Discord bot: ${e.getMessage}")
        getServer.getPluginManager.disablePlugin(this)
    }

    getServer.getPluginManager.registerEvents(this, this)
  }

  override def onDisable(): Unit = {
    jda.foreach(_.shutdown())
    jda = None
  }

  @EventHandler
  def onPlayerChat(event: AsyncPlayerChatEvent): Unit = {
    val playerMessage = "&[a-zA-Z0-9]".r replaceAllIn(event.getMessage, "")
    println(playerMessage)
    val message = config.minecraftMessage
      .replace("$user_name", event.getPlayer.getName)
      .replace("$message", playerMessage.trim)
    sendMessageToDiscord(message)
  }

  private def sendMessageToDiscord(message: String): Unit = {
    jda.foreach { bot =>
      val channel = bot.getTextChannelById(config.channelId)

      if (channel != null) {
        Future {
          channel.sendMessage(message).queue()
        }.onComplete {
          case Failure(exception) =>
            getLogger.severe(s"Failed to send message to Discord: ${exception.getMessage}")
          case Success(_) => // Message sent successfully
        }
      } else {
        getLogger.severe("Discord channel not found. Please check the channel ID in the config.")
      }
    }
  }

  private class DiscordListener(channelId: String) extends ListenerAdapter {
    override def onMessageReceived(event: MessageReceivedEvent): Unit = {
      if (!event.getChannel.getId.equals(channelId) || event.getAuthor.isBot) return

      val message = config.discordMessage
        .replace("$display_name", event.getAuthor.getEffectiveName)
        .replace("$handle", event.getAuthor.getName)
        .replace("$message", event.getMessage.getContentDisplay)

      if (!isFolia) {
        new BukkitRunnable {
          override def run(): Unit = Bukkit.broadcastMessage(message)
        }.runTask(MineCord.this)
      } else {
        Bukkit.getGlobalRegionScheduler.execute(MineCord.this, new Runnable {
          override def run(): Unit = Bukkit.broadcastMessage(message)
        })
      }
    }
  }
}
