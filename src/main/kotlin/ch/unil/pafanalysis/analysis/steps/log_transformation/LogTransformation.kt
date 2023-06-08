package ch.unil.pafanalysis.analysis.steps.log_transformation

data class LogTransformation(
    val min: Double?,
    val max: Double?,
    val mean: Double?,
    val median: Double?,
    val nrNaN: Int?,
    val nrValid: Int?,
    val sum: Double?
)