package ch.unil.pafanalysis.analysis.steps.scatter_plot

import ch.unil.pafanalysis.analysis.steps.EchartsPlot

data class ScatterPlot(
    val data: List<ScatterPoint>? = null,
    val plot: EchartsPlot? = null,
)

data class ScatterPoint(val x: Double?, val y: Double?, val d: Double? = null)