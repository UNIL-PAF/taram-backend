package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

data class OneDEnrichment(
    val enrichmentTable: String? = null,
    val selResults: List<EnrichmentRow>? = null,
    val annotation: EnrichmentAnnotationInfo? = null,
    val selColumn: String? = null
)

data class EnrichmentAnnotationInfo(
    val name: String? = null,
    val description: String? = null,
    val origFileName: String? = null,
    val nrEntries: Int? = null,
    val creationString: String? = null,
    val selHeaderNames: List<String>? = null
)

data class EnrichmentRow(
    val id: Int?,
    val column: String?,
    val type: String?,
    val name: String?,
    val size: Int?,
    val score: Double?,
    val pvalue: Double?,
    val qvalue: Double?,
    val mean: Double?,
    val median: Double?
)