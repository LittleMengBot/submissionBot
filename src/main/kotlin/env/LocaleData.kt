package env

import mu.KotlinLogging
import utils.ErrorHandler
import java.io.File
import java.util.*
import java.util.jar.JarFile

object LocaleData {

    private val logger = KotlinLogging.logger {}

    private var rb: ResourceBundle? = null
    var activeLangList: MutableList<String>? = null

    fun refreshResourceBundle(locale: Locale) {
        try {
            setPropertiesFileNames()
            rb = ResourceBundle.getBundle("i18n.string", locale)
        } catch (e: MissingResourceException) {
            logger.error(ErrorHandler.parseStackTrace(e))
        }
    }

    fun getI18nString(key: String): String {
        return try {
            rb!!.getString(key)
        } catch (e: MissingResourceException) {
            logger.error(ErrorHandler.parseStackTrace(e))
            ""
        }
    }

    private fun setPropertiesFileNames(): MutableList<String>? {
        val classLoader = Thread.currentThread().contextClassLoader
        val resources = classLoader.getResources("i18n")

        activeLangList = mutableListOf()
        while (resources.hasMoreElements()) {
            val resource = resources.nextElement()
            if (resource.protocol == "file") {
                val folder = File(resource.path)
                folder.listFiles()?.forEach {
                    if (it.isFile && it.name.endsWith(".properties")) {
                        activeLangList?.add(parseLangName(it.name))
                    }
                }
            } else if (resource.protocol == "jar") {
                val jarFile = resource.toURI().toString().split("!").first().replace("jar:file:", "")
                JarFile(jarFile).use { jar ->
                    val entries = jar.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name.startsWith("i18n/") && entry.name.endsWith(".properties")) {
                            activeLangList?.add(parseLangName(entry.name.replace("i18n/", "")))
                        }
                    }
                }
            }
        }

        return activeLangList
    }

    private fun parseLangName(name: String): String {
        if (name == "string.properties") return "en-US"
        return name
            .replace(".properties", "")
            .replace("string_", "")
            .replace("_", "-")
    }
}
