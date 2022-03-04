package ch.unil.pafanalysis.analysis.steps.quality_control

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus.*
import ch.unil.pafanalysis.analysis.model.AnalysisStepType.*
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.StepException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDateTime


@Service
@Transactional
class QualityControlRunner {
    @Autowired
    private var analysisStepRepo: AnalysisStepRepository? = null

    @Autowired
    private var analysisRepository: AnalysisRepository? = null

    @Autowired
    private var qualityControlR: QualityControlR? = null

    @Autowired
    private var env: Environment? = null

    fun createNewStep(oldStep: AnalysisStep?): AnalysisStep? {
        val lastModif = LocalDateTime.now()

        val analysis = analysisRepository?.findById(oldStep!!.analysis_id!!)

        val newStep = AnalysisStep(
            type = QUALITY_CONTROL.value,
            beforeId = oldStep?.beforeId,
            status = RUNNING.value,
            lastModifDate = lastModif,
            analysis = analysis
        )
        return analysisStepRepo?.save(newStep)
    }

    fun updatePathes(newStep: AnalysisStep, oldStep: AnalysisStep): AnalysisStep? {
        val outputRoot: String? = env?.getProperty("output.path.maxquant")
        val newOutPath = "${oldStep.analysis_id!!}/${newStep.id}"

        File(outputRoot!! + newOutPath).mkdir()

        val stepWithPathes =
            newStep.copy(resultPath = newOutPath, resultTablePath = oldStep.resultTablePath)
        return analysisStepRepo?.save(stepWithPathes)
    }

    fun run(stepId: Int): String {
        val oldStep: AnalysisStep? = analysisStepRepo?.findById(stepId)
        val newStep: AnalysisStep? = createNewStep(oldStep)

        if (newStep != null && oldStep != null) {
            analysisStepRepo?.setAfterIndexById(newStep.id!!, oldStep.id!!)
            val stepWithPathes = updatePathes(newStep, oldStep)
            if(stepWithPathes != null){qualityControlR?.runR(stepWithPathes, )}else{
                throw RuntimeException("Could not update current step: ${newStep.id}")
            }
        } else {
            throw RuntimeException("Could not create new step for QualityControl.")
        }

        return RUNNING.value
    }

}