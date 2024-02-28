package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.hints.StepHintsService
import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultStatus
import ch.unil.pafanalysis.results.service.ResultRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AnalysisService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var resultRepo: ResultRepository? = null

    @Autowired
    private var initialResult: InitialResultRunner? = null

    @Autowired
    private var analysisStepService: AnalysisStepService? = null


    private fun createNewAnalysis(result: Result?): List<Analysis>? {
        val newAnalysis = Analysis(
            idx = 0,
            result = result,
            lastModifDate = LocalDateTime.now(),
            status = AnalysisStepStatus.IDLE.value
        )
        val analysis: Analysis? = analysisRepo?.saveAndFlush(newAnalysis)

        val emptyRun = initialResult?.prepareRun(analysis?.id, result)
        initialResult?.run(emptyRun, result)

        val analysisList = listOf(analysis)
        return if (analysisList.any { it == null }) null else analysisList as List<Analysis>
    }

    fun switchLock(analysisId: Int): Boolean {
        val analysis = analysisRepo?.findById(analysisId)
        val isLocked = analysis?.isLocked != true
        analysisRepo?.saveAndFlush(analysis?.copy(isLocked = isLocked)!!)

        // set result to done if there is any locked analysis
        val resultId = analysis?.result?.id!!
        val isAnyLocked = analysisRepo?.findByResultId(resultId)?.map { it.isLocked }?.find { it == true }
        val resStatus = if (isLocked || isAnyLocked == true) ResultStatus.DONE.value else ResultStatus.RUNNING.value
        val result = resultRepo?.findById(resultId)
        resultRepo?.saveAndFlush(result?.copy(status = resStatus)!!)

        return isLocked
    }

    fun setName(analysisId: Int, analysisName: String): String {
        val analysis = analysisRepo?.findById(analysisId)
        analysisRepo?.saveAndFlush(analysis?.copy(name = analysisName)!!)
        return analysisName
    }

    fun delete(analysisId: Int): Int? {
        val analysis = analysisRepo?.findById(analysisId)
        val steps: List<AnalysisStep>? = sortAnalysisSteps(analysis?.analysisSteps)?.asReversed()
        steps?.forEach { analysisStepService?.deleteStep(it.id!!, relinkRemaining = false) }
        return analysisRepo?.deleteById(analysisId)
    }

    fun getByResultId(resultId: Int): List<Analysis>? {
        // check first if this result really exists
        val result = resultRepo?.findById(resultId)
            ?: throw RuntimeException("There is no result for resultId [$resultId]")

        val analysisInDb: List<Analysis>? = analysisRepo?.findByResultId(resultId)?.toList()

        val analysis = if (analysisInDb == null || analysisInDb!!.isEmpty()) {
            createNewAnalysis(result)
            analysisRepo?.findByResultId(resultId)?.toList()
        } else {
            analysisInDb
        }

        return analysis
    }

    fun sortAnalysisSteps(oldList: List<AnalysisStep>?): List<AnalysisStep>? {
        var emergencyBreak = 99
        val first: AnalysisStep? = oldList?.find { it.type == AnalysisStepType.INITIAL_RESULT.value }
        var sortedList = if (first != null) {
            mutableListOf<AnalysisStep>(first!!)
        } else {
            return oldList
        }

        var nextEl: AnalysisStep? = first

        while (nextEl?.nextId != null && emergencyBreak > 0) {
            nextEl = oldList?.find { it.id == nextEl?.nextId }
            if(nextEl != null){
                sortedList.add(nextEl)
            }
            emergencyBreak--
        }

        if (emergencyBreak == 0) {
            throw RuntimeException("Could not sort the analysis steps (or you have over 99 steps).")
        }

        return sortedList
    }

    fun getSortedAnalysisList(resultId: Int): AnalysisGroup? {
        // sort the analysis steps
        val analysisList: List<Analysis>? = getByResultId(resultId)

        val sortedList = analysisList?.map { a ->
            val analysisStepsWithoutPlots = removePlotResults(a.analysisSteps)
            val sortedAnalysisSteps = sortAnalysisSteps(analysisStepsWithoutPlots)
            a.copy(analysisSteps = sortedAnalysisSteps)
        }
        return getAnalysisWithStatus(sortedList)
    }

    private fun removePlotResults(steps: List<AnalysisStep>?): List<AnalysisStep>? {
        return steps?.map { step ->
            val type = AnalysisStepType.values().firstOrNull { it.value == step.type }
            if (type?.hasPlot == true) {
                step.copy(results = null)
            } else step
        }
    }

    fun duplicateAnalysis(analysisId: Int, copyAllSteps: Boolean): Analysis {
        val analysis = analysisRepo?.findById(analysisId)
        val allAnalysisIdx: List<Int?>? = analysisRepo?.findByResultId(analysis!!.result!!.id!!)?.map { it.idx }
        val maxIdx = allAnalysisIdx?.maxOfOrNull { it ?: 0 } ?: 0

        val newAnalysis =
            analysisRepo?.saveAndFlush(
                analysis!!.copy(
                    id = 0,
                    idx = maxIdx.plus(1),
                    copyFromIdx = if (copyAllSteps) analysis?.idx else null,
                    name = null,
                    isLocked = null
                )
            )

        val sortedSteps = sortAnalysisSteps(analysis?.analysisSteps)!!

        analysisStepService?.duplicateAnalysisSteps(sortedSteps, newAnalysis!!, copyAllSteps)
        return newAnalysis!!
    }

    private fun getAnalysisWithStatus(analysis: List<Analysis>?): AnalysisGroup? {
        val emptyString: String? = null
        val analysisWithStatus = analysis?.map {
            it.copy(status = it.analysisSteps?.fold(emptyString) { accS, s ->
                chooseAnalysisStatus(accS, s.status, analysisStatusOrder)
            })
        }
        val globalStatus = analysisWithStatus?.fold(emptyString) { acc, a ->
            chooseAnalysisStatus(acc, a.status, globalAnalysisStatusOrder)
        } ?: AnalysisStepStatus.IDLE.value

        return AnalysisGroup(analysisWithStatus, globalStatus)
    }

    private val analysisStatusOrder = listOf(
        AnalysisStepStatus.RUNNING.value,
        AnalysisStepStatus.ERROR.value,
        AnalysisStepStatus.IDLE.value,
        AnalysisStepStatus.DONE.value
    )

    private val globalAnalysisStatusOrder = listOf(
        AnalysisStepStatus.RUNNING.value,
        AnalysisStepStatus.IDLE.value,
        AnalysisStepStatus.ERROR.value,
        AnalysisStepStatus.DONE.value
    )

    private fun chooseAnalysisStatus(currStat: String?, newStat: String?, statOrder: List<String>): String? {
        val currIdx = statOrder.indexOf(currStat)
        val newIdx = statOrder.indexOf(newStat)

        val idx = if (currStat == null || newIdx < currIdx) newIdx else currIdx
        return if (idx < 0) null else statOrder[idx]
    }

}