package utils

import java.math.BigInteger
import java.security.MessageDigest

class MD5Builder(private val key: String) {
    fun build(): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(key.toByteArray(charset("UTF-8")))
        val result = md.digest()
        return BigInteger(1, result).toString(16)
    }
}
