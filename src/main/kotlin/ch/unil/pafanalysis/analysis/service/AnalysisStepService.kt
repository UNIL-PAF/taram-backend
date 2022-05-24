package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class AnalysisStepService {

    @Autowired
    private var columnInfoRepository: ColumnInfoRepository? = null

    @Autowired
    private var analysisStepRepo: AnalysisStepRepository? = null

    @Autowired
    private var initialResultRunner: InitialResultRunner? = null

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

        return setCorrectNextIds(copiedSteps)
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

}