package ch.unil.pafanalysis.analysis.steps.add_column

import com.google.gson.annotations.SerializedName

data class AddColumnParams (
    val selectedColumnIdx: Int? = null,
    val charColParams: CharColParams? = null,
    val numExpColParams: NumExpColParams? = null,
    val newColName: String? = null,
    val type: SelColType? = null
)

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

data class CharColParams(val strVal: String? = null, val charComp: CharComp? = null)

enum class CharComp(val value: String, val symbol: String) {
    @SerializedName("matches")
    MATCHES("matches", "=="),
    @SerializedName("matches-not")
    MATCHES_NOT("matches-not", "!="),
}

data class NumExpColParams(val mathOp: MathOp? = null)

enum class MathOp(val value: String) {
    @SerializedName("min")
    MIN("min"),
    @SerializedName("max")
    MAX("max"),
    @SerializedName("mean")
    MEAN("mean"),
    @SerializedName("median")
    MEDIAN("median"),
    @SerializedName("sum")
    SUM("sum"),

}