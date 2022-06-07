package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.service.ColumnInfoRepository
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotRunner
import ch.unil.pafanalysis.analysis.steps.transformation.TransformationRunner
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.results.model.ResultType
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
    private var transformationRunner: TransformationRunner? = null

    val hashComp: Crc32HashComputations = Crc32HashComputations()

    var type: AnalysisStepType? = null

    var resultType: ResultType? = null
    var outputRoot: String? = null
    var resultPath: String? = null

    fun runCommonStep(
        type: AnalysisStepType,
        oldStepId: Int? = null,
        modifiesResult: Boolean? = null,
        step: AnalysisStep? = null
    ): AnalysisStep? {
        val oldStep: AnalysisStep? = if (oldStepId != null) {
            analysisStepRepository?.findById(oldStepId)
        } else {
            null
        }

        val currentStep = step ?: createEmptyAnalysisStep(oldStep, type, modifiesResult)
        setPathes(currentStep?.analysis)
        val stepPath = setMainPaths(oldStep?.analysis, currentStep)
        val resultTablePathAndHash = getResultTablePath(modifiesResult, oldStep, stepPath, step?.resultTablePath)

        //val stepHash: Long = computeStepHash(step = currentStep, resultTableHash = resultTablePathAndHash.second)
        return updateEmptyStep(currentStep, stepPath, resultTablePathAndHash, oldStep?.commonResult)
    }

    fun addStep(stepId: Int, stepParams: AnalysisStepParams): AnalysisStep? {
        return getRunner(stepParams.type)?.run(stepId)
    }

    fun getRunner(type: String?): CommonRunner? {
        return when (type){
            AnalysisStepType.BOXPLOT.value -> boxPlotRunner
            AnalysisStepType.TRANSFORMATION.value -> transformationRunner
            else -> throw StepException("Analysis step [$type] not found.")
        }
    }

    fun runStep(step: AnalysisStep): AnalysisStep? {
        return getRunner(step.type)?.run(step?.beforeId!!, step)
    }

    fun setPathes(analysis: Analysis?) {
        resultType = getResultType(analysis?.result?.type)
        outputRoot = getOutputRoot(resultType)
        resultPath =
            env?.getProperty(if (resultType == ResultType.MaxQuant) "result.path.maxquant" else "result.path.spectronaut") + analysis?.result?.path
    }

    fun getResultType(type: String?): ResultType? {
        return if (type == ResultType.MaxQuant.value) ResultType.MaxQuant else ResultType.Spectronaut
    }

    fun getOutputRoot(resultType: ResultType?): String? {
        return env?.getProperty(if (resultType == ResultType.MaxQuant) "output.path.maxquant" else "output.path.spectronaut")
    }

    fun setMainPaths(analysis: Analysis?, emptyStep: AnalysisStep?): String {
        val outputPath: String = analysis?.id.toString()
        createResultDir(outputRoot?.plus(outputPath))

        val stepPath = "$outputPath/${emptyStep?.id}"
        createResultDir(outputRoot?.plus(stepPath))
        return stepPath
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
        val insertedStep = analysisStepRepository?.save(newStep)
        if(oldStep?.nextId != null) setNextStepBeforeId(oldStep?.nextId, insertedStep?.id)
        updateOldStep(oldStep, insertedStep?.id)
        return insertedStep
    }

    fun updateNextStep(step: AnalysisStep) {
        if (step.nextId != null) {
            val nextStep = analysisStepRepository?.findById(step.nextId!!)
            update(nextStep!!, step)
        }
    }

    fun update(step: AnalysisStep, stepBefore: AnalysisStep) {
        val newHash = computeStepHash(step, stepBefore)

        if (newHash != step.stepHash) {
            try {
                getRunner(step.type)?.run(stepBefore.id!!, step)
            } catch (e: Exception) {
                e.printStackTrace()
                step?.copy(
                    status = AnalysisStepStatus.ERROR.value,
                    error = e.message,
                    stepHash = Crc32HashComputations().getRandomHash()
                )
            }
        } else {
            analysisStepRepository?.save(step.copy(status = AnalysisStepStatus.DONE.value))
        }

        updateNextStep(step)
    }

    fun updateParams(analysisStep: AnalysisStep, params: String): AnalysisStep {
        val newStep = getRunner(analysisStep.type)?.run(analysisStep.beforeId!!, analysisStep.copy(parameters = params, parametersHash = hashComp.computeStringHash(params)))!!
        updateNextStep(newStep)
        return newStep
    }

    fun computeStepHash(step: AnalysisStep?, stepBefore: AnalysisStep? = null, resultTableHash: Long? = null): Long {
        val paramsHash = (step?.parametersHash ?: 0).toString()
        val columnsMappingHash = step?.columnInfo?.columnMappingHash.toString()
        val resultTableHash = (resultTableHash ?: stepBefore?.resultTableHash).toString()
        val commonHash = (step?.commonResult ?: 0).toString()
        val combinedHashString = "$paramsHash:$columnsMappingHash:$resultTableHash:$commonHash"
        return hashComp.computeStringHash(combinedHashString)
    }

    private fun updateEmptyStep(
        emptyStep: AnalysisStep?,
        stepPath: String?,
        resultTablePath: Pair<String?, Long?>,
        commonResult: CommonResult?
    ): AnalysisStep? {
        val newStep =
            emptyStep?.copy(
                resultPath = stepPath,
                resultTablePath = resultTablePath.first,
                resultTableHash = resultTablePath.second,
                status = AnalysisStepStatus.RUNNING.value,
                commonResult = commonResult
            )
        return analysisStepRepository?.save(newStep!!)
    }

    private fun updateOldStep(oldStep: AnalysisStep?, newNextId: Int?) {
        val updatedOldStep = oldStep?.copy(nextId = newNextId)
        analysisStepRepository?.save(updatedOldStep!!)
    }

    private fun setNextStepBeforeId(nextStepId: Int, currentStepId: Int?) {
        val nextStep = analysisStepRepository?.findById(nextStepId)
        val newNextStep = nextStep?.copy(beforeId = currentStepId)
        analysisStepRepository?.save(newNextStep!!)
    }

    fun getResultTablePath(
        modifiesResult: Boolean?,
        oldStep: AnalysisStep?,
        stepPath: String?,
        oldTablePath: String?
    ): Pair<String?, Long?> {
        val pathAndHash = if (modifiesResult != null && modifiesResult) {
            val oldTab = outputRoot?.plus("/") + oldStep?.resultTablePath
            val tabName = if (resultType == ResultType.MaxQuant) {
                "/proteinGroups_"
            } else {
                "/Report_"
            }
            val newTab = stepPath + tabName + Timestamp(System.currentTimeMillis()).time + ".txt"
            val newFile = File(outputRoot?.plus(newTab))
            File(oldTab).copyTo(newFile)

            // remove old if exists
            if(oldTablePath != null) File(outputRoot?.plus("/")?.plus(oldTablePath)).delete()

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