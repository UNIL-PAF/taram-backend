package ch.unil.pafanalysis.analysis.model

data class ProteinTable (
    val table: List<ProteinGroup>? = null,
    val resultType: String? = null,
    val intField: String? = null
)

data class ProteinGroup (
    val key: Int? = null,
    val prot: String? = null,
    val protGroup: String? = null,
    val gene: String? = null,
    val desc: String? = null,
    val int: Double? = null,
    val sel: Boolean? = null
)