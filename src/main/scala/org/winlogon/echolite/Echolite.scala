// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.{JDABuilder, JDA}

import org.bukkit.event.{Listener, EventHandler}
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, ChatColor}

import java.util.concurrent.{TimeUnit, Executors}
import java.util.concurrent.{ScheduledExecutorService}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Try, Random, Failure, Success}

case class Configuration(
    token: String,
    channelId: String,
    defaultRole: String,
    sendStatusMessages: Boolean,
    sendPlayerJoinMessages: Boolean,
    sendPlayerDeathMessages: Boolean,
    statusList: List[String],
    discordMessage: String,
    minecraftMessage: String,
)

class Echolite extends JavaPlugin with Listener {
    private var jda: Option[JDA] = None
    private implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
    private var config: Configuration = _
    private var discordBotManager: DiscordBotManager = _
    val logger = this.getLogger

    def isFolia: Boolean = {
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
            getConfig.getString("discord.default-role"),
            getConfig.getBoolean("discord.status-messages"),
            getConfig.getBoolean("discord.player-join-messages"),
            getConfig.getBoolean("discord.player-death-messages"),
            getConfig.getList("discord.status-list").asScala.map(_.toString).toList,
            getConfig.getString("message.discord"),
            getConfig.getString("message.minecraft")
        )
    }

    private def validateConfig(config: Configuration): Boolean = {
        logger.info("Validating configuration file...")
        if (
            config.token.isEmpty || config.channelId.isEmpty ||
            config.channelId == "CHANNEL_ID" || config.token == "BOT_TOKEN"
        ) {
            getLogger.severe(
                "The Discord bot isn't configured for use in this server. Check the config file."
            )
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

        logger.info(s"Hello World! I'm running on ${if (isFolia) "Folia" else "Bukkit"}")

        discordBotManager = new DiscordBotManager(this, config)(ec)
        discordBotManager.startBot()

        getServer.getPluginManager.registerEvents(new MinecraftChatBridge(config, discordBotManager), this)

        if (config.sendStatusMessages) {
            // TODO: doesn't send message to the server
            discordBotManager.sendMessageToDiscord("**Server Status** The server is online.")
        }
    }

    override def onDisable(): Unit = {
        if (config.sendStatusMessages) {
            discordBotManager.sendMessageToDiscord("**Server Status** The server is shutting down.")
        }
        discordBotManager.shutdownBot()
    }
}

class DiscordBotManager(plugin: JavaPlugin, config: Configuration)(implicit ec: ExecutionContext) {
    private var jda: Option[JDA] = None
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    def startBot(): Unit = {
        try {
            plugin.getLogger.info("Starting Discord bot")
            jda = Some(
                JDABuilder
                    .createDefault(config.token)
                    .addEventListeners(new DiscordChatBridge(plugin, config))
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .build()
            )
        } catch {
            case e: Exception =>
                plugin.getLogger.severe(s"Failed to initialize Discord bot: ${e.getMessage}")
                plugin.getServer.getPluginManager.disablePlugin(plugin)
        }

        jda.foreach { bot =>
            val commands = bot.updateCommands()
            commands.addCommands(
                Commands.slash("list", "Show the list of online players"),
                Commands.slash("msg", "Send a one-way message to a Minecraft player")
                    .addOption(OptionType.STRING, "player", "The name of the player to message", true)
                    .addOption(OptionType.STRING, "message", "The message to send", true)
            )
            commands.queue()

            startStatusCycling()
        }
    }

    def shutdownBot(): Unit = {
        jda.foreach(_.shutdown())
        scheduler.shutdown()
        jda = None
    }

    def sendMessageToDiscord(message: String): Unit = {
        jda.foreach { bot =>
            val channel = bot.getTextChannelById(config.channelId)

            if (channel != null) {
                Future {
                    channel.sendMessage(message).queue()
                }.onComplete {
                    case Failure(exception) =>
                        plugin.getLogger.severe(
                            s"Failed to send message to Discord: ${exception.getMessage}"
                        )
                    case Success(_) => // Message sent successfully
                }
            } else {
                plugin.getLogger.severe(
                    "Discord channel not found. Please check the channel ID in the config."
                )
            }
        }
    }

    def startStatusCycling(): Unit = {
        def scheduleNextStatusChange(): Unit = {
            // Random delay between 5 and 10 minutes
            val randomDelay = 5 + Random.nextInt(6)
            scheduler.schedule(
                new Runnable {
                    override def run(): Unit = {
                        val newStatus = config.statusList(Random.nextInt(config.statusList.size))
                        plugin.getLogger.info(s"Changing status to '$newStatus'")
                        jda.foreach(_.getPresence.setActivity(Activity.playing(newStatus)))
                        scheduleNextStatusChange()
                    }
                },
                randomDelay,
                TimeUnit.MINUTES
            )
        }

        scheduleNextStatusChange()
    }

    def isFolia: Boolean = {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch {
            case _: ClassNotFoundException => false
        }
    }
}
