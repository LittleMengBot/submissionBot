package utils

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatId
import env.ConfigLoader

object ErrorHandler {
    fun parseStackTrace(e: Throwable): String {
        val sb = StringBuilder()
        sb.append("$e\n\t")
        e.stackTrace.forEach {
            if (it.toString().startsWith("kotlin") || it.toString().startsWith("java")) return@forEach
            sb.append(it.toString())
            sb.append("\n\t")
        }
        return sb.toString()
    }

    suspend fun sendErrorLog(bot: BehaviourContext, log: String) {
        bot.sendMessage(
            ChatId(ConfigLoader.config!!.group),
            "*Runtime Error:*\n$log",
        )
    }
}
