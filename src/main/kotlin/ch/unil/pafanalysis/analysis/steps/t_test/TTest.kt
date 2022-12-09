package ch.unil.pafanalysis.analysis.steps.t_test

data class TTest (val comparisions: List<TTestComparision>? = null)

data class TTestComparision(
    val firstGroup: String? = null,
    val secondGroup: String? = null,
    val numberOfSignificant: Int? = null
)