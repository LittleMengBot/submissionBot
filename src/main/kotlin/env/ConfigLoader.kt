package env

import env.LocaleData.getI18nString
import mu.KotlinLogging
import type.ConfigData
import utils.ErrorHandler
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*

object ConfigLoader {

    private val logger = KotlinLogging.logger {}

    class ConfigLoadException(override val message: String?) : Throwable()

    var config: ConfigData? = null

    fun init() {
        LocaleData.refreshResourceBundle(Locale.forLanguageTag("zh-CN"))
        try {
            val properties = Properties()
            properties.load(FileReader("config.properties"))
            val token = properties
                .getProperty("token")
                ?: throw ConfigLoadException(getI18nString("config.error.token_error"))
            val group = properties
                .getProperty("group")
                ?: throw ConfigLoadException(getI18nString("config.error.group_error"))
            var channel: Any = properties
                .getProperty("channel")
                ?: throw ConfigLoadException(getI18nString("config.error.channel_error"))
            try {
                channel = (channel as String).toLong()
            } catch (_: NumberFormatException) {
            }
            val admin = properties
                .getProperty("admin")
                ?: throw ConfigLoadException(getI18nString("config.error.admin_error"))
            val lang = properties
                .getProperty("lang")
                ?: throw ConfigLoadException(getI18nString("config.error.lang_error"))
            val redisHost = properties
                .getProperty("redisHost")
                ?: throw ConfigLoadException(getI18nString("config.error.redis_info_error"))
            LocaleData.refreshResourceBundle(Locale.forLanguageTag(lang))
            config = ConfigData(
                token,
                group.toLong(),
                channel,
                admin.toLong(),
                lang,
                redisHost
            )
        } catch (e: IOException) {
            logger.error(ErrorHandler.parseStackTrace(e))
        } catch (e: ConfigLoadException) {
            logger.error(ErrorHandler.parseStackTrace(e))
        } catch (e: NumberFormatException) {
            logger.error(ErrorHandler.parseStackTrace(e))
        } catch (e: IllegalArgumentException) {
            logger.error(ErrorHandler.parseStackTrace(e))
        }
    }

    fun set(key: String, value: String) {
        try {
            val properties = Properties()
            properties.load(FileReader("config.properties"))
            properties.setProperty(key, value)
            val fileWriter = FileWriter("config.properties")
            properties.store(fileWriter, null)
            fileWriter.close()
            init()
        } catch (e: IllegalArgumentException) {
            logger.error(ErrorHandler.parseStackTrace(e))
        } catch (e: IOException) {
            logger.error(ErrorHandler.parseStackTrace(e))
        }
    }
}
