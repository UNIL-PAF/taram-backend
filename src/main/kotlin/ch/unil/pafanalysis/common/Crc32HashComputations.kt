package ch.unil.pafanalysis.common

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.sql.Timestamp
import java.util.zip.CRC32
import java.util.zip.Checksum


class Crc32HashComputations {

    private val crc: Checksum = CRC32()

    fun computeStringHash(objString: String?): Long? {
        return if(objString != null){
            val bytes: ByteArray = objString.toByteArray()
            crc.update(bytes, 0, bytes.size)
            crc.value
        }else{
            null
        }
    }

    fun getRandomHash(): Long {
        val currentTime = Timestamp(System.currentTimeMillis()).toString()
        val bytes: ByteArray = currentTime.toByteArray()
        crc.update(bytes, 0, bytes.size)
        return crc.value
    }

    fun computeFileHash(file: File): Long {
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