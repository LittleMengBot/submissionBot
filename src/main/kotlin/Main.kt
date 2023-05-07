import dev.inmo.tgbotapi.extensions.api.telegramBot
import env.ConfigLoader
import env.Environment
import env.LocaleData.getI18nString
import handler.MainHandler
import mu.KotlinLogging
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

@Suppress("WildcardImport")
suspend fun main() {
    if (!Environment().init()) exitProcess(-1)
    MainHandler.bot = telegramBot(ConfigLoader.config!!.token)
    if (!Environment().checkChannelPermission(MainHandler.bot)) {
        logger.error(getI18nString("config.error.channel_permission"))
        exitProcess(-1)
    }
    MainHandler.run()
}
