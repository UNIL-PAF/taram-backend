package ch.unil.pafanalysis.analysis.steps.log_transformation

data class LogTransformationParams (
    val transformationType: String? = null,
    val intCol: String? = null
)

enum class TransformationType(val value: String) {
    LOG2("log2"),
    NONE("none")
}