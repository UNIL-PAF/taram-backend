package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

@Service
class AnalysisStepService {

    @Autowired
    private var columnInfoRepository: ColumnInfoRepository? = null

    @Autowired
    private var analysisStepRepo: AnalysisStepRepository? = null

    @Autowired
    private var initialResultRunner: InitialResultRunner? = null

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var commonStep: CommonStep? = null

    @Autowired
    private var asyncAnaysisStepService: AsyncAnalysisStepService? = null

    fun updatePlotOptions(stepId: Int, echartsPlot: EchartsPlot): String? {
        return if(analysisStepRepo?.existsById(stepId) == true){
            val step = analysisStepRepo?.findById(stepId)!!
            return commonStep?.getRunner(step?.type)?.updatePlotOptions(step, echartsPlot)
        }else null
    }

    fun setAnalysisStepStatus(id: Int, status: AnalysisStepStatus): Int? {
        return analysisStepRepo?.setStatusById(status.value, id)
    }

    @Transactional
    fun setAllStepsStatus(analysisStep: AnalysisStep?, status: AnalysisStepStatus) {
        setAnalysisStepStatus(analysisStep?.id!!, status)
        if (analysisStep?.nextId != null) {
            val nextStep = analysisStepRepo?.findById(analysisStep?.nextId)
            setAllStepsStatus(nextStep, status)
        }
    }

    fun duplicateAnalysisSteps(
        sortedSteps: List<AnalysisStep>,
        newAnalysis: Analysis,
        copyAllSteps: Boolean
    ) {
        var stepBefore: AnalysisStep? = null

        val fltSteps = sortedSteps.filterIndexed { i: Int, _: AnalysisStep -> i == 0 || copyAllSteps }

        val emptyInitialStep = initialResultRunner?.prepareRun(analysisId = newAnalysis.id, newAnalysis.result)
        val columnInfo = columnInfoRepository?.saveAndFlush(fltSteps.first().columnInfo!!.copy(id = 0))

        val copiedSteps = fltSteps.mapIndexed { i: Int, step: AnalysisStep ->
            if (i == 0) {
                stepBefore = emptyInitialStep!!.copy(columnInfo = columnInfo, copyFromId = step.id, commonResult = step.commonResult)
                stepBefore
            } else if (copyAllSteps) {
                stepBefore = copyAnalysisStep(analysisStep = step, stepBefore = stepBefore, newAnalysis = newAnalysis, columnInfo = columnInfo)
                stepBefore
            } else {
                null
            }
        }

        val analysisSteps = setCorrectNextIds(copiedSteps)
        asyncAnaysisStepService?.runDuplicatedSteps(emptyInitialStep, analysisSteps, newAnalysis)
    }

    fun setCorrectNextIds(copiedSteps: List<AnalysisStep?>): List<AnalysisStep> {
        val nrSteps = copiedSteps.size - 1
        return copiedSteps.mapIndexed { i, step ->
            if (i < nrSteps) {
                analysisStepRepo?.saveAndFlush(step!!.copy(nextId = copiedSteps[i + 1]!!.id))!!
            } else {
                step!!
            }
        }
    }

    fun copyAnalysisStep(analysisStep: AnalysisStep, stepBefore: AnalysisStep?, newAnalysis: Analysis, columnInfo: ColumnInfo?): AnalysisStep? {
        return analysisStepRepo?.saveAndFlush(
            analysisStep.copy(
                id = 0,
                analysis = newAnalysis,
                beforeId = stepBefore?.id,
                copyFromId = analysisStep.id,
                status = AnalysisStepStatus.IDLE.value,
                stepHash = null,
                columnInfo = columnInfo
            )
        )
    }

    fun deleteStep(stepId: Int, relinkRemaining: Boolean? = true): Int? {
        val step: AnalysisStep = analysisStepRepo?.findById(stepId)!!
        var after: AnalysisStep? = null

        if (relinkRemaining == true) {
            val before: AnalysisStep? = if (step.beforeId != null) analysisStepRepo?.findById(step.beforeId) else null
            val after: AnalysisStep? = if (step.nextId != null) analysisStepRepo?.findById(step.nextId) else null

            if (before != null) {
                if (after != null) {
                    analysisStepRepo?.saveAndFlush(after.copy(beforeId = before?.id))
                    analysisStepRepo?.saveAndFlush(before.copy(nextId = after.id))
                } else {
                    analysisStepRepo?.saveAndFlush(before.copy(nextId = null))
                }
            }
        }

        deleteDirectory(Path.of(getOutputRoot(step?.analysis?.result?.type)?.plus(step.resultPath)))
        val res: Int? = analysisStepRepo?.deleteById(stepId)

        if (relinkRemaining == true && after !== null) {
            commonStep?.runStep(after)
            commonStep?.updateNextStep(after)
        }

        return res
    }

    fun getOutputRoot(resultType: String?): String? {
        return env?.getProperty("output.path")
    }

    fun deleteDirectory(directory: Path?): List<Boolean> {
        return if (directory != null && directory?.exists()) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map { it.toFile() }
                .map { it.delete() }.toList()
        } else {
            emptyList<Boolean>()
        }
    }

    fun updateComment(analysisStepId: Int, comment: String?): Boolean? {
        val analysisStep = analysisStepRepo?.findById(analysisStepId)
        val step = analysisStepRepo?.saveAndFlush(analysisStep!!.copy(comments = comment))
        return step != null
    }

}