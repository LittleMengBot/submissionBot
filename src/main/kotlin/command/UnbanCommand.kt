package command

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

class UnbanCommand(
    bot: BehaviourContext,
    update: CommonMessage<TextContent>,
    args: Array<String>
) : BanCommand(bot, update, args) {
    override suspend fun handle() {
        super.blockPart(false)
    }
}
