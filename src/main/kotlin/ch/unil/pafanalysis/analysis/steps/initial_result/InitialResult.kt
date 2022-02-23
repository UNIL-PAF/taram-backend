package ch.unil.pafanalysis.analysis.steps.initial_result

data class MaxQuantParameters(
    val version: String? = null,
    val matchBetweenRuns: Boolean? = null
)

data class InitialResult(
    val proteinGroupsTable: String? = null,
    val parametersTable: String? = null,
    val maxQuantParameters: MaxQuantParameters? = null
)