package ch.unil.pafanalysis.analysis.steps.transformation

data class TransformationParams (
    val normalizationType: String? = null,
    val transformationType: String? = null,
    val imputationType: String? = null,
    val intCol: String? = null,
    val imputationParams: ImputationParams? = null
)

data class ImputationParams (
    val replaceValue: Double? = null,
    val width: Double? = 0.3,
    val downshift: Double? = 1.8,
    val seed: Int? = 1
)

enum class NormalizationType(val value: String) {
    MEDIAN("median"),
    MEAN("mean"),
    NONE("none")
}

enum class TransformationType(val value: String) {
    LOG2("log2"),
    NONE("none")
}

enum class ImputationType(val value: String){
    NAN("nan"),
    NORMAL("normal"),
    VALUE("value"),
    NONE("none")
}