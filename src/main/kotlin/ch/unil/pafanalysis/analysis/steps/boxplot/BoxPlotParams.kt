package ch.unil.pafanalysis.analysis.steps.boxplot

data class BoxPlotParams (
    val column: String? = null,
    val logScale: Boolean? = null,
    val selProts: List<String>? = null,
    val groupByCondition: Boolean? = null
)