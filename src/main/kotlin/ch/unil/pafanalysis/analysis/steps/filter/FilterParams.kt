package ch.unil.pafanalysis.analysis.steps.filter

data class FilterParams (
    val removeOnlyIdentifiedBySite: Boolean? = null,
    val removeReverse: Boolean? = null,
    val removePotentialContaminant: Boolean? = null,
    val colFilters: List<ColFilter>? = null
)

data class ColFilter(
    val colName: String,
    val comparator: Comparator,
    val removeSelected: Boolean = true
)

enum class Comparator(val value: String, val symbol: String) {
    GT("gt", ">"),
    ST("sd", "<"),
    EQ("eq", "=="),
    NOT("not", "!="),
    GE("ge", ">="),
    SE("se", "<=")
}