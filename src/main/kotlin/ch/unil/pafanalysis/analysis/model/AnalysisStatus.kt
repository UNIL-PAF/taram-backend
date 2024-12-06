package ch.unil.pafanalysis.analysis.model

enum class AnalysisStatus(val value: String) {
    IDLE("idle"),
    RUNNING("running"),
    DONE("done"),
    ERROR("error")
}