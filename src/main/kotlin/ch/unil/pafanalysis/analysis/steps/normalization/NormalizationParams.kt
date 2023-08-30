package ch.unil.pafanalysis.analysis.steps.normalization

data class NormalizationParams (
    val normalizationType: String? = null,
    val normalizationCalculation: String? = null,
    val intCol: String? = null,
)

enum class NormalizationType(val value: String) {
    MEDIAN("median"),
    MEAN("mean"),
    NONE("none")
}

enum class NormalizationCalculation(val value: String) {
    SUBSTRACTION("substraction"),
    DIVISION("division")
}