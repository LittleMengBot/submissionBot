package base

import env.ConfigLoader
import kotlin.properties.Delegates

open class BaseCommandHandler : BaseMessageHandler() {

    protected var chatId: Long by Delegates.notNull()
    override suspend fun onBlock() = Unit

    open suspend fun isInConfigGroup(): Boolean {
        return ConfigLoader.config!!.group == chatId
    }
}
