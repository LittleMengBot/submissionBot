package base

open class BaseHandler {
    open suspend fun run() {
        runCatching {
            handle()
        }.onFailure { e ->
            errorProcess(e)
        }
    }
    open suspend fun handle() {}
    open suspend fun errorProcess(e: Throwable) {}
}
