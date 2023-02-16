package ch.unil.pafanalysis.analysis.steps.group_filter

data class GroupFilterParams (
    val minNrValid: Int? = null,
    val filterInGroup: String? = null,
    val field: String? = null,
    val zeroIsInvalid: Boolean? = null
)

enum class FilterInGroup(val value: String, val text: String) {
    ALL_GROUPS("all_groups", "all groups"),
    ONE_GROUP("one_group", "one group"),
    TOTAL("total", "total")
}