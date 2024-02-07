package ch.unil.pafanalysis.analysis.steps.scatter_plot

import ch.unil.pafanalysis.analysis.steps.EchartsPlot

data class ScatterPlot(
    val data: List<ScatterPoint>? = null,
    val plot: EchartsPlot? = null,
    val linearRegression: LinearRegression? = null
)

data class ScatterPoint(val x: Double?, val y: Double?, val d: Double? = null, val n: String?)

data class LinearRegression(val slope: Double?, val intercept: Double?, val rSquare: Double?)