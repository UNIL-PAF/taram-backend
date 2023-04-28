package ch.unil.pafanalysis.analysis.steps.summary_stat

data class SummaryStat(
    val expNames: List<String>? = null,
    val groups: List<String>? = null,
    val min: List<Double>? = null,
    val max: List<Double>? = null,
    val mean: List<Double>? = null,
    val median: List<Double>? = null,
    val nrValid: List<Int>? = null,
    val nrNaN: List<Int>? = null,
    val sum: List<Double>? = null,
    val stdDev: List<Double>? = null,
    val stdErr: List<Double>? = null,
    val coefOfVar: List<Double>? = null
)
