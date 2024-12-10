package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.ColumnInfoService
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.initial_result.maxquant.AdaptMaxQuantTable
import ch.unil.pafanalysis.analysis.steps.initial_result.maxquant.InitialMaxQuantRunner
import ch.unil.pafanalysis.analysis.steps.initial_result.maxquant.MaxQuantGeneParsing
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.AdaptSpectronautTable
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.InitialSpectronautRunner
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.ParseSpectronautColNames
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.SpectronautSetup
import ch.unil.pafanalysis.analysis.steps.one_d_enrichment.AsyncOneDEnrichmentRunner
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultType
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

    var logger: Logger = LoggerFactory.getLogger(AsyncOneDEnrichmentRunner::class.java)

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
            val (columnInfo, commonResOrig) =
                columnInfoService?.createAndSaveColumnInfo(resultPath + "/" + result?.resFile, resultPath, resultType)!!
            val origTable = readTable.getTable(newTable.path, commonResOrig?.headers)

            val (maxQuantGeneParsingStatus, adaptedTable) = if(resultType == ResultType.Spectronaut){
                Pair(null, AdaptSpectronautTable.adaptTable(origTable))
            } else if(resultType == ResultType.MaxQuant){
                AdaptMaxQuantTable.adaptTable(origTable)
            } else Pair(null, origTable)

            val adaptedCommonRes = commonResOrig?.copy(headers = adaptedTable?.headers)

            if(adaptedTable != null) writeTable.write(newTable.path, adaptedTable)
            val newTableHash = Crc32HashComputations().computeFileHash(newTable)
            val initialResult = createInitialResult(resultPath, resultType, adaptedTable)

            // for spectronaut we can try to get the groups from the setup
            val newColInfo = if(initialResult?.spectronautSetup != null){
                val newColumnInfo = matchSpectronautGroups(columnInfo, initialResult.spectronautSetup)
                columnInfoRepository?.saveAndFlush(newColumnInfo!!)
            } else columnInfo

            val newInitialResult = if(initialResult?.maxQuantParameters != null && maxQuantGeneParsingStatus != null){
                val newMaxQuantParams = initialResult?.maxQuantParameters.copy(
                    someGenesParsedFromFasta = if(maxQuantGeneParsingStatus == MaxQuantGeneParsing.Some) true else null,
                    allGenesParsedFromFasta = if(maxQuantGeneParsingStatus == MaxQuantGeneParsing.All) true else null
                )
                initialResult.copy(maxQuantParameters = newMaxQuantParams)
            } else initialResult

            emptyStep?.copy(
                resultPath = stepPath,
                resultTablePath = "$stepPath/${newTable?.name}",
                resultTableHash = newTableHash,
                status = AnalysisStepStatus.DONE.value,
                results = gson.toJson(newInitialResult),
                columnInfo = emptyStep?.columnInfo ?: newColInfo,
                commonResult = emptyStep?.commonResult ?: adaptedCommonRes,
                tableNr = 1,
                nrProteinGroups = initialResult?.nrProteinGroups
            )
        } catch (e: Exception) {
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

    fun matchSpectronautGroups(columnInfo: ColumnInfo?, spectronautSetup: SpectronautSetup):ColumnInfo? {
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

    fun updateColumnParams(analysisStep: AnalysisStep, params: String): AnalysisStep {
        val oldExpDetails = analysisStep.columnInfo?.columnMapping?.experimentDetails
        val colMapping: ColumnMapping = gson.fromJson(params, ColumnMapping::class.java)

        val allExpNamesExist = colMapping.experimentNames?.all { expNames ->
            analysisStep.columnInfo?.columnMapping?.experimentNames?.contains(expNames) == true
        }

        if(allExpNamesExist != true) {
            logger.info("Warning: some experiment names are not in this analysis.")
            return analysisStep
        }

        val runningStep =
            analysisStepRepository?.saveAndFlush(analysisStep.copy(status = AnalysisStepStatus.RUNNING.value))

        // if there are groups defined and some experiments not in any groups, we set them as not selected.
        val newExpDetails: Map<String, ExpInfo>?  = if(colMapping.groupsOrdered?.isNotEmpty() == true){
            colMapping.experimentDetails?.mapValues { a -> if(a.value.group == null) a.value.copy(isSelected = false) else a.value }
        }else colMapping.experimentDetails

        val newHeaders: List<Header>? = updateHeaders(newExpDetails, analysisStep.commonResult?.headers, oldExpDetails)
        val (newTablePath, newTableHash) = updateResFile(analysisStep, newHeaders)

        val newColumnMapping: ColumnMapping? =
            analysisStep.columnInfo?.columnMapping?.copy(
                experimentDetails = newExpDetails,
                intCol = colMapping.intCol,
                groupsOrdered = colMapping.groupsOrdered,
                experimentNames = colMapping.experimentNames
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

    private fun updateHeaders(experimentDetails: Map<String, ExpInfo>?, headers: List<Header>?, oldExpDetails: Map<String, ExpInfo>?): List<Header>? {
        return headers?.map { h ->
            val oldExpInfo = oldExpDetails?.values?.find{ expD -> h.experiment?.name == expD.name}
            val expInfo = experimentDetails?.values?.find{ expD -> oldExpInfo?.originalName == expD.originalName}
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
