package ch.unil.pafanalysis.analysis.steps.imputation

data class ImputationParams(
    val imputationType: String? = null,
    val intCol: String? = null,
    val selColIdxs: List<Int>? = null,
    val normImputationParams: NormImputationParams? = null,
    val replaceValue: Double? = null,
)

data class NormImputationParams(
    val width: Double? = 0.3,
    val downshift: Double? = 1.8,
    val seed: Int? = 1
)

enum class ImputationType(val value: String) {
    NAN("nan"),
    NORMAL("normal"),
    VALUE("value"),
    NONE("none")
}