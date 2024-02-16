package ch.unil.pafanalysis.analysis.hints

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.AnalysisService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StepHintsService {

    @Autowired
    private var hintsRepo: StepHintsRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    fun get(resultId: Int): StepHintInfo? {
        val stepHints = hintsRepo?.findByResultId(resultId)
        val stepHint = if(stepHints.isNullOrEmpty()) null else stepHints.first()
        val analysisGroup = analysisService?.getSortedAnalysisList(resultId)
        val stepHintInfo = CheckStepHints().check(analysisGroup, stepHint)
        return stepHintInfo
    }
}