package ch.unil.pafanalysis.analysis.steps.summary_stat

data class SummaryStat(
    val groups: List<OneGroupSummary>?
)

data class OneGroupSummary(
    val expName: String? = null,
    val groupName: String? = null,
    val min: Double?,
    val max: Double?,
    val mean: Double?,
    val median: Double?,
    val nrValid: Int?,
    val sum: Double?,
    val stdDev: Double? = null,
    val stdErr: Double? = null,
    val coeffOfVar: Double? = null
)