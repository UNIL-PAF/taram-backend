package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.service.ResultRepository
import com.google.gson.Gson
import org.hibernate.dialect.function.StandardAnsiSqlAggregationFunctions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime

@Service
class InitialResultRunner {

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var analysisRepository: AnalysisRepository? = null

    @Autowired
    private var analysisStepRepository: AnalysisStepRepository? = null

    private val type = AnalysisStepType.INITIAL_RESULT.value
    private val gson = Gson()

    fun run(analysisId: Int?, result: Result?): String {
        val resDir = env?.getProperty("output.path.maxquant")?.plus(analysisId)
        val maxQuantPath = env?.getProperty("result.path.maxquant") + result?.path
        val outputPath: String = this.createResultDir(resDir)
        val newTable = copyProteinGroupsTable(outputPath, maxQuantPath)

        val analysis = if (analysisId == null) null else analysisRepository?.findById(analysisId)
        val lastModif = LocalDateTime.now()

        val step: AnalysisStep = try {
            val initialResult = createInitialResult(maxQuantPath)

            AnalysisStep(
                resultTablePath = newTable.name,
                status = AnalysisStepStatus.DONE.value,
                type = type,
                analysis = analysis,
                lastModifDate = lastModif,
                results = gson.toJson(initialResult)
            )
        } catch (e: StepException) {
            AnalysisStep(
                status = AnalysisStepStatus.ERROR.value,
                type = type,
                error = e.message,
                analysis = analysis,
                lastModifDate = lastModif
            )
        }

        analysisStepRepository?.save(step)
        return "done"
    }

    private fun createInitialResult(maxQuantPath: String?): InitialResult {
        val parametersTable: String = maxQuantPath + "parameters.txt"

        val maxQuantParameters: MaxQuantParameters = this.parseMaxquantParameters(parametersTable)
        return InitialResult(
            maxQuantParameters = maxQuantParameters
        )
    }

    private fun copyProteinGroupsTable(outputPath: String, maxQuantPath: String?): File {
        val originalTable = File(maxQuantPath + "proteinGroups.txt")
        return originalTable.copyTo(File(outputPath + "proteinGroups_" + type + ".txt"), overwrite = true)
    }

    fun parseMaxquantParameters(parametersTable: String): MaxQuantParameters {
        val paramsFile = File(parametersTable)
        if (!paramsFile.exists()) throw StepException("Could not find parameters.txt in the results directory.")
        val pMap: HashMap<String, String> = HashMap<String, String>()
        paramsFile.useLines { lines ->
            lines.forEach {
                val (n, v) = it.split("\t")
                pMap[n] = v
            }
        }

        val matchBetweenRuns = if (pMap["Match between runs"] == "True") true else false
        return MaxQuantParameters(version = pMap["Version"], matchBetweenRuns = matchBetweenRuns)
    }

    fun createResultDir(outputPath: String?): String {
        if (outputPath == null) throw RuntimeException("There is no output path defined.")
        val dir: File = File(outputPath)
        if (!dir.exists()) dir.mkdir()
        return outputPath
    }
}