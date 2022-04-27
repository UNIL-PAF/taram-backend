package ch.unil.pafanalysis.analysis.steps.transformation

data class TransformationParams (
    val normalizationType: String? = null,
    val transformationType: String? = null,
    val imputationType: String? = null
)

enum class NormalizationType(val value: String) {
    MEDIAN("median"),
    MEAN("mean"),
}


enum class TransformationType(val value: String) {
    LOG2("log2")
}

enum class ImputationType(val value: String){
    NAN("NaN"),
    NORMAL("normal"),
    VALUE("value")
}