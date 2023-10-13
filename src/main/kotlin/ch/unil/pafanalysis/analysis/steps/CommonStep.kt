package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.service.ColumnInfoRepository
import ch.unil.pafanalysis.analysis.steps.add_column.AddColumnRunner
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotRunner
import ch.unil.pafanalysis.analysis.steps.filter.FilterRunner
import ch.unil.pafanalysis.analysis.steps.group_filter.GroupFilterRunner
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationRunner
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationRunner
import ch.unil.pafanalysis.analysis.steps.normalization.NormalizationRunner
import ch.unil.pafanalysis.analysis.steps.order_columns.OrderColumnsRunner
import ch.unil.pafanalysis.analysis.steps.pca.PcaRunner
import ch.unil.pafanalysis.analysis.steps.remove_columns.RemoveColumnsRunner
import ch.unil.pafanalysis.analysis.steps.remove_imputed.RemoveImputedRunner
import ch.unil.pafanalysis.analysis.steps.rename_columns.RenameColumnsRunner
import ch.unil.pafanalysis.analysis.steps.scatter_plot.ScatterPlotRunner
import ch.unil.pafanalysis.analysis.steps.summary_stat.SummaryStatRunner
import ch.unil.pafanalysis.analysis.steps.t_test.TTestRunner
import ch.unil.pafanalysis.analysis.steps.volcano.VolcanoPlotRunner
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.results.model.ResultType
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime

@Service
open class CommonStep {

    @Autowired
    var analysisStepRepository: AnalysisStepRepository? = null

    @Autowired
    var analysisRepository: AnalysisRepository? = null

    @Autowired
    var columnInfoRepository: ColumnInfoRepository? = null

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var boxPlotRunner: BoxPlotRunner? = null

    @Autowired
    private var pcaRunner: PcaRunner? = null

    @Autowired
    private var normalizationRunner: NormalizationRunner? = null

    @Autowired
    private var logTransRunner: LogTransformationRunner? = null

    @Autowired
    private var imputedRunner: ImputationRunner? = null

    @Autowired
    private var initialResultRunner: InitialResultRunner? = null

    @Autowired
    private var filterRunner: FilterRunner? = null

    @Autowired
    private var groupFilterRunner: GroupFilterRunner? = null

    @Autowired
    private var tTestRunner: TTestRunner? = null

    @Autowired
    private var volcanoPlotRunner: VolcanoPlotRunner? = null

    @Autowired
    private var removeImputedRunner: RemoveImputedRunner? = null

    @Autowired
    private var removeColumnsRunner: RemoveColumnsRunner? = null

    @Autowired
    private var scatterPlotRunner: ScatterPlotRunner? = null

    @Autowired
    private var summaryStatRunner: SummaryStatRunner? = null

    @Autowired
    private var orderColumnsRunner: OrderColumnsRunner? = null

    @Autowired
    private var renameColumnsRunner: RenameColumnsRunner? = null

    @Autowired
    private var addColumnRunner: AddColumnRunner? = null

    val gson = Gson()

    val hashComp: Crc32HashComputations = Crc32HashComputations()

    var type: AnalysisStepType? = null

    fun runCommonStep(
        type: AnalysisStepType,
        version: String,
        oldStepId: Int? = null,
        modifiesResult: Boolean? = null,
        step: AnalysisStep? = null,
        params: String? = null,
    ): AnalysisStep? {
        val oldStep: AnalysisStep? = if (oldStepId != null) {
            analysisStepRepository?.findById(oldStepId)
        } else {
            null
        }

        val currentStep = step ?: createEmptyAnalysisStep(oldStep, type, modifiesResult)

        try {
            val stepWithParams =
                if (params != null) currentStep?.copy(parameters = params) else currentStep

            val stepPath = setMainPaths(oldStep?.analysis, stepWithParams)
            val resultType = getResultType(stepWithParams?.analysis?.result?.type)
            val resultTablePathAndHash =
                getResultTablePath(modifiesResult, oldStep, stepPath, stepWithParams?.resultTablePath, resultType)

            val newStep =
                stepWithParams?.copy(
                    resultPath = stepPath,
                    resultTablePath = resultTablePathAndHash.first,
                    resultTableHash = resultTablePathAndHash.second,
                    status = AnalysisStepStatus.RUNNING.value,
                    commonResult = oldStep?.commonResult,
                    imputationTablePath = oldStep?.imputationTablePath,
                    tableNr = if (stepWithParams?.modifiesResult == true) {
                        oldStep?.tableNr?.plus(1)
                    } else oldStep?.tableNr
                )
            return analysisStepRepository?.saveAndFlush(newStep!!)
        } catch (e: Exception) {
            println("Error in runCommonStep ${currentStep?.id}")
            e.printStackTrace()
            return analysisStepRepository?.saveAndFlush(
                currentStep!!.copy(
                    status = AnalysisStepStatus.ERROR.value,
                    error = e.message,
                    stepHash = Crc32HashComputations().getRandomHash()
                )
            )
        }
    }

    private fun getNrProteinGroups(resultTablePath: String?): Int? {
        val fileName = getOutputRoot() + resultTablePath
        return File(fileName).readLines().size - 1
    }

    fun addStep(stepId: Int, stepParams: AnalysisStepParams): AnalysisStep? {
        return getRunner(stepParams.type)?.run(stepId, params = stepParams.params)
    }

    fun getRunner(type: String?): CommonRunner? {
        return when (type) {
            AnalysisStepType.INITIAL_RESULT.value -> initialResultRunner
            AnalysisStepType.BOXPLOT.value -> boxPlotRunner
            AnalysisStepType.PCA.value -> pcaRunner
            AnalysisStepType.NORMALIZATION.value -> normalizationRunner
            AnalysisStepType.SUMMARY_STAT.value -> summaryStatRunner
            AnalysisStepType.LOG_TRANSFORMATION.value -> logTransRunner
            AnalysisStepType.IMPUTATION.value -> imputedRunner
            AnalysisStepType.FILTER.value -> filterRunner
            AnalysisStepType.GROUP_FILTER.value -> groupFilterRunner
            AnalysisStepType.T_TEST.value -> tTestRunner
            AnalysisStepType.VOLCANO_PLOT.value -> volcanoPlotRunner
            AnalysisStepType.REMOVE_IMPUTED.value -> removeImputedRunner
            AnalysisStepType.REMOVE_COLUMNS.value -> removeColumnsRunner
            AnalysisStepType.SCATTER_PLOT.value -> scatterPlotRunner
            AnalysisStepType.ORDER_COLUMNS.value -> orderColumnsRunner
            AnalysisStepType.RENAME_COLUMNS.value -> renameColumnsRunner
            AnalysisStepType.ADD_COLUMN.value -> addColumnRunner
            else -> throw StepException("Analysis step [$type] not found.")
        }
    }

    fun getResultPath(analysis: Analysis?): String? {
        val resultType = getResultType(analysis?.result?.type)
        return env?.getProperty(if (resultType == ResultType.MaxQuant) "result.path.maxquant" else "result.path.spectronaut") + analysis?.result?.path
    }

    fun getResultType(type: String?): ResultType? {
        return if (type == ResultType.MaxQuant.value) ResultType.MaxQuant else ResultType.Spectronaut
    }

    fun getOutputRoot(): String? {
        return env?.getProperty("output.path")
    }

    fun setMainPaths(analysis: Analysis?, emptyStep: AnalysisStep?): String {
        val outputPath: String = analysis?.id.toString()
        val outputRoot = getOutputRoot()
        createResultDir(outputRoot?.plus(outputPath))

        val stepPath = "$outputPath/${emptyStep?.id}"
        createResultDir(outputRoot?.plus(stepPath))
        return stepPath
    }

    fun getCopyDifference(step: AnalysisStep?): String? {
        return if (step?.parametersHash != null && step?.copyFromId != null) {
            val origStep = analysisStepRepository?.findById(step.copyFromId)
            if (step.parametersHash != origStep?.parametersHash) {
                getRunner(step.type)?.getCopyDifference(step, origStep)
            } else null
        } else null
    }

    fun createEmptyAnalysisStep(
        oldStep: AnalysisStep?,
        type: AnalysisStepType,
        modifiesResult: Boolean? = null
    ): AnalysisStep? {
        val newStep = AnalysisStep(
            status = AnalysisStepStatus.RUNNING.value,
            type = type.value,
            analysis = oldStep?.analysis,
            commonResult = oldStep?.commonResult,
            lastModifDate = LocalDateTime.now(),
            beforeId = oldStep?.id,
            nextId = oldStep?.nextId,
            columnInfo = oldStep?.columnInfo,
            modifiesResult = modifiesResult
        )
        val insertedStep = analysisStepRepository?.saveAndFlush(newStep)
        if (oldStep?.nextId != null) setNextStepBeforeId(oldStep?.nextId, insertedStep?.id)
        updateOldStep(oldStep, insertedStep?.id)
        return insertedStep
    }

    fun updateNextStep(step: AnalysisStep) {
        if (step.nextId != null) {
            val nextStep = analysisStepRepository?.findById(step.nextId!!)
            update(nextStep!!, step)
        }
    }

    fun getSelProts(step: AnalysisStep?): List<String>? {
        return when (step?.type) {
            AnalysisStepType.BOXPLOT.value -> boxPlotRunner?.getParameters(step)?.selProts
            AnalysisStepType.VOLCANO_PLOT.value -> volcanoPlotRunner?.getParameters(step)?.selProteins
            else -> null
        }
    }

    private fun paramToHash(step: AnalysisStep?): Long? {
        val filterParams = when (step?.type) {
            AnalysisStepType.BOXPLOT.value -> boxPlotRunner?.getParameters(step).toString()
            AnalysisStepType.PCA.value -> pcaRunner?.getParameters(step).toString()
            AnalysisStepType.FILTER.value -> filterRunner?.getParameters(step).toString()
            AnalysisStepType.GROUP_FILTER.value -> groupFilterRunner?.getParameters(step).toString()
            AnalysisStepType.T_TEST.value -> tTestRunner?.getParameters(step).toString()
            AnalysisStepType.NORMALIZATION.value -> normalizationRunner?.getParameters(step).toString()
            AnalysisStepType.LOG_TRANSFORMATION.value -> logTransRunner?.getParameters(step).toString()
            AnalysisStepType.IMPUTATION.value -> imputedRunner?.getParameters(step).toString()
            AnalysisStepType.VOLCANO_PLOT.value -> volcanoPlotRunner?.getParameters(step).toString()
            AnalysisStepType.REMOVE_IMPUTED.value -> removeImputedRunner?.getParameters(step).toString()
            AnalysisStepType.REMOVE_COLUMNS.value -> removeColumnsRunner?.getParameters(step).toString()
            AnalysisStepType.SCATTER_PLOT.value -> scatterPlotRunner?.getParameters(step).toString()
            AnalysisStepType.SUMMARY_STAT.value -> summaryStatRunner?.getParameters(step).toString()
            AnalysisStepType.ORDER_COLUMNS.value -> orderColumnsRunner?.getParameters(step).toString()
            AnalysisStepType.RENAME_COLUMNS.value -> renameColumnsRunner?.getParameters(step).toString()
            AnalysisStepType.ADD_COLUMN.value -> addColumnRunner?.getParameters(step).toString()
            else -> throw RuntimeException("Cannot parse parameters for type [${step?.type}]")
        }
        return hashComp.computeStringHash(filterParams)
    }

    fun tryToRun(runFun: () -> AnalysisStep?, step: AnalysisStep?) {
        try {
            val step = runFun()

            val stepBefore = analysisStepRepository?.findById(step!!.beforeId!!)

            val stepWithFileHash = step?.copy(
                resultTableHash = hashComp.computeFileHash(File(getOutputRoot() + step.resultTablePath)),
                parametersHash = paramToHash(step)
            )

            val newHash = computeStepHash(stepWithFileHash, stepBefore)

            val updatedStep =
                stepWithFileHash?.copy(
                    status = AnalysisStepStatus.DONE.value,
                    stepHash = newHash,
                    copyDifference = getCopyDifference(stepWithFileHash),
                    nrProteinGroups = getNrProteinGroups(stepWithFileHash.resultTablePath)
                )
            analysisStepRepository?.saveAndFlush(updatedStep!!)!!
            updateNextStep(updatedStep!!)
        } catch (e: Exception) {
            println("Error in asyncRun ${step?.id}")
            e.printStackTrace()
            analysisStepRepository?.saveAndFlush(
                step!!.copy(
                    status = AnalysisStepStatus.ERROR.value,
                    error = e.message?.take(255),
                    stepHash = Crc32HashComputations().getRandomHash()
                )
            )
        }
    }

    fun update(step: AnalysisStep, stepBefore: AnalysisStep) {
        val newHash = computeStepHash(step, stepBefore, stepBefore.resultTableHash)

        if (newHash != step.stepHash) {
            val runningStep = analysisStepRepository?.saveAndFlush(step.copy(status = AnalysisStepStatus.RUNNING.value))
            try {
                getRunner(runningStep!!.type)?.run(stepBefore.id!!, runningStep)
            } catch (e: Exception) {
                println("Error in update ${runningStep?.id}")
                e.printStackTrace()
                analysisStepRepository?.saveAndFlush(
                    runningStep!!.copy(
                        status = AnalysisStepStatus.ERROR.value,
                        error = e.message,
                        stepHash = Crc32HashComputations().getRandomHash()
                    )
                )
            }
        } else {
            analysisStepRepository?.saveAndFlush(step.copy(status = AnalysisStepStatus.DONE.value))
            updateNextStep(step!!)
        }
    }

    fun computeStepHash(
        step: AnalysisStep?,
        stepBefore: AnalysisStep? = null,
        resultTableHash: Long? = null
    ): Long? {
        val paramsHash = (step?.parametersHash ?: 0).toString()
        val columnsMappingHash = step?.columnInfo?.columnMappingHash.toString()
        val resultTableHash = (resultTableHash ?: stepBefore?.resultTableHash).toString()
        val commonHash = (step?.commonResult ?: 0).toString()
        val combinedHashString = "$paramsHash:$columnsMappingHash:$resultTableHash:$commonHash"
        return hashComp.computeStringHash(combinedHashString)
    }

    private fun updateOldStep(oldStep: AnalysisStep?, newNextId: Int?) {
        val updatedOldStep = oldStep?.copy(nextId = newNextId)
        analysisStepRepository?.saveAndFlush(updatedOldStep!!)
    }

    private fun setNextStepBeforeId(nextStepId: Int, currentStepId: Int?) {
        val nextStep = analysisStepRepository?.findById(nextStepId)
        val newNextStep = nextStep?.copy(beforeId = currentStepId)
        analysisStepRepository?.saveAndFlush(newNextStep!!)
    }

    fun getResultTablePath(
        modifiesResult: Boolean?,
        oldStep: AnalysisStep?,
        stepPath: String?,
        oldTablePath: String?,
        resultType: ResultType?
    ): Pair<String?, Long?> {
        val pathAndHash = if (modifiesResult != null && modifiesResult) {
            val oldTab = getOutputRoot()?.plus("/") + oldStep?.resultTablePath
            val tabName = if (resultType == ResultType.MaxQuant) {
                "/proteinGroups_"
            } else {
                "/Report_"
            }
            val newTab = stepPath + tabName + Timestamp(System.currentTimeMillis()).time + ".txt"
            val newFile = File(getOutputRoot()?.plus(newTab))
            File(oldTab).copyTo(newFile)

            // remove old if exists
            if (oldTablePath != null) File(getOutputRoot()?.plus("/")?.plus(oldTablePath)).delete()

            Pair(newTab, Crc32HashComputations().computeFileHash(newFile))
        } else {
            Pair(oldStep?.resultTablePath, oldStep?.resultTableHash)
        }

        return pathAndHash
    }

    private fun createResultDir(outputPath: String?): String {
        if (outputPath == null) throw RuntimeException("There is no output path defined.")
        val dir = File(outputPath)
        if (!dir.exists()) dir.mkdir()
        return outputPath
    }
}