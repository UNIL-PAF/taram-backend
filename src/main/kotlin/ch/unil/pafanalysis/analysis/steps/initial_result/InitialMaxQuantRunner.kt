package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import org.springframework.stereotype.Service
import java.io.File

class InitialMaxQuantRunner() {

    fun createInitialMaxQuantResult(maxQuantPath: String?, fileName: String?): InitialResult {
        val (mqParams, fastaFiles, version) = parseMaxquantParameters(maxQuantPath.plus(fileName))
        return InitialResult(
            maxQuantParameters = mqParams,
            fastaFiles = fastaFiles,
            softwareVersion = "MaxQuant $version"
        )
    }

    private fun parseMaxquantParameters(parametersTable: String): Triple<MaxQuantParameters, List<String>?, String?> {
        val paramsFile = File(parametersTable)
        if (!paramsFile.exists()) throw StepException("Could not find parameters.txt in results directory.")
        val pMap: HashMap<String, String> = HashMap<String, String>()
        paramsFile.useLines { lines ->
            lines.forEach {
                val (n, v) = it.split("\t")
                pMap[n] = v
            }
        }

        val matchBetweenRuns = pMap["Match between runs"] == "True"
        val fastaFiles = pMap["Fasta file"]?.split(";")?.map { it.replace(Regex(".+\\\\(.+)\\.fasta"), "$1") }
        val mqParams = MaxQuantParameters(matchBetweenRuns = matchBetweenRuns)
        return Triple(mqParams, fastaFiles, pMap["Version"])
    }

}
