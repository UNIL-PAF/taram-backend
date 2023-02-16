package ch.unil.pafanalysis.analysis.steps.summary_stat

data class SummaryStat(
    val min: Double?,
    val max: Double?,
    val mean: Double?,
    val median: Double?,
    val nrNans: Int?,
    val sum: Double?
)