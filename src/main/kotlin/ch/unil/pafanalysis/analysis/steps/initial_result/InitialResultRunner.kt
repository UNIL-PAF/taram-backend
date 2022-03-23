package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.service.ColumnInfoService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultType
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
class InitialResultRunner(): CommonStep() {

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var analysisRepository: AnalysisRepository? = null

    @Autowired
    private var columnInfoService: ColumnInfoService? = null

    override var type: AnalysisStepType? = AnalysisStepType.INITIAL_RESULT
    private val gson = Gson()

    fun run(analysisId: Int?, result: Result?): String {
        val resultType = if (result?.type == ResultType.MaxQuant.value) ResultType.MaxQuant else ResultType.Spectronaut

        val outputRoot =
            env?.getProperty(if (resultType == ResultType.MaxQuant) "output.path.maxquant" else "output.path.spectronaut")
        val resultPath =
            env?.getProperty(if (resultType == ResultType.MaxQuant) "result.path.maxquant" else "result.path.spectronaut") + result?.path
        val outputPath: String = analysisId.toString()

        val analysis = if (analysisId == null) null else analysisRepository?.findById(analysisId)
        val newStep: AnalysisStep? = createEmptyAnalysisStep(analysis)

        createResultDir(outputRoot?.plus(outputPath))
        val stepPath = "$outputPath/${newStep?.id}"
        createResultDir(outputRoot?.plus(stepPath))
        val newTable = copyResultsTable(outputRoot?.plus(stepPath) + "/" + (result?.resFile), resultPath, resultType)

        val step: AnalysisStep? = try {
            val initialResult = createInitialResult(resultPath, result?.resFile, resultType)
            val columnInfo = columnInfoService?.createAndSaveColumnInfo(resultPath + "/" + result?.resFile, resultPath, resultType)

            newStep?.copy(
                resultPath = stepPath,
                resultTablePath = "$stepPath/${newTable?.name}",
                status = AnalysisStepStatus.DONE.value,
                results = gson.toJson(initialResult),
                columnInfo = columnInfo
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

    private fun createInitialResult(resultPath: String?, resultFilename: String?, type: ResultType): InitialResult {
        if (type == ResultType.MaxQuant) {
            return createInitialMaxQuantResult(resultPath, resultFilename)
        } else {
            return createInitialSpectronautResult(resultPath, resultFilename)
        }
    }

    private fun createInitialSpectronautResult(spectronautPath: String?, fileName: String?): InitialResult {
        return InitialResult(
            nrProteinGroups = getNrProteinGroups(spectronautPath.plus(fileName))
        )
    }

    private fun createInitialMaxQuantResult(maxQuantPath: String?, fileName: String?): InitialResult {
        return InitialResult(
            maxQuantParameters = parseMaxquantParameters(maxQuantPath.plus(fileName)),
            nrProteinGroups = getNrProteinGroups(maxQuantPath + "proteinGroups.txt")
        )
    }

    private fun copyResultsTable(outputPath: String?, resultPath: String, type: ResultType): File {
        val timestamp = Timestamp(System.currentTimeMillis())
        if (type == ResultType.MaxQuant) {
            return copyProteinGroupsTable(outputPath, resultPath, timestamp)
        } else {
            return copySpectronautResultTable(outputPath, resultPath, timestamp)
        }
    }

    private fun copySpectronautResultTable(outputPath: String?, spectronautPath: String?, timestamp: Timestamp): File {
        val originalTable = File(spectronautPath)
        return originalTable.copyTo(File(outputPath + "/Report_" + timestamp.time + ".txt"), overwrite = true)
    }

    private fun copyProteinGroupsTable(outputPath: String?, maxQuantPath: String?, timestamp: Timestamp): File {
        val originalTable = File(maxQuantPath + "proteinGroups.txt")
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


}
