package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.results.model.Result
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.sql.Timestamp
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
        val outputRoot = env?.getProperty("output.path.maxquant")
        val maxQuantPath = env?.getProperty("result.path.maxquant") + result?.path
        val outputPath: String = analysisId.toString()

        val analysis = if (analysisId == null) null else analysisRepository?.findById(analysisId)
        val newStep: AnalysisStep? = createEmptyAnalysisStep(analysis)

        createResultDir(outputRoot?.plus(outputPath))
        val stepPath = "$outputPath/${newStep?.id}"
        createResultDir(outputRoot?.plus(stepPath))
        val newTable = copyProteinGroupsTable(outputRoot?.plus(stepPath), maxQuantPath)

        val step: AnalysisStep? = try {
            val initialResult = createInitialResult(maxQuantPath)
            val experiments = getExperiments(maxQuantPath + "summary.txt")
            val columnMapping = ColumnMapping(experiments = experiments)

            newStep?.copy(
                resultPath = stepPath,
                resultTablePath = "$stepPath/${newTable?.name}",
                status = AnalysisStepStatus.DONE.value,
                results = gson.toJson(initialResult),
                columnMapping = columnMapping
            )
        } catch (e: StepException) {
            newStep?.copy(status = AnalysisStepStatus.ERROR.value, error = e.message)
        }

        if (step != null) {
            analysisStepRepository?.save(step)
            return "done"
        } else {
            throw RuntimeException("Could not create/save initial_result.")
        }
    }

    private fun createEmptyAnalysisStep(analysis: Analysis?): AnalysisStep? {
        val newStep = AnalysisStep(
            status = AnalysisStepStatus.IDLE.value,
            type = type,
            analysis = analysis,
            lastModifDate = LocalDateTime.now()
        )
        return analysisStepRepository?.save(newStep)
    }

    private fun createInitialResult(maxQuantPath: String?): InitialResult {
        return InitialResult(
            maxQuantParameters = parseMaxquantParameters(maxQuantPath + "parameters.txt"),
            nrProteinGroups = getNrProteinGroups(maxQuantPath + "proteinGroups.txt")
        )
    }

    private fun copyProteinGroupsTable(outputPath: String?, maxQuantPath: String?): File {
        val originalTable = File(maxQuantPath + "proteinGroups.txt")
        val timestamp = Timestamp(System.currentTimeMillis())
        return originalTable.copyTo(File(outputPath + "/proteinGroups_" + timestamp.time + ".txt"), overwrite = true)
    }

    private fun getNrProteinGroups(proteinGroupsTable: String): Int {
        val reader = BufferedReader(FileReader(proteinGroupsTable))
        var lines = 0
        while (reader.readLine() != null) lines++
        reader.close()
        lines--
        return lines
    }

    private fun parseMaxquantParameters(parametersTable: String): MaxQuantParameters {
        val paramsFile = File(parametersTable)
        if (!paramsFile.exists()) throw StepException("Could not find parameters.txt in the results directory.")
        val pMap: HashMap<String, String> = HashMap<String, String>()
        paramsFile.useLines { lines ->
            lines.forEach {
                val (n, v) = it.split("\t")
                pMap[n] = v
            }
        }

        val matchBetweenRuns = pMap["Match between runs"] == "True"
        return MaxQuantParameters(version = pMap["Version"], matchBetweenRuns = matchBetweenRuns)
    }

    private fun createResultDir(outputPath: String?): String {
        if (outputPath == null) throw RuntimeException("There is no output path defined.")
        val dir = File(outputPath)
        if (!dir.exists()) dir.mkdir()
        return outputPath
    }

    private fun getExperiments(summaryTable: String): List<String> {
        val lines: List<String> = File(summaryTable).bufferedReader().readLines()
        val headers: List<String> = lines[0].split("\t")
        val expIdx = headers.indexOf("Experiment")
        return lines.subList(1, lines.size-1).map { it.split("\t")[expIdx] }
    }
}
