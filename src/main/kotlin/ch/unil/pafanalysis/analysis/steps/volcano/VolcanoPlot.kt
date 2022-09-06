package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.steps.EchartsPlot

data class VolcanoPlot(
    val data: List<VolcanoPoint>? = null,
    val plot: EchartsPlot? = null
)

data class VolcanoPoint(
    val name: String? = null,
    val fc: Double? = null,
    val pVal: Double? = null,
    val isSign: Boolean? = null
)