package handler

import base.BaseMessageHandler
import db.RedisService
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.PrivateContentMessageImpl
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import env.LocaleData.getI18nString
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import service.BlockedUser
import utils.*

class SingleMessageHandler(
    private val bot: BehaviourContext,
    private val update: CommonMessage<MessageContent>
) : BaseMessageHandler() {

    private val logger = KotlinLogging.logger {}

    init {
        userId = update.chat.id.chatId
    }

    override suspend fun handle() {
        if (update !is PrivateContentMessageImpl) return
        val key = MD5Builder(
            "submission:single:" +
                "${update.chat.id.chatId}:" +
                "${update.messageId}"
        ).build()
        try {
            val value = MessageSerializer.json.encodeToString(update.content)
            RedisService.setValue(
                "submission:single:$key",
                GzipUtils.compress(value),
                null,
                2592000u
            )
            val button = KeyBoardBuilder("single", update).buildPrivate(false)
            bot.reply(update, getI18nString("submission.hint"), replyMarkup = button)
        } catch (e: NotImplementedError) {
            this.errorProcess(e)
        }
    }

    override suspend fun errorProcess(e: Throwable) {
        logger.error(ErrorHandler.parseStackTrace(e))
        bot.reply(update, getI18nString("submission.hint.error"))
        ErrorHandler.sendErrorLog(
            bot,
            "${getI18nString("submission.hint.error")}:\n" +
                update.content.javaClass.name.split(".").last() +
                "\nchat: ${update.chat.id.chatId}"
        )
    }

    override suspend fun onBlock() {
        if (BlockedUser.checkBlockStatus(userId) && !super.isAdmin(userId)) {
            bot.reply(update, text = getI18nString("blacklist.warn"))
            throw UserInBlockListException()
        }
    }
}
