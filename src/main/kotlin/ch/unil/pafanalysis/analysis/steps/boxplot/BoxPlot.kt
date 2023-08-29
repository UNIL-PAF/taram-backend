package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.steps.EchartsPlot

data class BoxPlot(
    val experimentNames: List<String>? = null,
    val boxPlotData: List<BoxPlotGroupData>? = null,
    val selProtData: List<SelProtData>? = null,
    val plot: EchartsPlot? = null
)

data class SelProtData(
    val prot: String? = null,
    val gene: String? = null,
    val ints: List<Double?>? = null,
    val logInts: List<Double?>? = null
)

data class BoxPlotGroupData(
    val group: String? = null,
    val groupData: List<BoxPlotData>? = null,
)

data class BoxPlotData(
    val name: String? = null,
    val data: List<Double>? = null,
    val logData: List<Double>? = null
)
