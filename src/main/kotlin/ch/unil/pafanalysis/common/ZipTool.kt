package ch.unil.pafanalysis.common

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class ZipTool {

    fun zipDir(
        dir: String,
        fileName: String? = null
    ): String {
        val zipFile = "/tmp/a.zip"
        val fos = FileOutputStream(zipFile)
        val zipOut = ZipOutputStream(fos)
        val fileToZip = File(dir)

        zipFile(fileToZip, fileToZip.name, zipOut)
        zipOut.close()
        fos.close()
        return zipFile
    }

    private fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isHidden) {
            return
        }
        if (fileToZip.isDirectory) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(ZipEntry(fileName))
                zipOut.closeEntry()
            } else {
                zipOut.putNextEntry(ZipEntry("$fileName/"))
                zipOut.closeEntry()
            }
            val children: Array<File> = fileToZip.listFiles()
            for (childFile in children) {
                zipFile(childFile, fileName + "/" + childFile.name, zipOut)
            }
            return
        }
        val fis = FileInputStream(fileToZip)
        val zipEntry = ZipEntry(fileName)
        zipOut.putNextEntry(zipEntry)
        val bytes = ByteArray(1024)
        var length: Int
        while (fis.read(bytes).also { length = it } >= 0) {
            zipOut.write(bytes, 0, length)
        }
        fis.close()
    }

}