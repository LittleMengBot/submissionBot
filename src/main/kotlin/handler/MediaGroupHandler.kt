package handler

import base.BaseMessageHandler
import db.RedisService
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.PrivateContentMessageImpl
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent
import env.LocaleData
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import service.BlockedUser
import utils.*

class MediaGroupHandler(
    private val bot: BehaviourContext,
    private val update: MediaGroupContent<MediaGroupPartContent>
) : BaseMessageHandler() {

    init {
        userId = update.group[0].sourceMessage.chat.id.chatId
    }

    private val logger = KotlinLogging.logger {}

    override suspend fun handle() {
        if (update.group[0].sourceMessage !is PrivateContentMessageImpl) return
        val button = KeyBoardBuilder("mediagroup", update.group[0].sourceMessage).buildPrivate(true)
        val key = MD5Builder(
            "submission:mediagroup:" +
                "${update.group[0].sourceMessage.chat.id.chatId}:" +
                "${update.group[0].sourceMessage.messageId}"
        ).build()
        val value = MessageSerializer.json.encodeToString(update.group.map { it.content })
        RedisService.setValue(
            "submission:mediagroup:$key",
            GzipUtils.compress(value),
            null,
            2592000u
        )
        bot.reply(
            update.group[0].sourceMessage,
            LocaleData.getI18nString("submission.hint"),
            replyMarkup = button
        )
    }

    override suspend fun errorProcess(e: Throwable) {
        ErrorHandler.sendErrorLog(
            bot,
            e.message.toString() +
                "\nchat: ${update.group[0].sourceMessage.chat.id.chatId}"
        )
        logger.error(ErrorHandler.parseStackTrace(e))
        return
    }

    override suspend fun onBlock() {
        if (BlockedUser.checkBlockStatus(userId) && !super.isAdmin(userId)) {
            bot.reply(update.group[0].sourceMessage, text = LocaleData.getI18nString("blacklist.warn"))
            throw UserInBlockListException()
        }
    }
}
