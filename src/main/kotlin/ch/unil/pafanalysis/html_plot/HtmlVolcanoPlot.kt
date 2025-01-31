package ch.unil.pafanalysis.html_plot

object HtmlVolcanoPlot {

    val script = """
         chart.on('click', 'series', function (params) {
        var newOpt = {...option, animation: false}
        const newDataset = newOpt.dataset[0].source.map(a => {
            if(a.prot === params.data.prot){
                return {...a, showLab: !a.showLab}
            } else return a
        })
        newOpt.dataset[0].source = newDataset
        chart.setOption(newOpt);
    });
    """.trimIndent()

    fun addScript(html: String?): String? {
        return replaceTooltip(
            html?.replace(
                "__SCRIPT__",
                script
            )
        )
    }

    private fun replaceTooltip(html: String?): String? {

        val originalFunction = """
             function (myParams) {
                    if (myParams.componentType === "markLine") {
                        const text =
                            myParams.data.name + " threshold: " + (myParams.data.name.includes("Fold") ? myParams.data.value : params.pValThresh);
                        return text;
                    } else {
                        const other = myParams.data.other ? myParams.data.other.map(a => {
                            return a.name + ": <strong>" + a.value + "</strong><br>"
                        }) : ""

                        return "Gene: <strong>" + myParams.data.gene + "</strong><br>" +
                            "Protein AC: <strong>" + myParams.data.prot + "</strong><br>" +
                            "p-value: <strong>" + myParams.data.pVal.toPrecision(3) + "</strong><br>" +
                            (myParams.data.qVal ? "adj. p-value: <strong>" + myParams.data.qVal.toPrecision(3) + "</strong><br>" : "") +
                            "fold change: <strong>" + myParams.data.fc.toFixed(2) + "</strong><br>" + other

                    }
                }
        """.trimIndent()

        val funFound = extractFunction(html)

        return if(funFound != null) html?.replace(funFound, originalFunction) else html
    }

    private fun extractFunction(text: String?): String? {
        if (text.isNullOrEmpty()) return null

        val selText = Regex("""tooltip(.+)dataset""").find(text)?.groups?.get(1)?.value ?: return null

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