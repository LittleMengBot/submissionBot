package handler

import base.BaseMessageHandler
import db.RedisService
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.tgbotapi.bot.exceptions.RequestException
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.forwardMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.resend
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_to_message
import dev.inmo.tgbotapi.extensions.utils.formatting.linkMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.formatting.textMentionMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.ifPrivateChat
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.ForwardInfo
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.types.message.PrivateContentMessageImpl
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import env.ConfigLoader
import env.LocaleData.getI18nString
import io.github.crackthecodeabhi.kreds.args.SetOption
import mu.KotlinLogging
import service.BlockedUser
import utils.*

class CallbackHandler(
    private val bot: BehaviourContext,
    private val callback: DataCallbackQuery
) : BaseMessageHandler() {

    private val logger = KotlinLogging.logger {}

    init {
        userId = callback.from.id.chatId
    }

    @OptIn(RiskFeature::class)
    override suspend fun handle() {
        val data = this.callback.data
        val key = MD5Builder(
            "submission:callback:${callback.from.id.chatId}:${callback.message!!.messageId}"
        ).build()
        if (RedisService.setValue(
                "submission:callback:$key",
                "0",
                SetOption.Builder(nx = true).build(),
                30u
            )
            == null
        ) {
            bot.answer(callback, text = getI18nString("callback.alert"), showAlert = true, cachedTimeSeconds = 3)
            return
        }
        when {
            data.startsWith("single") -> {
                try {
                    this.singleMessage(data.split(":"))
                } catch (e: RequestException) {
                    this.errorProcess(e)
                } catch (e: RedisService.RedisKeyException) {
                    this.errorProcess(e)
                }
            }

            data.startsWith("mediagroup") -> {
                try {
                    this.mediaGroupMessage(data.split(":"))
                } catch (e: RequestException) {
                    this.errorProcess(e)
                } catch (e: RedisService.RedisKeyException) {
                    this.errorProcess(e)
                }
            }

            data.startsWith("group") -> {
                runCatching {
                    SubmissionFactory(
                        bot,
                        groupChatId = callback.message!!.chat.id.chatId,
                        callbackMessageId = callback.message!!.messageId,
                        callbackReplyMessageId = callback.message!!.reply_to_message!!.messageId,
                        reader = callback.from,
                        type = "callback"
                    ).groupCallback(data.split(":"))
                }.onFailure {
                    logger.error(ErrorHandler.parseStackTrace(it))
                    ErrorHandler.sendErrorLog(bot, "SubmissionFactory Error.\nData:$data\nError:${it.message}")
                }
            }
        }
    }

    @OptIn(RiskFeature::class)
    private suspend fun singleMessage(dataList: List<String>) {
        when {
            dataList[1] == "yes" || dataList[1] == "no" -> {
                val key = MD5Builder(
                    "submission:single:" +
                        "${dataList[2]}:" +
                        dataList[3]
                ).build()
                val sourceRaw = RedisService.getValue("submission:single:$key")
                val source = GzipUtils.uncompress(sourceRaw)
                bot.edit(
                    callback.message!!.chat.id,
                    callback.message!!.messageId,
                    getI18nString("submission.button.success")
                )
                val message = bot.resend(
                    ChatId(ConfigLoader.config!!.group),
                    MessageSerializer.parseSingleMessage(source)
                )
                val newKey = MD5Builder(
                    "submission:single:" +
                        "${dataList[2]}:" +
                        message.messageId
                ).build()
                RedisService.rename("submission:single:$key", "submission:single:$newKey")
                var isAnon = true
                if (dataList[1] == "yes") isAnon = false
                val replyMessage = replyInGroup(message, isAnon, dataList, "single")
                this.archiveMessageId(message, replyMessage)
            }

            dataList[1] == "cancel" -> {
                bot.edit(
                    callback.message!!.chat.id,
                    callback.message!!.messageId,
                    getI18nString("submission.button.canceled")
                )
            }

            dataList[1] == "reply_only" -> {
                bot.forwardMessage(
                    ChatId(dataList[2].toLong()),
                    ChatId(ConfigLoader.config!!.group),
                    dataList[3].toLong()
                )
                bot.edit(
                    callback.message!!.chat.id,
                    callback.message!!.messageId,
                    getI18nString("command.reply.success")
                )
            }
        }
    }

    @OptIn(RiskFeature::class)
    private suspend fun mediaGroupMessage(dataList: List<String>) {
        when {
            dataList[1] == "yes" || dataList[1] == "no" -> {
                val key = MD5Builder(
                    "submission:mediagroup:" +
                        "${dataList[2]}:" +
                        dataList[3]
                ).build()
                val sourceRaw = RedisService.getValue("submission:mediagroup:$key")
                val source = GzipUtils.uncompress(sourceRaw)
                bot.edit(
                    callback.message!!.chat.id,
                    callback.message!!.messageId,
                    getI18nString("submission.button.success")
                )
                val message = bot.sendMediaGroup(
                    ChatId(ConfigLoader.config!!.group),
                    MessageSerializer.parseMediaGroup(source)
                ).content.group[0].sourceMessage

                val newKey = MD5Builder(
                    "submission:mediagroup:" +
                        "${dataList[2]}:" +
                        message.messageId
                ).build()
                RedisService.rename("submission:mediagroup:$key", "submission:mediagroup:$newKey")
                var isAnon = true
                if (dataList[1] == "yes") isAnon = false
                val replyMessage = replyInGroup(message, isAnon, dataList, "mediagroup")
                this.archiveMessageId(message, replyMessage)
            }

            dataList[1] == "cancel" -> {
                val key = MD5Builder(
                    "submission:mediagroup:" +
                        "${callback.message!!.chat.id}:" +
                        dataList[2]
                ).build()
                bot.edit(
                    callback.message!!.chat.id,
                    callback.message!!.messageId,
                    getI18nString("submission.button.canceled")
                )
                RedisService.unset("submission:mediagroup:$key")
            }
        }
    }

    @OptIn(RiskFeature::class)
    override suspend fun errorProcess(e: Throwable) {
        callback.message!!.chat.ifPrivateChat {
            if (e is RedisService.RedisKeyException) {
                bot.edit(
                    callback.message!!.chat.id,
                    callback.message!!.messageId,
                    getI18nString("submission.redis_key.error")
                )
                bot.answer(callback, text = getI18nString("submission.redis_key.error"), showAlert = true)
                return
            } else {
                bot.sendMessage(
                    callback.message!!.chat.id,
                    getI18nString("submission.error")
                )
            }
        }
        bot.sendMessage(
            callback.message!!.chat.id,
            getI18nString("submission.error")
        )
        ErrorHandler.sendErrorLog(
            bot,
            "${e.message}\n\n" +
                "on chat: " +
                "${callback.from.firstName} ${callback.from.lastName}".textMentionMarkdownV2(callback.from.id)
        )
        logger.error(ErrorHandler.parseStackTrace(e))
        return
    }

    @OptIn(RiskFeature::class)
    private suspend fun archiveMessageId(
        message: ContentMessage<MessageContent>,
        replyMessage: ContentMessage<MessageContent>
    ) {
        RedisService.hashSet(
            "submission:group:message:" +
                MD5Builder("submission:group:message:${ConfigLoader.config!!.group}").build(),
            Pair(message.messageId.toString(), "${callback.message!!.chat.id.chatId}:${replyMessage.messageId}")
        )
    }

    @OptIn(RiskFeature::class)
    private fun getSourceChannel(): String? {
        return when (
            val forwardInfo =
                (callback.message?.reply_to_message as? PrivateContentMessageImpl<*>)?.forwardInfo
        ) {
            is ForwardInfo.PublicChat.FromChannel -> {
                if (forwardInfo.channelChat.username != null) {
                    forwardInfo.channelChat.title
                        .linkMarkdownV2(
                            "https://t.me/" +
                                "${forwardInfo.channelChat.username?.usernameWithoutAt}/" +
                                "${forwardInfo.messageId}"
                        )
                } else {
                    forwardInfo.channelChat.title + "\\(PrivateChannel\\)\n"
                }
            }

            is ForwardInfo.PublicChat.SentByChannel -> {
                forwardInfo.channelChat.title
                    .linkMarkdownV2(
                        "https://t.me/" +
                            "${forwardInfo.channelChat.username?.usernameWithoutAt}"
                    ) + "\\(Premium\\)\n"
            }

            is ForwardInfo.PublicChat.FromSupergroup -> {
                forwardInfo.group.title + "\\(FromSupergroup\\)\n"
            }

            is ForwardInfo.ByUser -> {
                "${forwardInfo.from.firstName} ${forwardInfo.from.lastName}\n"
            }

            is ForwardInfo.ByAnonymous -> {
                forwardInfo.senderName + "\\(Private\\)\n"
            }

            else -> null
        }
    }

    private suspend fun replyInGroup(
        message: ContentMessage<MessageContent>,
        isAnon: Boolean,
        dataList: List<String>,
        messageType: String
    ): ContentMessage<TextContent> {
        bot.reply(
            message,
            GroupTextBuilder(isAnon, callback.from, this.getSourceChannel()).build(
                submissionInfo = true,
                newSubmission = false
            ),
            parseMode = MarkdownV2
        )
        return bot.reply(
            message,
            GroupTextBuilder(isAnon, callback.from, this.getSourceChannel()).build(
                submissionFrom = false,
                forwardFrom = false,
                keepSource = false
            ),
            replyMarkup = KeyBoardBuilder("group", null)
                .buildGroup(dataList[2], message, messageType, isAnon),
            parseMode = MarkdownV2
        )
    }

    override suspend fun onBlock() {
        if (BlockedUser.checkBlockStatus(userId) && !super.isAdmin(userId)) {
            bot.answer(callback, text = getI18nString("blacklist.warn"), showAlert = true, cachedTimeSeconds = 3)
            throw UserInBlockListException()
        }
    }
}
