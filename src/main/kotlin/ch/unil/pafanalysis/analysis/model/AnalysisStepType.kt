package ch.unil.pafanalysis.analysis.model

enum class AnalysisStepType(val value: String) {
    INITIAL_RESULT("initial-result"),
    QUALITY_CONTROL("quality-control"),
    BOXPLOT("boxplot"),
    TRANSFORMATION("transformation"),
    FILTER("filter"),
    GROUP_FILTER("group-filter")
}