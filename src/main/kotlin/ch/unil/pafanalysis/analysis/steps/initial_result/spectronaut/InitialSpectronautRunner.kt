package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResult
import ch.unil.pafanalysis.common.ReadTableData
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
