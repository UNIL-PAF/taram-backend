package ch.unil.pafanalysis.analysis.steps.boxplot

data class BoxPlot(
    val experimentNames: List<String>? = null,
    val data: List<BoxPlotGroupData>? = null
)

data class BoxPlotGroupData(
    val group: String? = null,
    val data: List<List<BoxPlotData>>? = null,
)

data class BoxPlotData(
    val name: String? = null,
    val data: List<Double>? = null
)
