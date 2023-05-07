package type

data class ConfigData(
    val token: String,
    val group: Long,
    val channel: Any,
    val admin: Long,
    val lang: String,
    val redisHost: String
)
