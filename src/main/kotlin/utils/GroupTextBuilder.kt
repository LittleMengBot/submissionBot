package utils

import dev.inmo.tgbotapi.extensions.utils.formatting.textMentionMarkdownV2
import dev.inmo.tgbotapi.types.chat.PrivateChat
import env.LocaleData.getI18nString

class GroupTextBuilder(
    private val isAnon: Boolean,
    private val user: PrivateChat,
    private val sourceChannel: String? = null
) {
    fun build(
        submissionInfo: Boolean = false,
        newSubmission: Boolean = true,
        submissionFrom: Boolean = true,
        forwardFrom: Boolean = true,
        keepSource: Boolean = true
    ): String {
        val sb = StringBuilder()
        if (submissionInfo) sb.append("${getI18nString("submission.group.info")}\n")
        if (newSubmission) sb.append("${getI18nString("submission.group.new")}\n")
        if (submissionFrom) {
            sb.append(getI18nString("submission.group.people"))
            if (TextUtils.isTelegramBlankName(user.firstName)) {
                sb.append("Super Blank Man (user_id: ${user.id.chatId})".textMentionMarkdownV2(user.id) + "\n")
            } else {
                sb.append("${user.firstName} ${user.lastName}".textMentionMarkdownV2(user.id) + "\n")
            }
        }
        if (sourceChannel != null && forwardFrom) {
            sb.append(getI18nString("submission.group.from_channel").format(sourceChannel) + "\n")
        }
        if (keepSource) {
            sb.append(
                getI18nString("submission.group.from_status").format(
                    if (isAnon) getI18nString("submission.button.no") else getI18nString("submission.button.yes")
                )
            )
            sb.append("\n\n" + getI18nString("submission.group.help"))
        }
        return sb.toString()
    }
}
