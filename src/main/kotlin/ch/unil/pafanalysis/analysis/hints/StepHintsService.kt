package ch.unil.pafanalysis.analysis.hints

import ch.unil.pafanalysis.analysis.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StepHintsService {

    @Autowired
    private var hintsRepo: StepHintsRepository? = null

    fun get(resultId: Int, analysisGroup: AnalysisGroup?): StepHintInfo? {
        val stepHint = hintsRepo?.findOneByResultId(resultId)
        return CheckStepHints().check(analysisGroup, stepHint)
    }

    fun switchHintDone(resultId: Int, hintId: String): Boolean {
        val stepHint = hintsRepo?.findOneByResultId(resultId)
        val (newHint, isDone) = if(stepHint != null){
            val hintsDone = stepHint?.hintsDone?.split(";") ?: emptyList()
            val (newHintsDone, isDone) = if(hintsDone.contains(hintId)){
                Pair(hintsDone?.filter{it != hintId}, false)
            } else Pair(hintsDone?.plus(hintId), true)
            Pair(stepHint.copy(hintsDone = newHintsDone?.distinct()?.filter{it.isNotEmpty()}?.joinToString(";")), isDone)
        } else {
            Pair(StepHints(resultId = resultId, hintsDone = hintId), true)
        }
        hintsRepo?.saveAndFlush(newHint)
        return isDone
    }
}