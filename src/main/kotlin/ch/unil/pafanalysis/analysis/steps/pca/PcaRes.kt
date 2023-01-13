package ch.unil.pafanalysis.analysis.steps.pca

import ch.unil.pafanalysis.analysis.steps.EchartsPlot

data class PcaRes(
    val groups: List<String>? = null,
    val explVars: List<Double>? = null,
    val nrPc: Int? = null,
    val plot: EchartsPlot? = null,
    val pcList: List<OnePcRow>? = null
)

data class OnePcRow(val groupName: String?, val expName: String?, val pcVals: List<Double>?)