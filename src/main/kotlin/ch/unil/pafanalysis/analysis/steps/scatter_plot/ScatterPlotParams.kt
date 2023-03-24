package ch.unil.pafanalysis.analysis.steps.scatter_plot

data class ScatterPlotParams (
    val xAxis: String? = null,
    val yAxis: String? = null,
    val colorBy: String? = null,
    val logScaleColor: Boolean? = null,
    val logTrans: Boolean? = null
)
