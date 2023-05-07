package command

import base.BaseCommandHandler
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.ifGroupChat
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import env.LocaleData
import mu.KotlinLogging
import service.BlockedUser
import utils.ErrorHandler
import java.lang.StringBuilder

class BlackList(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>
) : BaseCommandHandler() {

    private val logger = KotlinLogging.logger {}

    init {
        chatId = update.chat.id.chatId
    }
    override suspend fun handle() {
        update.chat.ifGroupChat {
            if (!super.isInConfigGroup()) return
            val blackList = BlockedUser.getBlackList()
            if (blackList.isEmpty()) {
                bot.reply(update, LocaleData.getI18nString("blacklist.empty")); return
            }
            val sb = StringBuilder()
            for (i in blackList.indices step 2) {
                val userId = blackList[i]
                val userInfo = blackList[i + 1]
                sb.append("```$userId```: ```$userInfo```\n")
            }
            bot.reply(update, sb.toString(), MarkdownV2)
        }
    }

    override suspend fun errorProcess(e: Throwable) {
        ErrorHandler.sendErrorLog(bot, e.message.toString())
        logger.error(ErrorHandler.parseStackTrace(e))
        return
    }
}
