package utils

object TextUtils {
    fun isTelegramBlankName(userName: String): Boolean {
        val chars = userName.toByteArray()
        if (chars.isEmpty()) {
            return false
        }
        if (chars.size < 3) return false
        return chars[0] == (-29).toByte() && chars[1] == (-123).toByte() && chars[2] == (-92).toByte()
    }
}
