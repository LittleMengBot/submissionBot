package base

import db.RedisService
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import env.ConfigLoader
import env.LocaleData
import mu.KotlinLogging
import utils.ErrorHandler
import utils.MD5Builder

open class ReplyCommandHandler(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>
) : BaseCommandHandler() {

    private val logger = KotlinLogging.logger {}

    init {
        chatId = update.chat.id.chatId
    }

    open suspend fun getReplyToId(): Long? {
        if (update.replyTo == null) {
            bot.reply(update, LocaleData.getI18nString("command.ok.reply")); return null
        }
        return try {
            RedisService.hashGet(
                "submission:group:message:" +
                    MD5Builder("submission:group:message:${ConfigLoader.config!!.group}").build(),
                update.replyTo!!.messageId.toString()
            ).split(":").first().toLong()
        } catch (_: RedisService.RedisKeyException) {
            bot.reply(update, LocaleData.getI18nString("command.ok.reply"))
            null
        }
    }

    override suspend fun errorProcess(e: Throwable) {
        if (e is RedisService.RedisKeyException) return
        ErrorHandler.sendErrorLog(bot, e.message.toString())
        logger.error(ErrorHandler.parseStackTrace(e))
        return
    }
}
