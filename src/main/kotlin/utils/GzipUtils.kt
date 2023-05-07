package utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipUtils {
    fun compress(source: String): String {
        val outputStream = ByteArrayOutputStream()
        val gzipStream = GZIPOutputStream(outputStream)
        val writer = OutputStreamWriter(gzipStream, StandardCharsets.UTF_8)
        writer.write(source)
        writer.flush()
        gzipStream.finish()
        val compressedBytes = outputStream.toByteArray()
        val lengthBytes = ByteBuffer.allocate(4).putInt(source.length).array()
        val resultBytes = ByteArray(lengthBytes.size + compressedBytes.size)
        System.arraycopy(lengthBytes, 0, resultBytes, 0, lengthBytes.size)
        System.arraycopy(compressedBytes, 0, resultBytes, lengthBytes.size, compressedBytes.size)
        return resultBytes.toString(StandardCharsets.ISO_8859_1)
    }

    fun uncompress(source: String): String {
        val sourceBytes = source.toByteArray(StandardCharsets.ISO_8859_1)
        val lengthBytes = ByteArray(4)
        System.arraycopy(sourceBytes, 0, lengthBytes, 0, lengthBytes.size)
        val compressedBytes = ByteArray(sourceBytes.size - lengthBytes.size)
        System.arraycopy(sourceBytes, lengthBytes.size, compressedBytes, 0, compressedBytes.size)
        return InputStreamReader(GZIPInputStream(ByteArrayInputStream(compressedBytes)), StandardCharsets.UTF_8)
            .readText()
    }
}
