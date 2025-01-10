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
        return html?.replace("__SCRIPT__", script)
    }

}