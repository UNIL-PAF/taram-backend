package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.ColumnInfoService
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.reflect.Type
import java.sql.Timestamp
import java.time.LocalDateTime

@Service
class InitialResultRunner() : CommonStep(), CommonRunner {

    @Autowired
    private var columnInfoService: ColumnInfoService? = null

    override var type: AnalysisStepType? = AnalysisStepType.INITIAL_RESULT
    private val gson = Gson()

    override fun createPdf(step: AnalysisStep): List<Paragraph> {
        val title = Paragraph().add(Text(step.type).setBold())
        val initialResult = gson.fromJson(step.results, InitialResult::class.java)
        val nrResults = Paragraph().add(Text("Number of protein groups: ${initialResult.nrProteinGroups}"))
        return listOf(title, nrResults)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?): AnalysisStep {
        throw Exception("InitialResultRunner does not implement ordinary run function.")
    }

    fun run(analysisId: Int?, result: Result?): AnalysisStep? {
        val analysis =
            analysisRepository?.findById(analysisId ?: throw StepException("No valid analysisId was provided."))
        setPathes(analysis)
        val emptyStep = createEmptyInitialResult(analysis)
        val stepPath = setMainPaths(analysis, emptyStep)

        val newTable = copyResultsTable(outputRoot?.plus(stepPath), result?.resFile, resultPath)
        val newTableHash = Crc32HashComputations().computeFileHash(newTable)

        val step: AnalysisStep? = try {
            val initialResult = createInitialResult(resultPath, result?.resFile, resultType)
            val columnParseRes =
                columnInfoService?.createAndSaveColumnInfo(resultPath + "/" + result?.resFile, resultPath, resultType)

            emptyStep?.copy(
                resultPath = stepPath,
                resultTablePath = "$stepPath/${newTable?.name}",
                resultTableHash = newTableHash,
                status = AnalysisStepStatus.DONE.value,
                results = gson.toJson(initialResult),
                columnInfo = columnParseRes?.first,
                commonResult = columnParseRes?.second
            )
        } catch (e: StepException) {
            emptyStep?.copy(status = AnalysisStepStatus.ERROR.value, error = e.message)
        }

        if (step != null) {
            return analysisStepRepository?.save(step)
        } else {
            throw RuntimeException("Could not create/save initial_result.")
        }
    }

    fun updateColumnParams(analysisStep: AnalysisStep, params: String): AnalysisStep {
        analysisStepRepository?.save(analysisStep.copy(status = AnalysisStepStatus.RUNNING.value))

        val expDetailsType: Type = object : TypeToken<HashMap<String, ExpInfo>>() {}.type
        val experimentDetails: HashMap<String, ExpInfo> = gson.fromJson(params, expDetailsType)
        val newColumnMapping: ColumnMapping? =
            analysisStep.columnInfo?.columnMapping?.copy(experimentDetails = experimentDetails)

        val columnHash = Crc32HashComputations().computeStringHash(newColumnMapping.toString())
        val newColumnInfo: ColumnInfo? =
            analysisStep.columnInfo?.copy(columnMapping = newColumnMapping, columnMappingHash = columnHash)
        columnInfoRepository?.save(newColumnInfo!!)
        analysisStepRepository?.save(analysisStep.copy(status = AnalysisStepStatus.DONE.value))

        updateNextStep(analysisStep)

        return analysisStep
    }

    private fun createEmptyInitialResult(analysis: Analysis?): AnalysisStep? {
        val newStep = AnalysisStep(
            status = AnalysisStepStatus.RUNNING.value,
            type = AnalysisStepType.INITIAL_RESULT.value,
            analysis = analysis,
            lastModifDate = LocalDateTime.now(),
            modifiesResult = true
        )
        return analysisStepRepository?.save(newStep)
    }


    private fun createInitialResult(resultPath: String?, resultFilename: String?, type: ResultType?): InitialResult {
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

    private fun copyResultsTable(outputPath: String?, resultFile: String?, resultPath: String?): File {
        val timestamp = Timestamp(System.currentTimeMillis())

        if (resultType == ResultType.MaxQuant) {
            return copyResultTableWithName(outputPath, resultFile, resultPath, timestamp, "proteinGroups")
        } else {
            return copyResultTableWithName(outputPath, resultFile, resultPath, timestamp, "Report")
        }
    }

    private fun copyResultTableWithName(outputPath: String?, resultFile: String?, resultPath: String?, timestamp: Timestamp, fileName: String?): File {
        val originalTable = File("$resultPath/$resultFile")
        val newTableName = "${fileName}_${timestamp.time}.txt"
        val newTable = File("$outputPath/$newTableName")
        originalTable.copyTo(newTable)
        return newTable
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


}
