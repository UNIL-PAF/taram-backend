package ch.unil.pafanalysis.analysis.steps.add_column

import com.google.gson.annotations.SerializedName

data class AddColumnParams (
    val selectedColumn: String? = null,
    val charColParams: CharColParams? = null,
    val newColName: String? = null
)

data class CharColParams(val strVal: String? = null, val charComp: CharComp? = null)

enum class CharComp(val value: String, val symbol: String) {
    @SerializedName("matches")
    MATCHES("matches", "=="),
    @SerializedName("matches-not")
    MATCHES_NOT("matches-not", "!="),
}