package ch.unil.pafanalysis.html_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotParams
import com.google.gson.Gson

object HtmlBoxPlot {

    val gson = Gson()

    fun addScript(html: String?, step: AnalysisStep): String? {
        val params = gson.fromJson(step.parameters, BoxPlotParams::class.java)
        val column = params.column ?: step.columnInfo?.columnMapping?.intCol

        val localAxisName = """
            var yAxisName = "$column"
        """.trimIndent()

        return replaceTooltip(html?.replace("__SCRIPT__", localAxisName))
    }

    private fun replaceTooltip(html: String?): String? {

        val originalFunction = """
            function(v){
                if(v.seriesType === "boxplot"){
                    return "<strong>" + v.seriesName.replace("group_", "") + "<br>"
                        + v.value[0] + "</strong><br>" +
                        "Min: " + v.value[1].toFixed(1) + "<br>" +
                        "Q1: " + v.value[2].toFixed(1) + "<br>" +
                        "Median: " + v.value[3].toFixed(1) + "<br>" +
                        "Q3: " + v.value[4].toFixed(1) + "<br>" +
                        "Max: " + v.value[5].toFixed(1) + "<br>"

                }else if(v.seriesType === "line"){
                    return "<strong>" + v.seriesName + "<br>"
                        + v.name + "</strong><br>" +
                        yAxisName + ": " + v.value.toFixed(1)
                }else return null
            }
        """.trimIndent()

        val funFound = extractFunction(html)

        return if(funFound != null) html?.replace(funFound, originalFunction) else html
    }

    private fun extractFunction(text: String?): String? {
        if (text.isNullOrEmpty()) return null

        val textAfterTooltip = text.replace(Regex(""".+tooltip"""), "")

        val pattern = Regex("""function\s*\([^)]*\)\s*\{""")  // Matches function(x){
        val match = pattern.find(textAfterTooltip) ?: return null  // Find function declaration

            val startIndex = match.range.first

            var braceCount: Int? = null
            var endIndex = startIndex

            for (i in startIndex until textAfterTooltip.length) {
                when (textAfterTooltip[i]) {
                    '{' -> if(braceCount == null) braceCount = 1 else braceCount++
                    '}' -> if(braceCount != null) braceCount--
                }
                if (braceCount == 0) {  // Found matching closing brace
                    endIndex = i + 1
                    break
                }
            }

        return textAfterTooltip.substring(startIndex, endIndex)
    }

}