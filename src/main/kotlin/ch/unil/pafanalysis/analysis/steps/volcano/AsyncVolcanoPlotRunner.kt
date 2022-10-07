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
import java.lang.Math.abs
import kotlin.math.log10
import kotlin.math.log2

@Service
class AsyncVolcanoPlotRunner() : CommonStep() {

    private val readTableData = ReadTableData()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {

        val funToRun: () -> AnalysisStep? = {
            val volcano = createVolcanoObj(newStep)

            newStep?.copy(
                results = gson.toJson(volcano),
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun createVolcanoObj(analysisStep: AnalysisStep?): VolcanoPlot {
        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )

        val params = gson.fromJson(analysisStep?.parameters, VolcanoPlotParams().javaClass)

        val pValHeaderName = if(params?.useAdjustedPVal == true) "q.value" else "p.value"
        val pVals = readTableData.getDoubleColumn(table, pValHeaderName)
        val foldChanges = readTableData.getDoubleColumn(table, "fold.change")
        val proteinName = readTableData.getStringColumn(table, "Majority.protein.IDs")
        val geneName = readTableData.getStringColumn(table, "Gene.names")

        if(pVals == null || foldChanges == null) throw StepException("You have to run a statistical test before this plot.")

        val volcanoData = pVals.mapIndexed{ i, pVal ->
            val plotPVal = if(params?.log10PVal == true) log10(pVal) * -1 else pVal
            val isSign = pVal <= (params?.pValThresh ?: 0.0) && kotlin.math.abs(foldChanges?.get(i)) >= (params?.fcThresh ?: 10000.0)
            VolcanoPoint(
                prot = proteinName?.get(i)?.split(";")?.get(0),
                gene = geneName?.get(i)?.split(";")?.get(0),
                fc = foldChanges?.get(i),
                pVal = pVal,
                plotPVal = plotPVal,
                isSign = isSign
            )
        }
        return VolcanoPlot(data = volcanoData)
    }

}