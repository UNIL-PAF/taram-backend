package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResult
import ch.unil.pafanalysis.common.Table
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileFilter

@Service
class InitialSpectronautRunner() {

    @Autowired
    private val parser: ParseSpectronautSetup? = null

    fun matchSpectronautGroups(columnInfo: ColumnInfo?, spectronautSetup: SpectronautSetup): ColumnInfo? {
        val newExpDetails = columnInfo?.columnMapping?.experimentNames?.fold(emptyMap<String, ExpInfo>()){ acc, expName ->

            val matched = spectronautSetup.runs?.filter{ it.name?.contains(expName) == true }

            val condition = if(matched?.size == 1) {
                matched?.get(0).condition
            } else if(!matched.isNullOrEmpty() && matched.all{!it.name.isNullOrEmpty()}){
                val (commonStart, commonEnd) = ParseSpectronautColNames.getCommonStartAndEnd(matched.map{it.name!!} )
                matched.find{a -> a.name?.matches(Regex(".*$commonStart$commonEnd.*")) ?: false }?.condition
            } else null

            val newEntry = columnInfo?.columnMapping?.experimentDetails?.get(expName)?.copy(group = condition)
            acc.plus(Pair(expName, newEntry!!))
        }
        val groupsOrdered = spectronautSetup.runs?.map{it.condition}?.filterNotNull()?.distinct()?.sorted()
        val newColMapping = columnInfo?.columnMapping?.copy(experimentDetails = newExpDetails, groupsOrdered = groupsOrdered)
        return columnInfo?.copy(columnMapping = newColMapping)
    }

    fun createInitialSpectronautResult(
        spectronautPath: String?,
        table: Table?
    ): InitialResult {
        val spectronautSetup = parseSetupFile(spectronautPath)

        return InitialResult(
            fastaFiles = spectronautSetup?.proteinDBs?.map{it.fileName ?: ""},
            softwareVersion = spectronautSetup?.softwareVersion,
            spectronautSetup = spectronautSetup
        )
    }

    private fun parseSetupFile(spectronautPath: String?): SpectronautSetup? {
        val dir = File(spectronautPath)
        val fileFilter: FileFilter = WildcardFileFilter("*.setup.txt")
        val files = dir.listFiles(fileFilter)
        val setupFile = if (files.isNotEmpty()) files[0] else null
        return if(setupFile != null) parser?.parseSetup(setupFile) else null
    }

}
