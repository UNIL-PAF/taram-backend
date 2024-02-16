package ch.unil.pafanalysis.analysis.hints

import ch.unil.pafanalysis.analysis.model.*

class CheckStepHints {

    fun check(analysisGroup: AnalysisGroup?, hints: StepHints?): StepHintInfo {
        val analysis = selAnalysis(analysisGroup)
        val defaultStepHints = DefaultStepHints().defaultList
        val hintsDone = hints?.hintsDone?.split(";")
        val hints = defaultStepHints.map{ h -> h.copy(isDone = checkHintStatus(h, analysis, hintsDone))}
        val nextHintId = getNextHintId(hints)
        return StepHintInfo(hintList = hints, nextHintId = nextHintId)
    }

    private fun getNextHintId(hints: List<StepHint>): String? {
        return hints.fold(Pair(hints.first().id, false)){acc, stepHint ->
            if(stepHint.isDone == true) {
                Pair(stepHint.id, true)
            } else if(acc.second){
                Pair(stepHint.id, false)
            } else acc
        }.first
    }

    private fun selAnalysis(analysisGroup: AnalysisGroup?): Analysis? {
        // we prefer locked ones
        return if(analysisGroup?.analysisList?.any{it.isLocked == true} == true){
            analysisGroup.analysisList?.filter{it.isLocked == true}?.first()
        }else analysisGroup?.analysisList?.first()
    }

    private fun checkHintStatus(hint: StepHint, analysis: Analysis?, hintsDone: List<String>?): Boolean {
        return if(hintsDone?.contains(hint.id) == true){
            true
        }else{
            when (hint.id) {
                "load-dataset" -> checkLoadDataset(analysis)
                else -> false
            }
        }
    }

    private fun checkLoadDataset(analysis: Analysis?): Boolean {
        return analysis?.result?.description != null
    }



}