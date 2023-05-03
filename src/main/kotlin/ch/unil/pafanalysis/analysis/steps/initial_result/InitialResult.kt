package ch.unil.pafanalysis.analysis.steps.initial_result

data class MaxQuantParameters(
    val matchBetweenRuns: Boolean? = null
)

data class InitialResult(
    val proteinGroupsTable: String? = null,
    val parametersTable: String? = null,
    val maxQuantParameters: MaxQuantParameters? = null,
    val nrProteinGroups: Int? = null,
    val fastaFiles: List<String>? = null,
    val softwareVersion: String? = null
)