package command

import db.RedisService
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.ifGroupChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import env.ConfigLoader
import env.LocaleData
import mu.KotlinLogging
import service.BlockedUser
import utils.ErrorHandler

open class BanCommand(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>,
    private val args: Array<String>
) : ReplyCommand(bot, update, args) {

    private val logger = KotlinLogging.logger {}

    init {
        chatId = update.chat.id.chatId
    }
    override suspend fun handle() {
        blockPart(true)
    }

    suspend fun blockPart(isBan: Boolean) {
        update.chat.ifGroupChat {
            if (!super.isInConfigGroup()) return
            val dbUserId = super.getReplyToId() ?: return
            when (isBan) {
                true -> {
                    if (BlockedUser.checkBlockStatus(dbUserId)) {
                        bot.reply(update, LocaleData.getI18nString("blacklist.already_in")); return
                    }
                    if (dbUserId == ConfigLoader.config!!.admin) {
                        bot.reply(update, LocaleData.getI18nString("permission_denied")); return
                    }
                    if (args.isEmpty()) {
                        BlockedUser.blockUser(dbUserId, "")
                    } else {
                        BlockedUser.blockUser(dbUserId, args.joinToString(" "))
                    }
                    bot.reply(update, LocaleData.getI18nString("blacklist.ban"))
                }
                false -> {
                    BlockedUser.unBlockUser(dbUserId)
                    bot.reply(update, LocaleData.getI18nString("blacklist.unban"))
                }
            }
        }
    }

    override suspend fun errorProcess(e: Throwable) {
        if (e is RedisService.RedisKeyException) {
            bot.reply(update, LocaleData.getI18nString("command.ok.reply")); return
        }
        ErrorHandler.sendErrorLog(bot, e.message.toString())
        logger.error(ErrorHandler.parseStackTrace(e))
        return
    }
}
