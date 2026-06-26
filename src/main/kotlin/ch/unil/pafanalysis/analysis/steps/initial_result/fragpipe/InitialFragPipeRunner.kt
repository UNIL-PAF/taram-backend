package ch.unil.pafanalysis.analysis.steps.initial_result.fragpipe

import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResult
import java.io.File

object InitialFragPipeRunner {

    fun createInitialResult(path: String?, fileName: String?): InitialResult {
        val resultMap = parseParameters(path.plus(fileName))
        return InitialResult(
            fastaFiles = listOf(resultMap["database.db-path"] ?: ""),
            softwareVersion = "FragPipe ${resultMap["FragPipe version"]}"
        )
    }

    private fun parseParameters(parametersTable: String): Map<String, String> {
        val paramsFile = File(parametersTable)
        if (!paramsFile.exists()) throw StepException("Could not find fragpipe.workflow in results directory.")
        val results = mutableMapOf<String, String>()
        val patterns = listOf(
            Regex("""#\s*(FragPipe\s+version)\s+([\d\\.]+)"""),
            Regex("""(database.db-path).+\\\\([\w.-]+)"""),
            )

        paramsFile.forEachLine { line ->
            for (regex in patterns) {
                val match = regex.find(line)
                if (match != null && match.groupValues.size > 2) {
                    results[match.groupValues[1]] = match.groupValues[2]
                }
            }
        }
        return results.toMap()
    }

}
