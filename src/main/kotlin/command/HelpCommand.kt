package command

import base.BaseCommandHandler
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.extensions.escapeMarkdownV2Common
import env.ConfigLoader
import env.LocaleData
import mu.KotlinLogging
import utils.ErrorHandler

class HelpCommand(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>
) : BaseCommandHandler() {

    private val logger = KotlinLogging.logger {}

    override suspend fun handle() {
        if (update.chat.id.chatId == ConfigLoader.config!!.group) {
            bot.reply(
                update,
                LocaleData.getI18nString("submission.group.help_text").escapeMarkdownV2Common(),
                MarkdownV2
            )
        }
    }

    override suspend fun errorProcess(e: Throwable) {
        ErrorHandler.sendErrorLog(bot, e.message.toString())
        logger.error(ErrorHandler.parseStackTrace(e))
        return
    }
}
