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
                "edit-groups" -> checkEditGroups(analysis)
                "remove-columns" -> checkRemoveColumns(analysis)
                "filter-rows" -> checkFilterRows(analysis)
                "transform-log2" -> checkLogTrans(analysis)
                "boxplot-and-stats" -> checkBoxplotAndStats(analysis)
                "normalize" -> checkNormalization(analysis)
                "repeat-boxplot" -> checkMultipleBoxplot(analysis)
                "filter-on-valid" -> checkFilterOnValid(analysis)
                else -> false
            }
        }
    }

    private fun checkFilterOnValid(analysis: Analysis?): Boolean {
        return analysis?.analysisSteps?.any { it.type == AnalysisStepType.GROUP_FILTER.value} ?: false
    }

    private fun checkMultipleBoxplot(analysis: Analysis?): Boolean {
        return analysis?.analysisSteps?.count { it.type == AnalysisStepType.BOXPLOT.value} ?: 0 > 1
    }

    private fun checkNormalization(analysis: Analysis?): Boolean {
        return analysis?.analysisSteps?.any { it.type == AnalysisStepType.NORMALIZATION.value} ?: false
    }

    private fun checkBoxplotAndStats(analysis: Analysis?): Boolean {
        val hasBoxplot = analysis?.analysisSteps?.any { it.type == AnalysisStepType.BOXPLOT.value} ?: false
        val hasStats = analysis?.analysisSteps?.any { it.type == AnalysisStepType.SUMMARY_STAT.value} ?: false
        return hasBoxplot && hasStats
    }

    private fun checkLogTrans(analysis: Analysis?): Boolean {
        return analysis?.analysisSteps?.any { it.type == AnalysisStepType.LOG_TRANSFORMATION.value} ?: false
    }

    private fun checkFilterRows(analysis: Analysis?): Boolean {
        return analysis?.analysisSteps?.any { it.type == AnalysisStepType.FILTER.value} ?: false
    }

    private fun checkRemoveColumns(analysis: Analysis?): Boolean {
        return analysis?.analysisSteps?.any { it.type == AnalysisStepType.REMOVE_COLUMNS.value} ?: false
    }

    private fun checkEditGroups(analysis: Analysis?): Boolean {
        return !analysis?.analysisSteps?.first()?.columnInfo?.columnMapping?.groupsOrdered.isNullOrEmpty()
    }

    private fun checkLoadDataset(analysis: Analysis?): Boolean {
        return analysis?.result?.description != null
    }



}