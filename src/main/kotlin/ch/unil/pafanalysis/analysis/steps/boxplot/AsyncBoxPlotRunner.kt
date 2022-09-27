package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.google.common.math.Quantiles
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.log2

@Service
class AsyncBoxPlotRunner() : CommonStep() {

    private val readTableData = ReadTableData()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?, paramsString: String?) {
        val funToRun: () -> AnalysisStep? = {
            val boxplot = createBoxplotObj(newStep)

            newStep?.copy(
                results = gson.toJson(boxplot),
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun createBoxplotObj(analysisStep: AnalysisStep?): BoxPlot {
        val expDetailsTable = analysisStep?.columnInfo?.columnMapping?.experimentNames?.map { name ->
            analysisStep?.columnInfo?.columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val experimentNames = expDetailsTable?.map { it?.name!! }
        val groupedExpDetails: Map<String?, List<ExpInfo?>>? = expDetailsTable?.groupBy { it?.group }

        val params = gson.fromJson(analysisStep?.parameters, BoxPlotParams().javaClass)
        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )
        val intCol = params?.column ?: analysisStep?.columnInfo?.columnMapping?.intCol

        val boxplotGroupData = groupedExpDetails?.mapKeys { createBoxplotGroupData(it.key, params, table, intCol) }
        val selProtData = getSelProtData(table, intCol, params)


        return BoxPlot(experimentNames = experimentNames, boxPlotData = boxplotGroupData?.keys?.toList(), selProtData = selProtData)
    }

    private fun getSelProtData(table: Table?, intCol: String?, params: BoxPlotParams?): List<SelProtData>? {
        if(params?.selProts == null) return null
        val intMatrix = readTableData.getDoubleMatrix(table, intCol).second

        val protGroup = readTableData.getStringColumn(table, "Majority.protein.IDs")
        return params?.selProts.map{ p ->
            val i = protGroup?.indexOf(p)
            val ints = intMatrix.map{ if(i == null) Double.NaN else it[i]}
            val normInts = if (params?.logScale != false) {
                ints.filter { it != 0.0 && !it.isNaN() }.map { log2(it) }
            } else {
                ints
            }
            SelProtData(prot = p, ints = normInts)
        }
    }

    private fun createBoxplotGroupData(
        group: String?,
        params: BoxPlotParams?,
        table: Table?,
        intCol: String?
    ): BoxPlotGroupData {
        val (headers, ints) = readTableData.getDoubleMatrix(table, intCol, group)

        val listOfBoxplots =
            headers.mapIndexed { i, h -> BoxPlotData(h.experiment?.name, computeBoxplotData(ints[i], params?.logScale)) }

        return BoxPlotGroupData(group = group, groupData = listOfBoxplots)
    }


    private fun computeBoxplotData(ints: List<Double>, logScale: Boolean?): List<Double>? {
        val normInts = if (logScale != false) {
            ints.filter { it != 0.0 && !it.isNaN() }.map { log2(it) }
        } else {
            ints
        }

        val intsFlt = normInts.filter { !it.isNaN() }

        val min = intsFlt.minOrNull()!!
        val q25: Double = Quantiles.percentiles().index(25).compute(intsFlt)
        val q50: Double = Quantiles.percentiles().index(50).compute(intsFlt)
        val q75: Double = Quantiles.percentiles().index(75).compute(intsFlt)
        val max = intsFlt.maxOrNull()!!
        return listOf(min, q25, q50, q75, max)
    }

}