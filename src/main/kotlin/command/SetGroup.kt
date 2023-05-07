package command

import base.BaseCommandHandler
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.ifGroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.RiskFeature
import env.ConfigLoader
import env.LocaleData.getI18nString
import handler.MainHandler
import mu.KotlinLogging
import utils.ErrorHandler

@OptIn(RiskFeature::class)
class SetGroup(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>
) : BaseCommandHandler() {

    private val logger = KotlinLogging.logger {}

    init {
        userId = update.from?.id?.chatId!!
    }

    override suspend fun handle() {
        update.chat.ifGroupChat {
            if (!isAdmin(userId) && userId != ConfigLoader.config!!.group) {
                bot.reply(update, getI18nString("permission_denied"))
                return
            }
            if (update.chat !is SupergroupChat) {
                bot.reply(update, getI18nString("command.setgroup.type.error"))
                return
            }
            ConfigLoader.set("group", update.chat.id.chatId.toString())
            bot.reply(update, getI18nString("command.setgroup.success"))
            MainHandler.resetHandler(bot)
        }
    }

    override suspend fun errorProcess(e: Throwable) {
        logger.error(ErrorHandler.parseStackTrace(e))
    }
}
