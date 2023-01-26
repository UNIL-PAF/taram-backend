package ch.unil.pafanalysis.analysis.steps.scatter_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.log10

@Service
class AsyncScatterPlotRunner() : CommonStep() {

    private val readTableData = ReadTableData()

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
        val field = params?.column ?: analysisStep?.columnInfo?.columnMapping?.intCol

        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )

        val xId =
            table.headers?.find { h -> h.experiment?.field == field && h.experiment?.name?.contains(params.xAxis!!) ?: false }
        val yId =
            table.headers?.find { h -> h.experiment?.field == field && h.experiment?.name?.contains(params.yAxis!!) ?: false }
        val xList = readTableData.getDoubleColumn(table, xId?.name!!)
        val yList = readTableData.getDoubleColumn(table, yId?.name!!)

        // get the gene or protein name
        val protGroup = readTableData.getStringColumn(table, "Majority.protein.IDs")?.map { it.split(";")?.get(0) }
        val genes = readTableData.getStringColumn(table, "Gene.names")?.map { it.split(";")?.get(0) }
        val names = genes?.zip(protGroup!!)?.map { a ->
            if (a.first.isNotEmpty()) {
                a.first
            } else {
                a.second
            }
        }

        // add colorByData
        val colData = if (!params.colorBy.isNullOrEmpty()) {
            getColorData(params.colorBy, table)
        } else null

        val doLog = params.logTrans == true

        val data: List<ScatterPoint>? =
            xList?.mapIndexed { i, x ->
                val y = yList?.get(i)
                ScatterPoint(
                    x = if (doLog) {
                        computeLog(x)
                    } else x,
                    y = if (doLog) {
                        computeLog(y)
                    } else y,
                    d = colData?.get(i),
                    n = names?.get(i)
                )
            }

        return ScatterPlot(data)
    }

    private fun computeLog(d: Double?): Double? {
        val a = if (d == 0.0) null else d
        return if (a == null) a else log10(a)
    }

    private fun getColorData(colorBy: String, table: Table?): List<Double?>? {
        val isDirectVal: Header? = table?.headers?.find { h -> h.name == colorBy }
        return if (isDirectVal !== null) {
            readTableData.getDoubleColumn(table, colorBy)?.map { if (it.isNaN()) null else it }
        } else {
            val matrix = readTableData.getDoubleMatrixByRow(table, colorBy)
            matrix.second.map { it.average() }
        }
    }

}