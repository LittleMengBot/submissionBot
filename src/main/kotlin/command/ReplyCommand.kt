package command

import base.ReplyCommandHandler
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
import env.LocaleData

open class ReplyCommand(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>,
    private val args: Array<String>
) : ReplyCommandHandler(bot, update) {

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
}
