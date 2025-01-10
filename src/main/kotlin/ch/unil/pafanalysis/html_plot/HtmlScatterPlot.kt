package ch.unil.pafanalysis.html_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.scatter_plot.ScatterPlot
import ch.unil.pafanalysis.analysis.steps.scatter_plot.ScatterPlotParams
import com.google.gson.Gson

object HtmlScatterPlot {

    val gson = Gson()

    val script = """
        chart.on('click', 'series', function (params) {
        var newOpt = {...option, animation: false}

        const newDataset = newOpt.dataset[0].source.map(a => {
            if(a[3] === params.data[3]){
                a[5] = !a[5]
                return a
            } else return a
        })
        newOpt.dataset[0].source = newDataset

        chart.setOption(newOpt);
    });
    """.trimIndent()

    fun addScript(html: String?, step: AnalysisStep): String? {
        val params = gson.fromJson(step.parameters, ScatterPlotParams::class.java)
        val res = gson.fromJson(step.results, ScatterPlot::class.java)

        val colorBy = """
            var params = {"colorBy": "${params.colorBy}"};
        """.trimIndent()

        val otherFieldValue = if((res.data?.get(0)?.other?.size ?: 0) > 0) "undefined" else res.data?.get(0)?.other?.get(0)?.name
        val otherField = """
          var otherField = "$otherFieldValue";  
        """.trimIndent()


        return html?.replace("__SCRIPT__", colorBy + otherField + script)
    }

}