package base

import env.ConfigLoader
import service.BlockedUser
import kotlin.properties.Delegates

open class BaseMessageHandler : BaseHandler() {

    class UserInBlockListException : Throwable()

    protected var userId: Long by Delegates.notNull()
    override suspend fun run() {
        runCatching {
            onBlock()
            handle()
        }.onFailure { e ->
            if (e is UserInBlockListException) return
            errorProcess(e)
        }
    }

    open suspend fun onBlock() {
        if (BlockedUser.checkBlockStatus(userId)) throw UserInBlockListException()
    }

    open suspend fun isAdmin(userId: Long): Boolean {
        return ConfigLoader.config!!.admin == userId
    }
}
