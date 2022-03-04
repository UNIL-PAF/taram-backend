package ch.unil.pafanalysis.analysis.steps.quality_control

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus.*
import ch.unil.pafanalysis.analysis.model.AnalysisStepType.*
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.StepException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

    fun run(stepId: Int): String {
        val oldStep: AnalysisStep? = analysisStepRepo?.findById(stepId)
        val newStep: AnalysisStep? = createNewStep(oldStep)
        analysisStepRepo?.setAfterIndexById(newStep!!.id!!, oldStep!!.id!!)

        if(newStep != null){
            qualityControlR?.runR(newStep)
        }else{
            throw RuntimeException("Could not create new step QualityControl.")
        }

        return RUNNING.value
    }

}