package utils

import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.row
import env.LocaleData

class KeyBoardBuilder(
    private val type: String,
    private val update: CommonMessage<MessageContent>? = null
) {
    fun buildPrivate(isMediaGroup: Boolean): InlineKeyboardMarkup {
        return inlineKeyboard {
            row {
                +CallbackDataInlineKeyboardButton(
                    LocaleData.getI18nString("submission.button.yes"),
                    "$type:yes:${update!!.chat.id.chatId}:${update.messageId}"
                )
                +CallbackDataInlineKeyboardButton(
                    LocaleData.getI18nString("submission.button.no"),
                    "$type:no:${update.chat.id.chatId}:${update.messageId}"
                )
            }
            if (!isMediaGroup) {
                row {
                    +CallbackDataInlineKeyboardButton(
                        LocaleData.getI18nString("submission.button.reply_only"),
                        "$type:reply_only:${update!!.chat.id.chatId}:${update.messageId}"
                    )
                }
            }
            row {
                +CallbackDataInlineKeyboardButton(
                    LocaleData.getI18nString("submission.button.cancel"),
                    "$type:cancel:${update!!.messageId}"
                )
            }
        }
    }

    fun buildGroup(chatId: String, messageId: String, messageType: String, isAnon: Boolean): InlineKeyboardMarkup {
        return inlineKeyboard {
            +CallbackDataInlineKeyboardButton(
                LocaleData.getI18nString("submission.button.group.ok"),
                "$type:ok:$chatId:$messageId:$messageType:$isAnon"
            )
            if (!isAnon) {
                +CallbackDataInlineKeyboardButton(
                    LocaleData.getI18nString("submission.button.group.ok_anon"),
                    "$type:ok_anon:$chatId:$messageId:$messageType:true"
                )
            }
        }
    }
}
