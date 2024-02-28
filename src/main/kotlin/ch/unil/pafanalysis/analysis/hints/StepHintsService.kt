package ch.unil.pafanalysis.analysis.hints

import ch.unil.pafanalysis.analysis.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StepHintsService {

    @Autowired
    private var hintsRepo: StepHintsRepository? = null

    fun get(resultId: Int, analysisGroup: AnalysisGroup?): StepHintInfo? {
        val stepHints = hintsRepo?.findByResultId(resultId)
        val stepHint = if(stepHints.isNullOrEmpty()) null else stepHints.first()
        //val analysisGroup = analysisService?.getSortedAnalysisList(resultId)
        val stepHintInfo = CheckStepHints().check(analysisGroup, stepHint)
        return stepHintInfo
    }
}