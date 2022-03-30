package ch.unil.pafanalysis.analysis.steps.quality_control

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepParams
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus.RUNNING
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonStep
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional
class QualityControlRunner : CommonStep(){
    @Autowired
    private var qualityControlR: QualityControlR? = null

    fun run(oldStepId: Int): AnalysisStepStatus {
        val newStep = runCommonStep(AnalysisStepType.QUALITY_CONTROL, oldStepId, modifiesResult = false)

        if (newStep != null) {
          qualityControlR?.runR(newStep)
        } else {
            throw RuntimeException("Could not create new step for QualityControl.")
        }

        return RUNNING
    }

}