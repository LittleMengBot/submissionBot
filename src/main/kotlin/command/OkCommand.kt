package command

import base.ReplyCommandHandler
import db.RedisService
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.media_group_id
import dev.inmo.tgbotapi.extensions.utils.ifGroupChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.RiskFeature
import env.LocaleData
import mu.KotlinLogging
import utils.ErrorHandler
import utils.SubmissionFactory

class OkCommand(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>,
    private val args: Array<String>
) : ReplyCommandHandler(bot, update) {

    private val logger = KotlinLogging.logger {}

    @OptIn(RiskFeature::class)
    override suspend fun handle() {
        update.chat.ifGroupChat {
            if (!super.isInConfigGroup()) return
            if (args.isEmpty()) {
                bot.reply(update, LocaleData.getI18nString("command.ok.hint")); return
            }
            val userId = getReplyToId() ?: return
            val type = if (update.replyTo!!.media_group_id != null) "mediagroup" else "single"
            val data = "group:ok:$userId:${update.replyTo!!.messageId}:$type:true"
            SubmissionFactory(
                bot,
                groupChatId = update.chat.id.chatId,
                groupMessageId = update.replyTo!!.messageId,
                reader = update.from!!,
                type = "command",
                comment = LocaleData.getI18nString("submission.group.comment") + args.joinToString(" ")
            ).groupCallback(data.split(":"))
            bot.reply(update, LocaleData.getI18nString("command.reply.success"))
        }
    }

    override suspend fun errorProcess(e: Throwable) {
        if (e is RedisService.RedisKeyException) {
            bot.reply(update, LocaleData.getI18nString("command.ok.error")); return
        }
        ErrorHandler.sendErrorLog(bot, e.message.toString())
        logger.error(ErrorHandler.parseStackTrace(e))
        return
    }
}
