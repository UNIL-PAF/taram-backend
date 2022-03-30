package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisStepService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BoxPlotRunner(): CommonStep() {

    @Autowired
    private var analysisStepService: AnalysisStepService? = null

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    fun run(oldStepId: Int): AnalysisStepStatus {
        val newStep = runCommonStep(AnalysisStepType.BOXPLOT, oldStepId, false)
        analysisStepService?.setAnalysisStepStatus(newStep!!.id!!, AnalysisStepStatus.DONE)

        return AnalysisStepStatus.DONE
    }
}