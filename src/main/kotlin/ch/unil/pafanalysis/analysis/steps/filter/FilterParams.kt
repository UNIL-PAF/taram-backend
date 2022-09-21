package ch.unil.pafanalysis.analysis.steps.filter

import com.google.gson.annotations.SerializedName

data class FilterParams (
    val removeOnlyIdentifiedBySite: Boolean? = null,
    val removeReverse: Boolean? = null,
    val removePotentialContaminant: Boolean? = null,
    val minNrPeptides: Int? = null,
    val colFilters: List<ColFilter>? = null
)

data class ColFilter(
    val colName: String,
    val comparator: Comparator,
    val removeSelected: Boolean = true,
    val compareToValue: String
)

enum class Comparator(val value: String, val symbol: String, val numOnly: Boolean) {
    @SerializedName("gt")
    GT("gt", ">", true),
    @SerializedName("sd")
    ST("sd", "<", true),
    @SerializedName("eq")
    EQ("eq", "==", false),
    @SerializedName("not")
    NOT("not", "!=", false),
    @SerializedName("ge")
    GE("ge", ">=", true),
    @SerializedName("se")
    SE("se", "<=", true)
}