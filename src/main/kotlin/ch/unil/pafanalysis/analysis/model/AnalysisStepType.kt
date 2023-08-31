package ch.unil.pafanalysis.analysis.model

enum class AnalysisStepType(val value: String, val hasPlot: Boolean = false) {
    INITIAL_RESULT("initial-result"),
    QUALITY_CONTROL("quality-control"),
    BOXPLOT("boxplot", true),
    IMPUTATION("imputation"),
    LOG_TRANSFORMATION("log-transformation"),
    FILTER("filter"),
    GROUP_FILTER("group-filter"),
    T_TEST("t-test"),
    VOLCANO_PLOT("volcano-plot", true),
    REMOVE_IMPUTED("remove-imputed"),
    REMOVE_COLUMNS("remove-columns"),
    PCA("pca", true),
    SCATTER_PLOT("scatter-plot", true),
    NORMALIZATION("normalization"),
    SUMMARY_STAT("summary-stat"),
    ORDER_COLUMNS("order-columns"),
    RENAME_COLUMNS("rename-columns")
}