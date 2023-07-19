package ch.unil.pafanalysis.analysis.steps.scatter_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.log10

@Service
class AsyncScatterPlotRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val hMap = HeaderTypeMapping()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val scatterPlot = createScatterPlot(newStep)
            newStep?.copy(
                results = gson.toJson(scatterPlot),
            )
        }
        tryToRun(funToRun, newStep)
    }

    private fun createScatterPlot(analysisStep: AnalysisStep?): ScatterPlot? {
        val params = gson.fromJson(analysisStep?.parameters, ScatterPlotParams().javaClass)

        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )

        val xId = table.headers?.find { h -> h.name == params.xAxis!! }
        val yId = table.headers?.find { h -> h.name == params.yAxis!! }
        val xList = readTableData.getDoubleColumn(table, xId?.name!!)
        val yList = readTableData.getDoubleColumn(table, yId?.name!!)

        val resType = analysisStep?.analysis?.result?.type
        val protColName = hMap.getCol("proteinIds", resType)
        val geneColName = hMap.getCol("geneNames", resType)

        // get the gene or protein name
        val protGroup = readTableData.getStringColumn(table, protColName)?.map { it.split(";")?.get(0) }
        val genes = readTableData.getStringColumn(table, geneColName)?.map { it.split(";")?.get(0) }
        val names = genes?.zip(protGroup!!)?.map { a ->
            if (a.first.isNotEmpty()) {
                a.first
            } else {
                a.second
            }
        }

        // add colorByData
        val colData = if (!params.colorBy.isNullOrEmpty()) {
            getColorData(params.colorBy, table, analysisStep?.columnInfo?.columnMapping?.experimentDetails)
        } else null

        val data: List<ScatterPoint>? =
            xList?.mapIndexed { i, x ->
                val y = yList?.get(i)
                val tmpCol = colData?.get(i)

                ScatterPoint(
                    x = if(x.isNaN()) null else x,
                    y = if(y?.isNaN() == true) null else y,
                    d = if(tmpCol?.isNaN() == true) null else tmpCol,
                    n = names?.get(i)
                )
            }

        val sortedData = data?.sortedBy { it.d }

        return ScatterPlot(sortedData)
    }

    private fun computeLog(d: Double?): Double? {
        val a = if (d == 0.0) null else d
        return if (a == null) a else log10(a)
    }

    private fun getColorData(colorBy: String, table: Table?, expDetails: Map<String, ExpInfo>?): List<Double?>? {
        val isDirectVal: Header? = table?.headers?.find { h -> h.name == colorBy }
        return if (isDirectVal !== null) {
            readTableData.getDoubleColumn(table, colorBy)?.map { if (it.isNaN()) null else it }
        } else {
            val matrix = readTableData.getDoubleMatrixByRow(table, colorBy, expDetails)
            matrix.second.map { it.average() }
        }
    }

}