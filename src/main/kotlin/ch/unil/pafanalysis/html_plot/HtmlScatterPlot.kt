package ch.unil.pafanalysis.html_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
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

        val colorBy = """
            var params = {
                "colorBy": "${params.colorBy}",
                "xAxis": "${params.xAxis}",
                "yAxis": "${params.yAxis}",
                "showProteinACs": "${params.showProteinACs}",
            };
        """.trimIndent()

        val otherField = """
          var otherField = undefined;  
        """.trimIndent()

        return replaceTooltip(html?.replace("__SCRIPT__", colorBy + otherField + script))
    }

    private fun replaceTooltip(html: String?): String? {

        val originalFunction = """
            function (p) {
                const text1 = "<strong>" + p.data[2] + "</strong><br>"
                const text2 = p.data[3] + "<br>"
                const text3 = params.xAxis + ": <strong>" + String(p.data[0].length > 5 ? p.data[0].toExponential(1) : p.data[0].toFixed(1)) + "</strong><br>"
                const text4 = params.yAxis + ": <strong>" + String(p.data[1].length > 5 ? p.data[1].toExponential(1) : p.data[1].toFixed(1)) + "</strong><br>"
                const text5 = (params.colorBy && params.colorBy !== "null") ? (params.colorBy + ": <strong>" + p.data[4].toFixed(1) + "</strong><br>") : ""
                const text6 = otherField ? (otherField + ": <strong>" + p.data[6] + "</strong><br>") : ""
                return text1 + text2 + text3 + text4 + text5 + text6
            }
        """.trimIndent()

        val funFound = extractFunction(html)

        return if(funFound != null) html?.replace(funFound, originalFunction) else html
    }

    private fun extractFunction(text: String?): String? {
        if (text.isNullOrEmpty()) return null

        val selText = Regex("""tooltip(.+)series""").find(text)?.groups?.get(1)?.value ?: return null

        //val textAfterTooltip = text.replace(Regex(""".+tooltip"""), "")

        val pattern = Regex("""function\s*\([^)]*\)\s*\{""")  // Matches function(x){
        val match = pattern.find(selText) ?: return null  // Find function declaration

        val startIndex = match.range.first

        var braceCount: Int? = null
        var endIndex = startIndex

        for (i in startIndex until selText.length) {
            when (selText[i]) {
                '{' -> if(braceCount == null) braceCount = 1 else braceCount++
                '}' -> if(braceCount != null) braceCount--
            }
            if (braceCount == 0) {  // Found matching closing brace
                endIndex = i + 1
                break
            }
        }

        return selText.substring(startIndex, endIndex)
    }


}