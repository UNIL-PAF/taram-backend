package ch.unil.pafanalysis.analysis.steps.limma

data class LimmaParams (
    val field: String? = null,
    val firstGroup: List<String>? = null,
    val secondGroup: List<String>? = null,
    val multiTestCorr: String? = MulitTestCorr.BH.value,
    val signThres: Double? = 0.05,
    val valuesAreLog: Boolean = true,
    val paired: Boolean? = null,
    val filterOnValid: Boolean? = null,
    val minNrValid: Int? = null,
    val trend: Boolean? = null,
)

enum class MulitTestCorr(val value: String) {
    NONE("none"),
    BH("BH")
}