package ch.unil.pafanalysis.analysis.steps.umap

import ch.unil.pafanalysis.analysis.steps.EchartsPlot

data class UmapRes(
    val groups: List<String>? = null,
    val groupColors: List<String>? = null,
    val nrUmaps: Int? = null,
    val plot: EchartsPlot? = null,
    val umapList: List<OneUmapRow>? = null,
)

data class OneUmapRow(val groupName: String?, val expName: String?, val umapVals: List<Double>?)