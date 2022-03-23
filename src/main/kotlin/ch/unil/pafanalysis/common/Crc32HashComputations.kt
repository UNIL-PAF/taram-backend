package ch.unil.pafanalysis.common

import java.util.zip.CRC32
import java.util.zip.Checksum

class Crc32HashComputations {
    fun computeCrc32Hash(objString: String): Long {
        val bytes: ByteArray = objString.toByteArray()
        val checksum: Checksum = CRC32()
        checksum.update(bytes, 0, bytes.size)
        return checksum.value
    }
}