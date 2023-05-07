package command

import base.BaseCommandHandler
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.RiskFeature
import env.ConfigLoader
import env.LocaleData
import env.LocaleData.getI18nString
import mu.KotlinLogging
import utils.ErrorHandler

@OptIn(RiskFeature::class)
class SetLang(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>,
    private val args: Array<String>
) : BaseCommandHandler() {

    private val logger = KotlinLogging.logger {}

    init {
        userId = update.from?.id?.chatId!!
        chatId = update.chat.id.chatId
    }

    override suspend fun handle() {
        if (!super.isAdmin(userId)) {
            bot.reply(update, getI18nString("permission_denied"))
            return
        }
        if (args.isEmpty()) {
            bot.reply(update, getI18nString("command.setlang.hint").format(LocaleData.activeLangList.toString()))
            return
        }

        if (args[0] !in LocaleData.activeLangList!!) {
            bot.reply(update, getI18nString("command.setlang.error").format(LocaleData.activeLangList.toString()))
            return
        }

        ConfigLoader.set("lang", args[0])
        bot.reply(update, getI18nString("command.setlang.success"))
    }

    override suspend fun errorProcess(e: Throwable) {
        logger.error(ErrorHandler.parseStackTrace(e))
    }
}
