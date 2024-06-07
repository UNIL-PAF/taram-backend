package ch.unil.pafanalysis.analysis.steps.umap

data class UmapParams (
    val column: String? = null,
    val nrOfNeighbors: Int? = null,
    val minDistance: Double? = null,
    val selExps: List<String>? = null,
    val useAllGroups: Boolean? = null,
    val selGroups: List<String>? = null
)
