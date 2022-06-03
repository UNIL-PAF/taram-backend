package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.steps.EchartsPlot

data class BoxPlot(
    val experimentNames: List<String>? = null,
    val data: List<BoxPlotGroupData>? = null,
    val plot: EchartsPlot? = null
)

data class BoxPlotGroupData(
    val group: String? = null,
    val data: List<BoxPlotData>? = null,
)

data class BoxPlotData(
    val name: String? = null,
    val data: List<Double>? = null
)
