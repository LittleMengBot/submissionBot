package command

import base.BaseCommandHandler
import db.RedisService
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.formatting.bold
import dev.inmo.tgbotapi.extensions.utils.formatting.code
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import env.ConfigLoader
import mu.KotlinLogging
import utils.ErrorHandler
import java.lang.management.ManagementFactory
import java.math.RoundingMode

class StatusCommand(
    private val bot: BehaviourContext,
    private val update: CommonMessage<TextContent>
) : BaseCommandHandler() {

    private val logger = KotlinLogging.logger {}

    override suspend fun handle() {
        if (update.chat.id.chatId == ConfigLoader.config!!.group) {
            val dbInfo = RedisService.getInfo()
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val osBean = ManagementFactory.getOperatingSystemMXBean()
            val cpuUsage = if (osBean is com.sun.management.OperatingSystemMXBean) {
                "${osBean.processCpuLoad.toBigDecimal().setScale(8, RoundingMode.HALF_UP)} %"
            } else {
                "Unknown"
            }
            bot.reply(
                update,
                "--------------------".bold(MarkdownV2) +
                    "\nSystem Status:".bold(MarkdownV2) +
                    "\nDatabase Used: ${convertToHumanMemory(dbInfo as Long)}".code(MarkdownV2) +
                    "\nMemory Used: ${convertToHumanMemory(usedMemory)}".code(MarkdownV2) +
                    "\nCPU Used: $cpuUsage".code(MarkdownV2),
                MarkdownV2
            )
        }
    }

    override suspend fun errorProcess(e: Throwable) {
        ErrorHandler.sendErrorLog(bot, e.message.toString())
        logger.error(ErrorHandler.parseStackTrace(e))
    }

    private fun convertToHumanMemory(bytesCount: Long): String {
        return when (bytesCount) {
            in 0..1023 -> {
                "$bytesCount B"
            }

            in 1024..(1024 * 1024) -> {
                "${
                bytesCount.toBigDecimal()
                    .divide((1024).toBigDecimal())
                    .setScale(3, RoundingMode.HALF_UP)
                } KB"
            }

            else -> {
                "${
                bytesCount.toBigDecimal()
                    .divide((1024 * 1024).toBigDecimal())
                    .setScale(3, RoundingMode.HALF_UP)
                } MB"
            }
        }
    }
}
