package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.ColumnInfoService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.initial_result.maxquant.AdaptMaxQuantTable
import ch.unil.pafanalysis.analysis.steps.initial_result.maxquant.InitialMaxQuantRunner
import ch.unil.pafanalysis.analysis.steps.initial_result.maxquant.MaxQuantGeneParsing
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.AdaptSpectronautTable
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.InitialSpectronautRunner
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
import java.sql.Timestamp

@Service
class AsyncInitialResultRunner(): CommonStep(){

    private val readTable = ReadTableData()
    private val writeTable = WriteTableData()


    @Autowired
    private var initialSpectronautRunner: InitialSpectronautRunner? = null


    @Autowired
    private var columnInfoService: ColumnInfoService? = null

    @Async
    fun run(emptyStep: AnalysisStep?, result: Result?): AnalysisStep?{
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
                val newColumnInfo = initialSpectronautRunner?.matchSpectronautGroups(columnInfo, initialResult.spectronautSetup)
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

    @Async
    fun updateColumnParams(analysisStep: AnalysisStep, params: String): AnalysisStep {

        val logger: Logger = LoggerFactory.getLogger(AsyncInitialResultRunner::class.java)

        val oldExpDetails = analysisStep.columnInfo?.columnMapping?.experimentDetails
        val colMapping: ColumnMapping = gson.fromJson(params, ColumnMapping::class.java)

        val allExpNamesExist = colMapping.experimentNames?.all { expNames ->
            analysisStep.columnInfo?.columnMapping?.experimentNames?.contains(expNames) == true
        }

        if(allExpNamesExist != true) {
            logger.info("Warning: some experiment names are not in this analysis.")
            return analysisStep
        }

        // if there are groups defined and some experiments not in any groups, we set them as not selected.
        val newExpDetails: Map<String, ExpInfo>?  = if(colMapping.groupsOrdered?.isNotEmpty() == true){
            colMapping.experimentDetails?.mapValues { a -> if(a.value.group == null) a.value.copy(isSelected = false) else a.value }
        }else colMapping.experimentDetails

        val newHeaderOrder: List<Int>? = getNewHeaderOrder(newExpDetails, analysisStep.commonResult?.headers)

        val newHeaders: List<Header>? = updateHeaders(newExpDetails, analysisStep.commonResult?.headers, oldExpDetails, newHeaderOrder)
        val (newTablePath, newTableHash) = updateResFile(analysisStep, newHeaders, newHeaderOrder)

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
        return finalStep
    }

    private fun updateResFile(analysisStep: AnalysisStep?,
                              newHeaders: List<Header>?,
                              headerOrder: List<Int>?): Pair<String?, Long?> {
        val resFilePath = getOutputRoot() + analysisStep?.resultTablePath
        val oldTable = ReadTableData().getTable(resFilePath, analysisStep?.commonResult?.headers)

        val newCols = oldTable.cols?.mapIndexed { i, h -> headerOrder!![i] to h }?.sortedBy { it.first }?.map{it.second} ?: oldTable.cols

        WriteTableData().write(resFilePath, Table(headers = newHeaders, cols = newCols))

        val resultType = getResultType(analysisStep?.analysis?.result?.type)
        return getResultTablePath(
            modifiesResult = true,
            oldStep = analysisStep,
            oldTablePath = analysisStep?.resultTablePath,
            stepPath = analysisStep?.resultPath,
            resultType = resultType
        )
    }

    private fun getNewHeaderOrder(experimentDetails: Map<String, ExpInfo>?, headers: List<Header>?): List<Int>? {
        var expType: String? = null
        var fieldPos: Int? = null

        val newOrder: List<Int>? = headers?.fold(emptyList()){acc, h ->
            val newIdx = if(h.experiment != null){
                val fieldIdx = experimentDetails?.get(h.experiment.name)?.idx
                if(expType == null || expType != h.experiment.field) {
                    expType = h.experiment.field
                    fieldPos = h.idx
                }
                val a = (fieldIdx ?: return null) + (fieldPos ?: return null)
                a
            }else{
                if(expType != null){
                    expType = null
                    fieldPos = null
                }
                h.idx
            }
            acc.plus(newIdx)
        }
        return newOrder
    }

    private fun updateHeaders(experimentDetails: Map<String, ExpInfo>?,
                              headers: List<Header>?,
                              oldExpDetails: Map<String, ExpInfo>?,
                              headerOrder: List<Int>?): List<Header>? {
        val newHeaders = headers?.map { h ->
            val oldExpInfo = oldExpDetails?.values?.find{ expD -> h.experiment?.name == expD.name}
            val expInfo = experimentDetails?.values?.find{ expD -> oldExpInfo?.originalName == expD.originalName}
            val exp = h.experiment?.copy(name = expInfo?.name)
            val headerName = if (exp != null) "${expInfo?.name}.${h.experiment.field}" else h.name
            h.copy(experiment = exp, name = headerName)
        }

        val orderedHeaders = newHeaders?.mapIndexed { i, h -> headerOrder!![i] to h }?.sortedBy { it.first }?.map{it.second} ?: newHeaders
        return orderedHeaders?.mapIndexed{ i, h -> h.copy(idx = i) }
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