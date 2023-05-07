package utils

import dev.inmo.tgbotapi.types.message.content.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

object MessageSerializer {
    val json = Json {
        serializersModule = SerializersModule {
            polymorphic(MediaGroupPartContent::class) {
                subclass(PhotoContent::class, PhotoContent.serializer())
                subclass(AudioContent::class, AudioContent.serializer())
                subclass(DocumentContent::class, DocumentContent.serializer())
                subclass(VideoContent::class, VideoContent.serializer())
            }
            polymorphic(MessageContent::class) {
                subclass(TextContent::class, TextContent.serializer())
                subclass(PhotoContent::class, PhotoContent.serializer())
                subclass(AudioContent::class, AudioContent.serializer())
                subclass(DocumentContent::class, DocumentContent.serializer())
                subclass(VideoContent::class, VideoContent.serializer())
                subclass(DiceContent::class, DiceContent.serializer())
                subclass(PollContent::class, PollContent.serializer())
                subclass(LocationContent::class, LocationContent.serializer())
                subclass(InvoiceContent::class, InvoiceContent.serializer())
                subclass(ContactContent::class, ContactContent.serializer())
                subclass(StickerContent::class, StickerContent.serializer())
                subclass(VideoNoteContent::class, VideoNoteContent.serializer())
                subclass(AnimationContent::class, AnimationContent.serializer())
                subclass(VenueContent::class, VenueContent.serializer())
            }
        }
    }
    fun parseMediaGroup(source: String): List<MediaGroupPartContent> {
        return json.decodeFromString<List<MediaGroupPartContent>>(source)
    }

    fun parseSingleMessage(source: String): MessageContent {
        return json.decodeFromString<MessageContent>(source)
    }
}
