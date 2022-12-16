package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ReadTableData
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.log10

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
        val compName = "." + params.comparison?.group1?.plus("-" + params.comparison?.group2)

        val pValHeaderName = (if(params?.useAdjustedPVal == true) "q.value" else "p.value") + compName
        val pVals = readTableData.getDoubleColumn(table, pValHeaderName)
        val foldChanges = readTableData.getDoubleColumn(table, "fold.change$compName")
        val proteinName = readTableData.getStringColumn(table, "Majority.protein.IDs")
        val geneName = readTableData.getStringColumn(table, "Gene.names")

        if(pVals == null || foldChanges == null) throw StepException("You have to run a statistical test before this plot.")

        val volcanoData = pVals.mapIndexed{ i, pVal ->
            val plotPVal = if(params?.log10PVal == true) log10(pVal) * -1 else pVal
            val isSign = pVal <= (params?.pValThresh ?: 0.0) && kotlin.math.abs(foldChanges?.get(i)) >= (params?.fcThresh ?: 10000.0)

            VolcanoPoint(
                prot = proteinName?.get(i)?.split(";")?.get(0),
                gene = geneName?.get(i)?.split(";")?.get(0),
                fc = if(foldChanges?.get(i).isNaN()) null else foldChanges?.get(i),
                pVal = if(pVal.isNaN()) null else pVal,
                plotPVal = if(plotPVal.isNaN()) null else plotPVal,
                isSign = isSign
            )
        }

        return VolcanoPlot(data = volcanoData)
    }

}