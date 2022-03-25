package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.steps.CommonStep
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BoxPlotRunner(): CommonStep() {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    fun run(oldStepId: Int): String {
        val newStep = runCommonStep(AnalysisStepType.BOXPLOT, oldStepId, false)

        return "run"
    }
}