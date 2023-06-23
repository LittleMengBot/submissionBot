package env

import db.RedisService
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.exceptions.RequestException
import dev.inmo.tgbotapi.bot.exceptions.UnauthorizedException
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.Username
import dev.inmo.tgbotapi.types.chat.ExtendedSupergroupChatImpl
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMemberImpl
import dev.inmo.tgbotapi.utils.RiskFeature
import env.LocaleData.getI18nString
import io.ktor.client.plugins.*
import mu.KotlinLogging
import utils.ErrorHandler
import java.io.PrintStream

class Environment {

    private val logger = KotlinLogging.logger {}

    suspend fun init(): Boolean {
        setError()
        logger.info("Loading Config...")
        ConfigLoader.init()
        if (ConfigLoader.config == null) {
            logger.error("Config file error! Please check the config.properties.")
            return false
        }
        logger.info("Init database...")
        RedisService.init()
        if (RedisService.client == null) {
            logger.error(getI18nString("config.error.redis_info_error"))
            return false
        }
        return true
    }

    private fun setError() {
        System.setErr(object : PrintStream(System.err) {
            override fun println(x: Any?) {
                when {
                    x is UnauthorizedException -> {
                        val logger = KotlinLogging.logger {}
                        logger.error("401 Unauthorized! Check your bot token.")
                    }

                    x is Exception && x !is HttpRequestTimeoutException -> {
                        logger.error(ErrorHandler.parseStackTrace(x))
                    }
                }
            }
        })
    }

    suspend fun checkChannelPermission(bot: TelegramBot): Boolean {
        try {
            val channel = ConfigLoader.config!!.channel
            if (channel is String) {
                if (channel.startsWith("@")) {
                    val channelChat = bot.getChat(Username(ConfigLoader.config!!.channel as String))
                    val permission = bot.getChatMember(
                        Username(ConfigLoader.config!!.channel as String),
                        bot.getMe().id
                    )
                    if (permission is AdministratorChatMemberImpl) {
                        ConfigLoader.set("channel", channelChat.id.chatId.toString())
                        return permission.canPostMessages
                    }
                    return false
                }
            }
            if (channel is Long) {
                val permission = bot.getChatMember(ChatId(channel), bot.getMe())
                if (permission is AdministratorChatMemberImpl) {
                    return permission.canPostMessages
                }
                return false
            }
            return false
        } catch (e: RequestException) {
            logger.error(ErrorHandler.parseStackTrace(e))
            return false
        }
    }

    @OptIn(RiskFeature::class)
    suspend fun checkGroupPermission(bot: TelegramBot): Boolean {
        logger.info("Checking permission...")
        try {
            val permission = bot.getChat(ChatId(ConfigLoader.config!!.group))
            if (permission !is ExtendedSupergroupChatImpl) {
                return false
            }
            return permission.permissions.canSendMessages!!
        } catch (_: RequestException) {
            logger.error("Add the bot to a group and run /setgroup .")
            return false
        }
    }
}
