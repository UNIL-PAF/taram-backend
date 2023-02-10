package ch.unil.pafanalysis.analysis.model

enum class AnalysisStepType(val value: String) {
    INITIAL_RESULT("initial-result"),
    QUALITY_CONTROL("quality-control"),
    BOXPLOT("boxplot"),
    IMPUTATION("imputation"),
    LOG_TRANSFORMATION("log-transformation"),
    FILTER("filter"),
    GROUP_FILTER("group-filter"),
    T_TEST("t-test"),
    VOLCANO_PLOT("volcano-plot"),
    REMOVE_IMPUTED("remove-imputed"),
    REMOVE_COLUMNS("remove-columns"),
    PCA("pca"),
    SCATTER_PLOT("scatter-plot")
}