package ch.unil.pafanalysis.analysis.steps.correlation_table

data class CorrelationTable(
    val correlationTable: String? = null,
    val correlationMatrix: List<List<Double>>? = null,
    val experimentNames: List<String>? = null,
    val groupNames: List<String>? = null,
)