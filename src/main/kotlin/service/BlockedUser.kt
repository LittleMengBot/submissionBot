package service

import db.RedisService
import env.ConfigLoader
import utils.MD5Builder

object BlockedUser {

    private val key = "submission:blacklist:" +
        MD5Builder("submission:blacklist:${ConfigLoader.config!!.group}").build()

    suspend fun blockUser(userId: Long, reason: String) {
        var blockReason = reason
        if (blockReason == "") blockReason = "None"
        RedisService.hashSet(
            key,
            Pair(userId.toString(), blockReason)
        )
    }

    suspend fun unBlockUser(userId: Long) {
        RedisService.hashUnset(
            key,
            userId.toString()
        )
    }

    suspend fun getBlackList(): List<String> {
        return RedisService.hashGetAll(key)
    }

    suspend fun checkBlockStatus(userId: Long): Boolean {
        return try {
            RedisService.hashGet(key, userId.toString()); true
        } catch (_: RedisService.RedisKeyException) {
            false
        }
    }
}
