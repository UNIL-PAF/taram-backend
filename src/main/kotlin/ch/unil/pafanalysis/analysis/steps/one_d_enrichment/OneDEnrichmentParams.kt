package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import com.google.gson.annotations.SerializedName

data class OneDEnrichmentParams(
    val colIdx: Int? = null,
    val fdrCorrection: Boolean? = null,
    val categoryNames: List<String>? = null,
    val annotationId: Int? = null,
    val threshold: Double? = null
)