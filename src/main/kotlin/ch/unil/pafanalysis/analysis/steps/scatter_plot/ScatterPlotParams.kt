package ch.unil.pafanalysis.analysis.steps.scatter_plot

data class ScatterPlotParams (
    val column: String? = null,
    val xAxis: String? = null,
    val yAxis: String? = null,
    val colorBy: String? = null,
    val logScaleColor: Boolean = false
)
