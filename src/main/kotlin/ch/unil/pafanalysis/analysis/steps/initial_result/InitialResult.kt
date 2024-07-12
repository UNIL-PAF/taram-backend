package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.SpectronautSetup

data class MaxQuantParameters(
    val matchBetweenRuns: Boolean? = null,
    val someGenesParsedFromFasta: Boolean? = null,
    val allGenesParsedFromFasta: Boolean? = null
)

data class InitialResult(
    val proteinGroupsTable: String? = null,
    val parametersTable: String? = null,
    val maxQuantParameters: MaxQuantParameters? = null,
    val spectronautSetup: SpectronautSetup? = null,
    val nrProteinGroups: Int? = null,
    val fastaFiles: List<String>? = null,
    val softwareVersion: String? = null
)