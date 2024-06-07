package ch.unil.pafanalysis.analysis.steps.pca

data class PcaParams (
    val column: String? = null,
    val center: Boolean? = null,
    val scale: Boolean? = null,
    val selExps: List<String>? = null,
    val useAllGroups: Boolean? = null,
    val selGroups: List<String>? = null
)
