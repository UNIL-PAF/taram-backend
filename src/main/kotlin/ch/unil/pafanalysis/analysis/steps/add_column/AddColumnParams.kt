package ch.unil.pafanalysis.analysis.steps.add_column

import com.google.gson.annotations.SerializedName

data class AddColumnParams (
    val selColIdx: Int? = null,
    val charColParams: CharColParams? = null,
    val addConditionNames: Boolean? = null
)

data class CharColParams(val strVal: String? = null, val charComp: CharComp? = null)

enum class CharComp(val value: String, val symbol: String) {
    @SerializedName("matches")
    GT("matches", "=="),
    @SerializedName("matches_not")
    ST("matches_not", "!="),
}