package ch.unil.pafanalysis.analysis.steps.correlation_table

data class CorrelationTableParams(
    val correlationType: String? = null,
    val intCol: String? = null,
)

enum class CorrelationType(val value: String) {
    PEARSON("pearson"),
    SPEARMAN("spearman"),
}