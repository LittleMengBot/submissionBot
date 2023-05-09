package handler

import base.BaseHandler
import command.*
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.exceptions.CommonBotException
import dev.inmo.tgbotapi.bot.exceptions.MessageIsNotModifiedException
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.PrivateContentMessageImpl
import dev.inmo.tgbotapi.utils.RiskFeature
import env.ConfigLoader
import env.Environment
import env.LocaleData
import io.ktor.client.plugins.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import mu.KotlinLogging
import utils.ErrorHandler
import java.net.SocketException

object MainHandler : BaseHandler() {

    private val logger = KotlinLogging.logger {}

    private lateinit var job: Job
    lateinit var bot: TelegramBot

    private var lastException: Throwable? = null

    override suspend fun handle() {
        resetHandler(bot)
    }

    suspend fun resetHandler(bot: TelegramBot) {
        if (::job.isInitialized) {
            job.cancelAndJoin()
        }
        job = bot.buildBehaviourWithLongPolling(
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        ) {
            handleUpdate(Environment().checkGroupPermission(bot), this)
        }
        job.join()
    }

    override suspend fun errorProcess(e: Throwable) {
        val ignore = listOf(
            CancellationException::class,
            HttpRequestTimeoutException::class,
            SocketException::class,
            MessageIsNotModifiedException::class,
            ClosedReceiveChannelException::class,
            CommonBotException::class
        )
        if (ignore.none { e.instanceOf(it) || e.cause?.instanceOf(it) == true }) {
            if (e == lastException) {
                return
            }
            if ("query is too old" in e.localizedMessage) return
            lastException = e
            if (Environment().checkGroupPermission(bot)) {
                bot.sendMessage(ChatId(ConfigLoader.config!!.group), e.message.toString())
            }
            logger.error(ErrorHandler.parseStackTrace(e))
        }
    }

    @OptIn(RiskFeature::class)
    suspend fun handleUpdate(groupPermission: Boolean, bc: BehaviourContext) {
        if (!groupPermission) {
            logger.warn("Please use /setgroup in a supergroup to set a group.")
            bc.onCommand("setgroup") {
                SetGroup(this, it).run()
            }
            return
        }

        logger.info(LocaleData.getI18nString("config.success"))

        bc.onMediaGroup {
            MediaGroupHandler(this, it).run()
        }

        bc.onDataCallbackQuery {
            CallbackHandler(this, it).run()
        }

        bc.onContentMessage {
            if (it.text != null) {
                if (it.text!!.startsWith("/")) return@onContentMessage
            }
            if (it.mediaGroupId != null) return@onContentMessage
            SingleMessageHandler(this, it).run()
        }

        bc.onCommand("setgroup") {
            SetGroup(this, it).run()
        }

        bc.onCommand("start") {
            if (it !is PrivateContentMessageImpl) return@onCommand
            reply(it, LocaleData.getI18nString("command.start"))
        }

        bc.onCommand("help") {
            HelpCommand(this, it).run()
        }

        bc.onCommand("blacklist") {
            BlackList(this, it).run()
        }

        bc.onCommand("info|status".toRegex()) {
            StatusCommand(this, it).run()
        }

        bc.onCommandWithArgs("setlang") { msg, args ->
            SetLang(this, msg, args).run()
        }

        bc.onCommandWithArgs("ok") { msg, args ->
            OkCommand(this, msg, args).run()
        }

        bc.onCommandWithArgs("(ban|block)".toRegex()) { msg, args ->
            BanCommand(this, msg, args).run()
        }

        bc.onCommandWithArgs("(unban|unblock)".toRegex()) { msg, args ->
            UnbanCommand(this, msg, args).run()
        }

        bc.onCommandWithArgs("(re|reply|echo)".toRegex()) { msg, args ->
            ReplyCommand(this, msg, args).run()
        }
    }
}
