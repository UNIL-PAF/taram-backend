package ch.unil.pafanalysis.analysis.steps.normalization

data class NormalizationParams (
    val normalizationType: String? = null,
    val normalizationCalculation: String? = null,
    val intCol: String? = null,
    val selProts: List<String>? = null,
)

enum class NormalizationType(val value: String) {
    MEDIAN("median"),
    MEAN("mean"),
    NONE("none"),
    SEL_PROT("sel-prot"),
}

enum class NormalizationCalculation(val value: String) {
    SUBSTRACTION("substraction"),
    DIVISION("division")
}