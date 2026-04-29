package ch.unil.pafanalysis.analysis.steps.stat_test

data class StatTestParams (
    val field: String? = null,
    val firstGroup: List<String>? = null,
    val secondGroup: List<String>? = null,
    val multiTestCorr: String? = MulitTestCorr.BH.value,
    val signThres: Double? = 0.05,
    val valuesAreLog: Boolean = true,
    val paired: Boolean? = null,
    val filterOnValid: Boolean? = null,
    val minNrValid: Int? = null,
    val statTestType: String? = null,
    val limmaParams: LimmaParams? = null,
)

data class LimmaParams(
    val trend: Boolean? = null,
)

enum class MulitTestCorr(val value: String) {
    NONE("none"),
    BH("BH")
}

enum class StatTestType(val value: String) {
    STUDENT_T_TEST("student_t_test"),
    WELCH_T_TEST("welch_t_test"),
    LIMMA("limma")
}