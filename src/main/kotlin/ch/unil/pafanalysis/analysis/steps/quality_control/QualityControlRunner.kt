package ch.unil.pafanalysis.analysis.steps.quality_control

import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus.*
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class QualityControlRunner {
    @Autowired
    private var analysisStepRepo: AnalysisStepRepository? = null

    fun run(stepId: Int): String {

        return RUNNING.value
    }

}