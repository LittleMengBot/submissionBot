package utils

import db.RedisService
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.edit.caption.editMessageCaption
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.media.sendAnimation
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.media.sendVideo
import dev.inmo.tgbotapi.extensions.api.send.resend
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.asPrivateChat
import dev.inmo.tgbotapi.extensions.utils.asTextedInput
import dev.inmo.tgbotapi.extensions.utils.formatting.textMentionMarkdownV2
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.files.PhotoSize
import dev.inmo.tgbotapi.types.files.VideoFile
import dev.inmo.tgbotapi.types.media.MediaGroupMemberTelegramMedia
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.media.TelegramMediaVideo
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.*
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import env.ConfigLoader
import env.LocaleData.getI18nString
import utils.TextUtils.isTelegramBlankName

class SubmissionFactory(
    private val bot: BehaviourContext,
    private val groupChatId: Long,
    private val groupMessageId: Long? = null,
    private val callbackMessageId: Long? = null,
    private val callbackReplyMessageId: Long? = null,
    private val reader: User,
    private val type: String,
    private val comment: String? = null
) {
    @OptIn(PreviewFeature::class, RiskFeature::class)
    suspend fun groupCallback(dataList: List<String>) {
        val p = groupMessageId?.toString() ?: (callbackReplyMessageId?.toString() ?: return)
        val key = MD5Builder(
            "submission:${dataList[4]}:" +
                "${dataList[2]}:" + p
        ).build()

        val compressedSource = try {
            RedisService.getValue("submission:${dataList[4]}:$key")
        } catch (_: RedisService.RedisKeyException) {
            when (type) {
                "command" -> {
                    bot.sendMessage(ChatId(groupChatId), getI18nString("command.ok.error"))
                }
                "callback" -> {
                    bot.edit(ChatId(groupChatId), callbackMessageId!!, getI18nString("command.ok.error"))
                }
            }
            return
        }
        val sentMessage: ContentMessage<MessageContent> = if (dataList[4] == "single") {
            val sourceMessage = MessageSerializer.parseSingleMessage(
                GzipUtils.uncompress(compressedSource)
            )
            if (dataList[5] == "nsfw") {
                reSendMessageAsSpoiler(sourceMessage)
            } else {
                bot.resend(
                    ChatId(ConfigLoader.config!!.channel as Identifier),
                    sourceMessage
                )
            }
        } else {
            val source = MessageSerializer.parseMediaGroup(
                GzipUtils.uncompress(compressedSource)
            )
            if (dataList[5] == "nsfw") {
                bot.sendMediaGroup(
                    ChatId(ConfigLoader.config!!.channel as Identifier),
                    spoilerMediaGroup(source)
                ).content.group[0].sourceMessage
            } else {
                bot.sendMediaGroup(
                    ChatId(ConfigLoader.config!!.channel as Identifier),
                    source
                ).content.group[0].sourceMessage
            }
        }
        RedisService.unset("submission:${dataList[4]}:$key")
        val groupText = StringBuilder()
        groupText.append(getI18nString("submission.group.reader"))
        if (isTelegramBlankName(reader.firstName)) {
            groupText.append(
                "Dear Blank Admin"
                    .textMentionMarkdownV2(reader.id)
            )
        } else {
            groupText.append(
                "${reader.firstName} ${reader.lastName}"
                    .textMentionMarkdownV2(reader.id)
            )
        }
        groupText.append("\n\n${getI18nString("submission.button.success")}")
        if (callbackReplyMessageId != null) {
            bot.edit(
                ChatId(groupChatId),
                RedisService.hashGet(
                    "submission:group:message:" +
                        MD5Builder("submission:group:message:${ConfigLoader.config!!.group}").build(),
                    callbackReplyMessageId.toString()
                ).split(":").last().toLong(),
                groupText.toString(),
                MarkdownV2
            )
        } else {
            bot.edit(
                ChatId(groupChatId),
                RedisService.hashGet(
                    "submission:group:message:" +
                        MD5Builder("submission:group:message:${ConfigLoader.config!!.group}").build(),
                    groupMessageId.toString()
                ).split(":").last().toLong(),
                groupText.toString(),
                MarkdownV2
            )
        }
        RedisService.unset(key)
        val user = bot.getChat(ChatId(dataList[2].toLong())).asPrivateChat()
        if (dataList.last() == "false" || type == "command") {
            if (sentMessage.content is MediaContent
            ) {
                bot.editMessageCaption(
                    chatId = sentMessage.chat.id,
                    messageId = sentMessage.messageId,
                    text = getCommentText(sentMessage, dataList, user)
                )
            } else if (sentMessage.content is TextContent) {
                bot.edit(
                    chatId = sentMessage.chat.id,
                    messageId = sentMessage.messageId,
                    text = getCommentText(sentMessage, dataList, user)
                )
            }
        }
        bot.sendMessage(ChatId(dataList[2].toLong()), getI18nString("submission.finish"))
    }

    @OptIn(PreviewFeature::class)
    private fun getCommentText(
        sentMessage: ContentMessage<MessageContent>,
        dataList: List<String>,
        user: PrivateChat?
    ): String {
        val editText = StringBuilder()
        if (sentMessage.content.asTextedInput()?.text != null) {
            editText.append("${sentMessage.content.asTextedInput()?.text}\n\n")
        }
        if (dataList.last() == "false") {
            editText.append(getI18nString("submission.channel.from"))
            editText.append(" ")
            if (isTelegramBlankName("${user?.firstName}${user?.lastName}")) {
                editText.append("Super Blank Man")
            } else {
                editText.append("${user?.firstName} ${user?.lastName}\n")
            }
        }
        if (comment != null) editText.append(comment)
        return editText.toString()
    }

    private suspend fun reSendMessageAsSpoiler(sourceMessage: MessageContent): ContentMessage<MessageContent> {
        return when (sourceMessage) {
            is PhotoContent -> {
                bot.sendPhoto(
                    ChatId(ConfigLoader.config!!.channel as Identifier),
                    sourceMessage.media,
                    sourceMessage.text,
                    spoilered = true
                )
            }
            is VideoContent -> {
                bot.sendVideo(
                    ChatId(ConfigLoader.config!!.channel as Identifier),
                    sourceMessage.media,
                    sourceMessage.text,
                    spoilered = true

                )
            }
            is AnimationContent -> {
                bot.sendAnimation(
                    ChatId(ConfigLoader.config!!.channel as Identifier),
                    sourceMessage.media,
                    sourceMessage.text,
                    spoilered = true
                )
            }
            else -> {
                bot.resend(
                    ChatId(ConfigLoader.config!!.channel as Identifier),
                    sourceMessage
                )
            }
        }
    }

    private fun spoilerMediaGroup(mediaGroup: List<MediaGroupPartContent>): List<MediaGroupMemberTelegramMedia> {
        val re: MutableList<MediaGroupMemberTelegramMedia> = mutableListOf()
        mediaGroup.forEach { mediaPart ->
            when (mediaPart.media) {
                is PhotoSize -> {
                    re.add(TelegramMediaPhoto(file = mediaPart.media.fileId, text = mediaPart.text, spoilered = true))
                }
                is VideoFile -> {
                    re.add(TelegramMediaVideo(mediaPart.media.fileId, text = mediaPart.text, spoilered = true))
                }
                else -> {
                    re.add(mediaPart as MediaGroupMemberTelegramMedia)
                }
            }
        }
        return re
    }
}
