package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.ColumnInfoService
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.InitialSpectronautRunner
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultType
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime


@Service
class InitialResultRunner() : CommonStep(), CommonRunner {

    @Autowired
    private var columnInfoService: ColumnInfoService? = null

    @Autowired
    private var initialResultPdf: InitialResultPdf? = null

    @Autowired
    private var initialSpectronautRunner: InitialSpectronautRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.INITIAL_RESULT

    private val readTable = ReadTableData()
    private val writeTable = WriteTableData()

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, pageWidth: Float, stepNr: Int): Div? {
        return initialResultPdf?.createPdf(step, pdf, pageWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        throw Exception("InitialResultRunner does not implement ordinary run function.")
    }

    fun prepareRun(analysisId: Int?, result: Result?): AnalysisStep? {
        val analysis =
            analysisRepository?.findById(analysisId ?: throw StepException("No valid analysisId was provided."))
        return createEmptyInitialResult(analysis)
    }

    fun run(emptyStep: AnalysisStep?, result: Result?): AnalysisStep? {
        val stepPath = setMainPaths(emptyStep?.analysis, emptyStep)

        val resultType = getResultType(emptyStep?.analysis?.result?.type)
        val outputRoot = getOutputRoot()
        val resultPath = getResultPath(emptyStep?.analysis)
        val newTable = copyResultsTable(outputRoot?.plus(stepPath), result?.resFile, resultPath, resultType)

        val step: AnalysisStep? = try {
            val (columnInfo, commonRes) =
                columnInfoService?.createAndSaveColumnInfo(resultPath + "/" + result?.resFile, resultPath, resultType)!!
            val table = readTable.getTable(newTable.path, commonRes?.headers)
            writeTable.write(newTable.path, table)
            val newTableHash = Crc32HashComputations().computeFileHash(newTable)
            val initialResult = createInitialResult(resultPath, resultType, table)

            // for spectronaut we can try to get the groups from the setup
            val newColInfo = if(initialResult?.spectronautSetup != null){
                val newExpDetails = columnInfo?.columnMapping?.experimentNames?.fold(emptyMap<String, ExpInfo>()){ acc, expName ->
                    val matched = initialResult.spectronautSetup.runs?.filter{ it.name?.contains(expName) == true }
                    val condition = if(matched?.size == 1) matched?.get(0).condition else null
                    val newEntry = columnInfo?.columnMapping?.experimentDetails?.get(expName)?.copy(group = condition)
                    acc.plus(Pair(expName, newEntry!!))
                }
                val newColMapping = columnInfo?.columnMapping?.copy(experimentDetails = newExpDetails)
                val newColumnInfo = columnInfo?.copy(columnMapping = newColMapping)
                columnInfoRepository?.saveAndFlush(newColumnInfo!!)
            } else columnInfo

            emptyStep?.copy(
                resultPath = stepPath,
                resultTablePath = "$stepPath/${newTable?.name}",
                resultTableHash = newTableHash,
                status = AnalysisStepStatus.DONE.value,
                results = gson.toJson(initialResult),
                columnInfo = emptyStep?.columnInfo ?: newColInfo,
                commonResult = emptyStep?.commonResult ?: commonRes,
                tableNr = 1,
                nrProteinGroups = initialResult?.nrProteinGroups
            )
        } catch (e: StepException) {
            e.printStackTrace()
            emptyStep?.copy(status = AnalysisStepStatus.ERROR.value, error = e.message)
        }

        if (step != null) {
            return analysisStepRepository?.saveAndFlush(step)
        } else {
            throw RuntimeException("Could not create/save initial_result.")
        }
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        throw Exception("InitialResultRunner does not implement getCopyDifference function.")
    }

    fun updateColumnParams(analysisStep: AnalysisStep, params: String): AnalysisStep {
        val runningStep =
            analysisStepRepository?.saveAndFlush(analysisStep.copy(status = AnalysisStepStatus.RUNNING.value))

        //val expDetailsType: Type = object : TypeToken<HashMap<String, ExpInfo>>() {}.type
        val colMapping: ColumnMapping = gson.fromJson(params, ColumnMapping::class.java)
        val newHeaders: List<Header>? = updateHeaders(colMapping.experimentDetails, analysisStep.commonResult?.headers)
        val (newTablePath, newTableHash) = updateResFile(analysisStep, newHeaders)

        val newColumnMapping: ColumnMapping? =
            analysisStep.columnInfo?.columnMapping?.copy(
                experimentDetails = colMapping.experimentDetails,
                intCol = colMapping.intCol
            )

        val columnHash = Crc32HashComputations().computeStringHash(newColumnMapping.toString())
        val newColumnInfo: ColumnInfo? =
            analysisStep.columnInfo?.copy(columnMapping = newColumnMapping, columnMappingHash = columnHash)
        columnInfoRepository?.saveAndFlush(newColumnInfo!!)
        val newCommonRes = analysisStep.commonResult?.copy(headers = newHeaders)

        val finalStep = analysisStep.copy(
            status = AnalysisStepStatus.DONE.value,
            commonResult = newCommonRes,
            resultTableHash = newTableHash,
            resultTablePath = newTablePath
        )

        analysisStepRepository?.saveAndFlush(finalStep)

        updateNextStep(finalStep)
        return runningStep!!
    }

    private fun updateResFile(analysisStep: AnalysisStep?, newHeaders: List<Header>?): Pair<String?, Long?> {
        val resFilePath = getOutputRoot() + analysisStep?.resultTablePath
        val oldTable = ReadTableData().getTable(resFilePath, analysisStep?.commonResult?.headers)
        WriteTableData().write(resFilePath, oldTable.copy(headers = newHeaders))
        val resultType = getResultType(analysisStep?.analysis?.result?.type)
        return getResultTablePath(
            modifiesResult = true,
            oldStep = analysisStep,
            oldTablePath = analysisStep?.resultTablePath,
            stepPath = analysisStep?.resultPath,
            resultType = resultType
        )
    }

    private fun updateHeaders(experimentDetails: Map<String, ExpInfo>?, headers: List<Header>?): List<Header>? {
        return headers?.map { h ->
            val expInfo = experimentDetails?.get(h.experiment?.name)
            val exp = h.experiment?.copy(name = expInfo?.name)
            val headerName = if (exp != null) "${expInfo?.name}.${h.experiment?.field}" else h.name
            h.copy(experiment = exp, name = headerName)
        }
    }

    private fun createEmptyInitialResult(analysis: Analysis?): AnalysisStep? {
        val newStep = AnalysisStep(
            status = AnalysisStepStatus.RUNNING.value,
            type = AnalysisStepType.INITIAL_RESULT.value,
            analysis = analysis,
            lastModifDate = LocalDateTime.now(),
            modifiesResult = true
        )
        return analysisStepRepository?.saveAndFlush(newStep)
    }

    private fun createInitialResult(
        resultPath: String?,
        type: ResultType?,
        table: Table?
    ): InitialResult? {
        val initialRes = if (type == ResultType.MaxQuant) {
            InitialMaxQuantRunner().createInitialMaxQuantResult(resultPath, "parameters.txt")
        } else {
            initialSpectronautRunner?.createInitialSpectronautResult(resultPath, table)
        }
        return initialRes?.copy(nrProteinGroups = table?.cols?.get(0)?.size)
    }

    private fun copyResultsTable(
        outputPath: String?,
        resultFile: String?,
        resultPath: String?,
        resultType: ResultType?
    ): File {
        val timestamp = Timestamp(System.currentTimeMillis())

        return if (resultType == ResultType.MaxQuant) {
            copyResultTableWithName(outputPath, resultFile, resultPath, timestamp, "proteinGroups")
        } else {
            copyResultTableWithName(outputPath, resultFile, resultPath, timestamp, "Report")
        }
    }

    private fun copyResultTableWithName(
        outputPath: String?,
        resultFile: String?,
        resultPath: String?,
        timestamp: Timestamp,
        fileName: String?
    ): File {
        val originalTable = File("$resultPath/$resultFile")
        val newTableName = "${fileName}_${timestamp.time}.txt"
        val newTable = File("$outputPath/$newTableName")
        originalTable.copyTo(newTable)
        return newTable
    }

}
