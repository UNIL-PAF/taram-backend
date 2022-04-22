package ch.unil.pafanalysis.analysis.steps.transformation

data class TransformationParams (
    val normalizationType: String? = null,
)

enum class NormalizationType(val value: String) {
    MEDIAN("median"),
    MEAN("mean"),
}