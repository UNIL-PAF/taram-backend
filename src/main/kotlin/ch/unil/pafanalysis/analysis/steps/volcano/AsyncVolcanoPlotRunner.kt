package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotParams
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.google.common.math.Quantiles
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.log2

@Service
class AsyncVolcanoPlotRunner() : CommonStep() {

    private val readTableData = ReadTableData()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?, paramsString: String?) {

        val params = gson.fromJson(paramsString, VolcanoPlotParams().javaClass)

        val funToRun: () -> AnalysisStep? = {
            val volcano = createVolcanoObj(newStep, params)

            newStep?.copy(
                results = gson.toJson(volcano),
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun createVolcanoObj(analysisStep: AnalysisStep?, params: VolcanoPlotParams?): VolcanoPlot {
        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.columnInfo?.columnMapping
        )

        val pValHeaderName = if(params?.useAdjustedPVal == true) "q.value" else "p.value"
        val pVals = readTableData.getDoubleColumn(table, pValHeaderName)
        val foldChanges = readTableData.getDoubleColumn(table, "fold.change")
        val proteinName = readTableData.getStringColumn(table, "Majority.protein.IDs")

        if(pVals == null || foldChanges == null) throw StepException("You have to run a statistical test before this plot.")

        val volcanoData = pVals.mapIndexed{ i, pVal -> VolcanoPoint(proteinName?.get(i), foldChanges?.get(i), pVal, null)}
        return VolcanoPlot(data = volcanoData)
    }

}