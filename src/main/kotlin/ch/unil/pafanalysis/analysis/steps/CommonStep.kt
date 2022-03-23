package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
open class CommonStep {

    @Autowired
    var analysisStepRepository: AnalysisStepRepository? = null

    open var type: AnalysisStepType? = null

    fun runCommonStep(analysis: Analysis?, oldStepId: Int): AnalysisStep?{
        val emptyStep = createEmptyAnalysisStep(analysis)
        return null
    }

    fun createEmptyAnalysisStep(analysis: Analysis?): AnalysisStep? {
        val newStep = AnalysisStep(
            status = AnalysisStepStatus.IDLE.value,
            type = type?.value,
            analysis = analysis,
            lastModifDate = LocalDateTime.now()
        )
        return analysisStepRepository?.save(newStep)
    }
}