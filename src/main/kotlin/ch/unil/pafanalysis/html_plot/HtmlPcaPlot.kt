package ch.unil.pafanalysis.html_plot

object HtmlPcaPlot {

    val script = """
            chart.on('click', 'series', function (params) {
        var newOpt = {...option, animation: false}

        const newDataset = newOpt.dataset[0].source.map(a => {
            if(a[0] === params.data[0]){
                a[4] = !a[4]
                return a
            } else return a
        })
        newOpt.dataset[0].source = newDataset

        chart.setOption(newOpt);
    });
    """.trimIndent()

    fun addScript(html: String?): String? {
        return html?.replace("__SCRIPT__", script)
    }

}