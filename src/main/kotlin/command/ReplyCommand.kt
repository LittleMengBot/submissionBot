package command

import base.BaseCommandHandler
import db.RedisService
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.formatting.bold
import dev.inmo.tgbotapi.extensions.utils.ifGroupChat
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.extensions.escapeMarkdownV2Common
import env.ConfigLoader
import env.LocaleData
import mu.KotlinLogging
import utils.ErrorHandler
import utils.MD5Builder

open class ReplyCommand(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>,
    private val args: Array<String>
) : BaseCommandHandler() {

    private val logger = KotlinLogging.logger {}

    init {
        chatId = update.chat.id.chatId
    }

    override suspend fun handle() {
        update.chat.ifGroupChat {
            if (!super.isInConfigGroup()) return
            val dbUserId = getReplyToId() ?: return
            if (args.isEmpty()) {
                bot.reply(update, LocaleData.getI18nString("command.ok.hint")); return
            }
            bot.sendMessage(
                ChatId(dbUserId),
                LocaleData.getI18nString("submission.group.comment").bold(MarkdownV2) +
                    args.joinToString(" ").escapeMarkdownV2Common(),
                MarkdownV2
            )
            bot.reply(update, LocaleData.getI18nString("command.reply.success"))
        }
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
