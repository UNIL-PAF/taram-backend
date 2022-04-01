package ch.unil.pafanalysis.common

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.CRC32
import java.util.zip.Checksum


class Crc32HashComputations {
    fun computeStringHash(objString: String): Long {
        val bytes: ByteArray = objString.toByteArray()
        val checksum: Checksum = CRC32()
        checksum.update(bytes, 0, bytes.size)
        return checksum.value
    }

    fun computeFileHash(file: File): Long {
        val crc = CRC32()
        val buffer: ByteBuffer = ByteBuffer.allocate(1024)
        var len = 0
        Files.newByteChannel(file.toPath(), StandardOpenOption.READ).use { input ->
            while (input.read(buffer).also {
                    len = it
                } > 0) {
                buffer.flip()
                crc.update(buffer.array(), 0, len)
            }
        }
        return crc.value
    }
}