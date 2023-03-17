package ch.unil.pafanalysis.analysis.model

data class FullProteinTable (
    val stepId: Int? = null,
    val rows: List<FullProtRow>? = null,
    val headers: List<Header>? = null,
)

data class FullProtRow (
    val key: Int? = null,
    val cols: List<Any>
)