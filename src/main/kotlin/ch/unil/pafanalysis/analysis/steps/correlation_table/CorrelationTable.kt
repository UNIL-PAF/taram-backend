package ch.unil.pafanalysis.analysis.steps.correlation_table

data class CorrelationTable(
    val correlationTable: String? = null,
    val correlationMatrix: List<OneCorrelation>? = null,
    val experimentNames: List<String>? = null,
    val groupNames: List<String>? = null,
    val colors: List<String>? = null,
    val groupsAndColors: List<OneGroupAndColor>? = null,
)

data class OneCorrelation(val x: Int, val y: Int, val v: Double)

data class OneGroupAndColor(val group: String, val color: String?)