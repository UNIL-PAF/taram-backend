package ch.unil.pafanalysis.analysis.steps.volcano

data class VolcanoPlotParams(
    val pValThresh: Double? = null,
    val fcThresh: Double? = null,
    val useAdjustedPVal: Boolean? = true,
    val log10PVal: Boolean = true,
    val selProteins: List<String>? = null,
    val comparison: ComparisonParams? = null,
    val showQVal: Boolean? = null
)

data class ComparisonParams (val group1: String, val group2: String)