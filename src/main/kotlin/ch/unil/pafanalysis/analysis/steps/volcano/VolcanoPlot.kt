package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.steps.EchartsPlot

data class VolcanoPlot(
    val data: List<VolcanoPoint>? = null,
    val plot: EchartsPlot? = null
)

data class VolcanoPoint(
    val prot: String? = null,
    val gene: String? = null,
    val multiGenes: Boolean? = null,
    val fc: Double? = null,
    val pVal: Double? = null,
    val qVal: Double? = null,
    val plotVal: Double? = null,
    val isSign: Boolean? = null,
    val qIsSign: Boolean? = null,
    val other: List<VolcanoPointInfo>? = null
)

data class VolcanoPointInfo(val name: String? = null, val value: Double? = null)
