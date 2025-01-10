package ch.unil.pafanalysis.html_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotParams
import com.google.gson.Gson

object HtmlBoxPlot {

    val gson = Gson()

    fun addScript(html: String?, step: AnalysisStep): String? {
        val params = gson.fromJson(step.parameters, BoxPlotParams::class.java)
        val column = params.column ?: step.columnInfo?.columnMapping?.intCol
        return html?.replace("__SCRIPT__", "var yAxisName = \"$column\"")
    }

}