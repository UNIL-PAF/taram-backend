package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResult
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import org.apache.commons.io.filefilter.WildcardFileFilter
import java.io.File
import java.io.FileFilter


class InitialSpectronautRunner() {

    fun createInitialSpectronautResult(
        spectronautPath: String?,
        table: Table?
    ): InitialResult {
        val fastaFileCol = table?.headers?.find { it.name?.contains("fastafile", ignoreCase = true) ?: false }
        val fastaFiles: List<String>? =
            if (fastaFileCol != null) {
                val fastas = ReadTableData().getStringColumn(table, fastaFileCol.name!!)
                fastas?.fold(emptyList<String>()) { a, v ->
                    val names = v.split(";")
                    names.plus(a).distinct()
                }
            } else null

        return InitialResult(
            fastaFiles = fastaFiles,
            softwareVersion = parseSoftwareVersion(spectronautPath)
        )
    }

    private fun parseSoftwareVersion(spectronautPath: String?): String? {
        val dir = File(spectronautPath)
        val fileFilter: FileFilter = WildcardFileFilter("*.setup.txt")
        val files = dir.listFiles(fileFilter)
        val setupFile = if (files.isNotEmpty()) files[0] else null
        return setupFile?.readLines()?.find { it.contains(Regex("^Spectronaut.+")) }
    }

}
