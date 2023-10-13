package ch.unil.pafanalysis.analysis.steps.add_column

import com.google.gson.annotations.SerializedName

data class AddColumnParams (
    val selectedColumn: String? = null,
    val charColParams: CharColParams? = null,
    val newColName: String? = null,
    val type: SelColType? = null
)

data class CharColParams(val strVal: String? = null, val charComp: CharComp? = null)

enum class SelColType(val value: String) {
    @SerializedName("char")
    CHAR("char"),
    @SerializedName("char-exp")
    CHAR_EXP("char-exp"),
    @SerializedName("num")
    NUM("num"),
    @SerializedName("num-exp")
    NUM_EXP("num-exp")
}

enum class CharComp(val value: String, val symbol: String) {
    @SerializedName("matches")
    MATCHES("matches", "=="),
    @SerializedName("matches-not")
    MATCHES_NOT("matches-not", "!="),
}