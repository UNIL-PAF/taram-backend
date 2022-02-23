package ch.unil.pafanalysis.analysis.model

enum class AnalysisStepStatus(val value: String) {
    IDLE("idle"),
    RUNNING("running"),
    DONE("done"),
    ERROR("error")
}