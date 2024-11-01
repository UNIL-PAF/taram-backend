package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.log10

@Service
class AsyncVolcanoPlotRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val hMap = HeaderTypeMapping()

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
        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )

        val params = gson.fromJson(analysisStep?.parameters, VolcanoPlotParams().javaClass)
        val compName = "." + params.comparison?.group1?.trim().plus("-" + params.comparison?.group2?.trim())

        val hasQVal: Boolean = (table.headers?.find { it.name == "q.value$compName" || it.name == "adj.p.value$compName"}) != null
        if (params?.useAdjustedPVal == true && !hasQVal) throw StepException("There are no adjusted p-values for this comparison. Please make sure you use a multiple test correction in your statistical test.")

        val resType = analysisStep?.analysis?.result?.type

        val value = readTableData.getDoubleColumn(table, "p.value$compName")

        // for back compatibility we look with and without log2 in name
        val fcHeaderName = table.headers?.find{ a -> a.name?.contains("fold.change$compName") ?: false}
        val foldChanges = readTableData.getDoubleColumn(table, fcHeaderName?.name ?: throw StepException("Could not fold change column for [$compName]."))

        val proteinName = readTableData.getStringColumn(table, hMap.getCol("proteinIds", resType))
        val geneName = readTableData.getStringColumn(table, hMap.getCol("geneNames", resType))
        val qVals = if(hasQVal) {
            val qVals1 = readTableData.getDoubleColumn(table, "q.value$compName")
            if(qVals1 != null) qVals1 else readTableData.getDoubleColumn(table, "adj.p.value$compName")
        } else null

        if (foldChanges == null) throw StepException("You have to run a statistical test before this plot.")

        // nr petides or precursors quantified
        val pepOrPreHeader = table.headers?.find { a ->
            a.experiment == null && (a.name?.contains("NrOfPrecursorsIdentified") ?: false || a.name?.contains("Razor.unique.peptides") ?: false)
        }
        val pepOrPreQuant = if(pepOrPreHeader?.name != null) readTableData.getDoubleColumn(table, pepOrPreHeader.name) else null

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
            val other = if(pepOrPreQuant != null) listOf(VolcanoPointInfo(pepOrPreHeader?.name, pepOrPreQuant?.get(i))) else null

            val genes = geneName?.get(i)

            VolcanoPoint(
                prot = proteinName?.get(i)?.split(";")?.get(0),
                gene = genes?.split(";")?.get(0),
                multiGenes= genes?.split(";")?.size ?: 0 > 1,
                fc = invalidToNull(foldChanges?.get(i)),
                pVal = invalidToNull(v),
                qVal = invalidToNull(qVal),
                plotVal = invalidToNull(plotPVal),
                isSign = isSign,
                qIsSign = qIsSign,
                other = other
            )
        }

        return VolcanoPlot(data = volcanoData)
    }

}