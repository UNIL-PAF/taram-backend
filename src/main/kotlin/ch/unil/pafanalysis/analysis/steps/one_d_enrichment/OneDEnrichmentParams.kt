package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

data class OneDEnrichmentParams(
    val colIdxs: List<Int>? = null,
    val fdrCorrection: Boolean? = null,
    val categoryIds: List<Int>? = null,
    val annotationId: Int? = null,
    val threshold: Double? = null,
    val selResults: List<Int>? = null
)