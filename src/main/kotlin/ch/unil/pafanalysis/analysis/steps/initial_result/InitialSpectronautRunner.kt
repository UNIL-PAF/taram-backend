package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import org.springframework.stereotype.Service

class InitialSpectronautRunner() {

    fun createInitialSpectronautResult(
        spectronautPath: String?,
        fileName: String?,
        table: Table?
    ): InitialResult {

        println(spectronautPath)
        println(fileName)

        val fastaFileCol = table?.headers?.find { it.name?.contains("fastafile", ignoreCase = true) ?: false }
        val fastaFiles: List<String>? =
            if (fastaFileCol != null){
                val fastas = ReadTableData().getStringColumn(table, fastaFileCol.name!!)
                fastas?.fold(emptyList<String>()){ a, v ->
                    val names = v.split(";")
                    names.plus(a).distinct()
                }
            } else null
        return InitialResult(
            fastaFiles = fastaFiles
        )
    }

}
