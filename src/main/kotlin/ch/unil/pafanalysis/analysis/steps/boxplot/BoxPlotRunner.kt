package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.results.model.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Autowired
private var analysisRepository: AnalysisRepository? = null

@Service
class BoxPlotRunner(): CommonStep() {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    fun run(analysisId: Int?, result: Result?): String {

        val analysis = if (analysisId == null) null else analysisRepository?.findById(analysisId)
        val newStep: AnalysisStep? = createEmptyAnalysisStep(analysis)

        return "run"
    }
}