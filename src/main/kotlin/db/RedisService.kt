package db

import env.ConfigLoader
import io.github.crackthecodeabhi.kreds.args.SetOption
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.KredsConnectionException
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.netty.handler.codec.CodecException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import utils.ErrorHandler

private val logger = KotlinLogging.logger {}

object RedisService {
    var client: KredsClient? = null

    class RedisKeyException : Throwable()

    suspend fun init() {
        client = newClient(Endpoint.from(ConfigLoader.config!!.redisHost))
        try {
            client?.ping()
        } catch (e: KredsConnectionException) {
            client = null
            logger.error(ErrorHandler.parseStackTrace(e))
        } catch (e: CodecException) {
            client = null
            logger.error(ErrorHandler.parseStackTrace(e))
        }
    }

    suspend fun setValue(key: String, value: String, setOption: SetOption?, seconds: ULong): String? {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val s = client!!.set(key, value, setOption)
            client!!.expire(key, seconds)
            return@withContext s
        }
    }

    suspend fun getValue(key: String): String {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            return@withContext client!!.get(key) ?: throw RedisKeyException()
        }
    }

    suspend fun hashSet(key: String, value: Pair<String, String>) {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            client!!.hset(key, value)
            return@withContext
        }
    }

    suspend fun hashGet(key: String, field: String): String {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            return@withContext client!!.hget(key, field)
                ?: throw RedisKeyException()
        }
    }

    suspend fun hashGetAll(key: String): List<String> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            return@withContext client!!.hgetAll(key)
        }
    }

    suspend fun hashUnset(key: String, field: String) {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            client!!.hdel(key, field)
            return@withContext
        }
    }

    suspend fun unset(key: String) {
        CoroutineScope(Dispatchers.IO).launch {
            client!!.del(key)
        }
    }

    suspend fun rename(oldKey: String, newKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            client!!.rename(oldKey, newKey)
        }
    }

    suspend fun getInfo(): Any? {
        val script = """
            local total_size = 0
            for _, key in ipairs(redis.call('keys', '*')) do
                total_size = total_size + redis.call('memory', 'usage', key)
            end
            return total_size
        """.trimIndent()
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            return@withContext client!!.eval(script, arrayOf("*"), emptyArray<String>(), true)
        }
    }
}
