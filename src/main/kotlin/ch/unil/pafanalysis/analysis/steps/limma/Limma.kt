package ch.unil.pafanalysis.analysis.steps.limma

data class Limma (val comparisions: List<LimmaComparision>? = null)

data class LimmaComparision(
    val firstGroup: String? = null,
    val secondGroup: String? = null,
    val numberOfSignificant: Int? = null,
    val nrPassedFilter: Int? = null
)