package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

data class OneDEnrichment(val enrichmentTable: String? = null, val selResults: List<EnrichmentRow>? = null)

data class EnrichmentRow(
    val id: Int?,
    val column: String?,
    val type: String?,
    val name: String?,
    val size: Int?,
    val score: Double?,
    val pValue: Double?,
    val qValue: Double?,
    val mean: Double?,
    val median: Double?
)