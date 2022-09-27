package ch.unil.pafanalysis.analysis.model

import ch.unil.pafanalysis.results.model.ResultType

data class ProteinTable (
    val table: List<ProteinGroup>? = null,
    val resultType: String? = null
)

data class ProteinGroup (
    val key: Int? = null,
    val prot: String? = null,
    val gene: String? = null,
    val desc: String? = null,
    val int: Double? = null,
    val ibaq: Double? = null,
)