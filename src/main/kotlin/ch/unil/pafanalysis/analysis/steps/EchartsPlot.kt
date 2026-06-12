package ch.unil.pafanalysis.analysis.steps

data class EchartsPlot(
    val echartsOptions: String? = null,
    val echartsHash: Long? = null,
    val outputPath: String? = null,
    val width: Double? = null,
    val height: Double? = null,
    val pdfWidth: Double? = null,
    val zoom: Int? = null,
)