package ch.unil.pafanalysis.analysis.steps.stat_test

data class StatTest (val comparisions: List<StatTestComparision>? = null)

data class StatTestComparision(
    val firstGroup: String? = null,
    val secondGroup: String? = null,
    val numberOfSignificant: Int? = null,
    val nrPassedFilter: Int? = null
)