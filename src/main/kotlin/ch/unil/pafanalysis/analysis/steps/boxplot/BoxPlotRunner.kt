package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.model.ColumnMapping
import ch.unil.pafanalysis.analysis.service.AnalysisStepService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.Crc32HashComputations
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.ceil

@Service
class BoxPlotRunner(): CommonStep() {

    @Autowired
    private var analysisStepService: AnalysisStepService? = null

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    private val gson = Gson()

    fun run(oldStepId: Int): AnalysisStepStatus {
        val newStep = runCommonStep(AnalysisStepType.BOXPLOT, oldStepId, false)
        val boxplot = createBoxplotObj(newStep?.columnInfo?.columnMapping, newStep?.resultTablePath)
        val updatedStep = newStep?.copy(status = AnalysisStepStatus.DONE.value, results = gson.toJson(boxplot))
        analysisStepRepository?.save(updatedStep!!)

        return AnalysisStepStatus.DONE
    }

    override fun computeAndUpdate(step: AnalysisStep, stepBefore: AnalysisStep, newHash: Long) {
        val boxplot = createBoxplotObj(step.columnInfo?.columnMapping, step.resultTablePath)

        val newStep = step.copy(resultTableHash = stepBefore?.resultTableHash, results = gson.toJson(boxplot))
        analysisStepRepository?.save(newStep)
        
        updateNextStep(step)
    }

    private fun createBoxplotObj(columnMapping: ColumnMapping?, resultTablePath: String?): BoxPlot {
        val boxplotGroupData = columnMapping?.experimentNames?.map{ name ->
            createBoxplotGroupData()
        }
        return BoxPlot(experimentNames = null, data = boxplotGroupData)
    }

    private fun createBoxplotGroupData(): BoxPlotGroupData {
        val listOfBoxplots = null
        return BoxPlotGroupData(group = "blibla", data = listOfBoxplots)
    }

    private fun computeBoxplotData(ints: List<Double>): List<Double>{
        val min = ints.minOrNull()!!
        val Q1 = percentile(ints, 25.0)
        val median = percentile(ints, 50.0)
        val Q3 = percentile(ints, 75.0)
        val max = ints.maxOrNull()!!
        return listOf(min, Q1, median, Q3, max)
    }

    private fun percentile(ints: List<Double>, percentile: Double): Double {
        val index = ceil(percentile / 100.0 * ints.size).toInt()
        return ints[index - 1]
    }

}