package ch.unil.pafanalysis.analysis.steps.remove_imputed

import com.google.gson.annotations.SerializedName

data class RemoveImputedParams (
    val replaceBy: ReplaceBy? = null
)

enum class ReplaceBy(val ident: String, val value: Double, val printName: String) {
    @SerializedName("nan")
    NAN("nan", Double.NaN, "NaN"),
    @SerializedName("zero")
    ZERO("zero", 0.0, "0"),
}
