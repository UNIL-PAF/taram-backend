package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepParams
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotParams
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.results.model.ResultType
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path

@Transactional
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

    fun updatePlotOptions(stepId: Int, echartsPlot: EchartsPlot): String? {
        val step = analysisStepRepo?.findById(stepId)!!
        println(echartsPlot)
        return commonStep?.getRunner(step?.type)?.updatePlotOptions(step, echartsPlot)
    }

    fun setAnalysisStepStatus(id: Int, status: AnalysisStepStatus): Int? {
        return analysisStepRepo?.setStatusById(status.value, id)
    }

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
    ): List<AnalysisStep>? {
        var stepBefore: AnalysisStep? = null

        val fltSteps = sortedSteps.filterIndexed{ i: Int, step: AnalysisStep -> i ==0 || copyAllSteps }

        val copiedSteps = fltSteps.mapIndexed { i: Int, step: AnalysisStep ->
            if(i == 0){
                val initialStep = initialResultRunner?.run(analysisId = newAnalysis.id, newAnalysis.result)
                val columnInfo = columnInfoRepository?.save(step.columnInfo!!.copy(id=0))
                stepBefore = initialStep!!.copy(columnInfo = columnInfo)
                stepBefore
            }else if(copyAllSteps) {
                stepBefore = copyAnalysisStep(analysisStep = step, stepBefore = stepBefore, newAnalysis)
                stepBefore
            }else { null }
        }

        val analysisSteps = setCorrectNextIds(copiedSteps)
        if(analysisSteps != null && analysisSteps.size > 1) {
            commonStep?.updateNextStep(analysisSteps[0]!!)
        }
        return analysisSteps
    }

    fun setCorrectNextIds(copiedSteps:List<AnalysisStep?>): List<AnalysisStep> {
        val nrSteps = copiedSteps.size - 1
        return copiedSteps.mapIndexed{ i, step ->
            if(i < nrSteps){
                analysisStepRepo?.save(step!!.copy(nextId = copiedSteps[i+1]!!.id))!!
            }else{
                step!!
            }
        }
    }

    fun copyAnalysisStep(analysisStep: AnalysisStep, stepBefore: AnalysisStep?, newAnalysis: Analysis): AnalysisStep? {
        return analysisStepRepo?.save(
            analysisStep.copy(
                id = 0,
                analysis = newAnalysis,
                beforeId = stepBefore?.id
                )
        )
    }

    fun deleteStep(stepId: Int): Int? {
        val step: AnalysisStep = analysisStepRepo?.findById(stepId)!!
        val before: AnalysisStep? = if(step.beforeId != null) analysisStepRepo?.findById(step.beforeId) else null
        val after: AnalysisStep? = if(step.nextId != null) analysisStepRepo?.findById(step.nextId) else null

        if(before != null){
            if(after != null){
                analysisStepRepo?.save(after.copy(beforeId = before?.id))
                analysisStepRepo?.save(before.copy(nextId = after.id))
            }else{
                analysisStepRepo?.save(before.copy(nextId = null))
            }
        }

        deleteDirectory(Path.of(getOutputRoot(step?.analysis?.result?.type)?.plus(step.resultPath)))
        val res: Int? = analysisStepRepo?.deleteById(stepId)

        if(after != null){
            commonStep?.runStep(after)
            commonStep?.updateNextStep(after)
        }

        return res
    }

    fun getOutputRoot(resultType: String?): String? {
        return env?.getProperty("output.path")
    }

    fun deleteDirectory(directory: Path?): List<Boolean> {
        return Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .map { it.toFile() }
            .map { it.delete() }.toList()
    }

}