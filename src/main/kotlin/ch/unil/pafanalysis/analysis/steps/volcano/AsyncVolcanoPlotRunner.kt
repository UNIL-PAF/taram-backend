package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.HeaderMaps
import ch.unil.pafanalysis.common.ReadTableData
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
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

    private fun invalidToNull(d: Double?): Double?{
        return if(d?.isNaN() != false || d?.isInfinite()?:true) return null else d
    }

    private fun createVolcanoObj(analysisStep: AnalysisStep?): VolcanoPlot {
        val headerMap = HeaderMaps.getHeaderMap(analysisStep?.analysis?.result?.type)

        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )

        val params = gson.fromJson(analysisStep?.parameters, VolcanoPlotParams().javaClass)
        val compName = "." + params.comparison?.group1?.trim().plus("-" + params.comparison?.group2?.trim())

        val hasQVal: Boolean = (table.headers?.find { it.name == "q.value$compName" }) != null
        if (params?.useAdjustedPVal == true && !hasQVal) throw StepException("There are no q-values for this comparison. Please make sure you use a multiple test correction in your statistical test.")

        val value = readTableData.getDoubleColumn(table, "p.value$compName")
        val foldChanges = readTableData.getDoubleColumn(table, "fold.change$compName")
        val proteinName = readTableData.getStringColumn(table, headerMap.get("prot")!!)
        val geneName = readTableData.getStringColumn(table, headerMap.get("gene")!!)
        val qVals = if(hasQVal) readTableData.getDoubleColumn(table, "q.value$compName") else null

        if (foldChanges == null) throw StepException("You have to run a statistical test before this plot.")

        val volcanoData = value?.mapIndexed { i, v ->
            val plotValTarget = if(params?.useAdjustedPVal == true) qVals?.get(i) ?: Double.NaN else v
            val plotPVal = if (params?.log10PVal == true) log10(plotValTarget) * -1 else plotValTarget
            val isSign = v <= (params?.pValThresh ?: 0.0) && kotlin.math.abs(foldChanges?.get(i)) >= (params?.fcThresh
                ?: 10000.0)
            val qIsSign = if(qVals != null && foldChanges != null) qVals[i] <= (params?.pValThresh ?: 0.0) && kotlin.math.abs(
                foldChanges[i]
            ) >= (params?.fcThresh
                ?: 10000.0) else null

            val qVal = qVals?.get(i)

            VolcanoPoint(
                prot = proteinName?.get(i)?.split(";")?.get(0),
                gene = geneName?.get(i)?.split(";")?.get(0),
                fc = invalidToNull(foldChanges?.get(i)),
                pVal = invalidToNull(v),
                qVal = invalidToNull(qVal),
                plotVal = invalidToNull(plotPVal),
                isSign = isSign,
                qIsSign = qIsSign
            )
        }

        return VolcanoPlot(data = volcanoData)
    }

}