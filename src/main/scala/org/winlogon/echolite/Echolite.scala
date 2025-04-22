// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.{JDABuilder, JDA}

import org.bukkit.event.{Listener, EventHandler}
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, ChatColor}

import java.util.concurrent.{TimeUnit, Executors}
import java.util.concurrent.{ScheduledExecutorService}
import java.util.logging.Logger

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

case class ServerState(
    logger: Logger,
    config: Configuration,
    pluginManager: PluginManager,
    plugin: Echolite,
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
            getConfig.getStringList("discord.status-list").asScala.toList,
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

        var state = ServerState(logger, config, Bukkit.getPluginManager(), this)
        discordBotManager = DiscordBotManager(state)(ec)
        discordBotManager.startBot()

        getServer.getPluginManager.registerEvents(new MinecraftChatBridge(config, discordBotManager), this)

    }

    override def onDisable(): Unit = {
        discordBotManager.shutdownBot()
    }
}

class DiscordBotManager(state: ServerState)(implicit ec: ExecutionContext) {
    private var jda: Option[JDA] = None
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    def startBot(): Unit = {
        Future {
            state.logger.info("Starting Discord bot (awaiting ready)...")
            val bot = JDABuilder
                .createDefault(state.config.token)
                .addEventListeners(new DiscordChatBridge(state.plugin, state.config))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build()
                .awaitReady()

            state.logger.info("Discord bot ready, registering commands & status cycling")

            jda = Some(bot)

            val commands = bot.updateCommands()
            commands.addCommands(
                Commands.slash("list", "Show the list of online players"),
                Commands.slash("msg", "Send a one-way message to a Minecraft player")
                    .addOption(OptionType.STRING, "player", "The name of the player to message", true)
                    .addOption(OptionType.STRING, "message", "The message to send", true)
            )
            commands.queue()

            if (state.config.sendStatusMessages) {
                sendMessageToDiscord("**Server Status** The server is online.")
            }
            startStatusCycling()

            
        }.recover {
            case e: Exception =>
                state.logger.severe(s"Failed to initialize Discord bot: ${e.getMessage}")
                state.pluginManager.disablePlugin(state.plugin)
        }
    }

    def shutdownBot(): Unit = {
        if (state.config.sendStatusMessages) {
            sendMessageToDiscord("**Server Status** The server is shutting down.")
        }
        jda.foreach(_.shutdown())
        scheduler.shutdown()
        jda = None
    }

    def sendMessageToDiscord(message: String): Unit = {
        jda.foreach { bot =>
            val channel = bot.getTextChannelById(state.config.channelId)

            if (channel != null) {
                Future {
                    channel.sendMessage(message).queue()
                }.onComplete {
                    case Failure(exception) =>
                        state.logger.severe(
                            s"Failed to send message to Discord: ${exception.getMessage}"
                        )
                    case Success(_) => // Message sent successfully
                }
            } else {
                state.logger.severe(
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
                        val newStatus = state.config.statusList(Random.nextInt(state.config.statusList.size))
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
