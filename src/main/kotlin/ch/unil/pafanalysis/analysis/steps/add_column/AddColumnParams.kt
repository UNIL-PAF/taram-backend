package ch.unil.pafanalysis.analysis.steps.add_column

import com.google.gson.annotations.SerializedName

data class AddColumnParams (
    val selIdxs: List<Int>? = null,
    val charColParams: CharColParams? = null,
    val numColParams: NumColParams? = null,
    val newColName: String? = null,
    val type: SelColType? = null
)

enum class SelColType(val value: String) {
    @SerializedName("char")
    CHAR("char"),
    @SerializedName("num")
    NUM("num"),
}

data class CharColParams(val compVal: String? = null, val compOp: CompOp? = null, val compSel: CompSel? = null)

enum class CompSel(val value: String) {
    @SerializedName("all")
    ALL("all"),
    @SerializedName("any")
    ANY("any"),
}

enum class CompOp(val value: String, val symbol: String) {
    @SerializedName("matches")
    MATCHES("matches", "=="),
    @SerializedName("matches-not")
    MATCHES_NOT("matches-not", "!="),
}

data class NumColParams(val mathOp: MathOp? = null, val removeNaN: Boolean?)

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