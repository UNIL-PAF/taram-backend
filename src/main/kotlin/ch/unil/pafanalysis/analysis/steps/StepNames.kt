package ch.unil.pafanalysis.analysis.steps

object StepNames {
    fun getName(type: String?): String {
        return when (type) {
            "boxplot" -> "Boxplot"
            "filter"->"Filter rows"
            "group-filter"->"Filter on valid"
            "imputation"->"Imputation"
            "initial-result"->"Initial result"
            "log-transformation"->"Log transformation"
            "normalization"->"Normalization"
            "order-columns"->"Order columns"
            "pca"->"PCA"
            "remove-columns"->"Remove columns"
            "remove-imputed"->"Remove imputed"
            "rename-columns"->"Rename headers"
            "scatter-plot"->"Scatter plot"
            "summary-stat"->"Summary"
            "t-test"->"t-test"
            "volcano-plot"->"Volcano plot"
            else -> throw Exception("Type [$type] doesnt exist.")
        }
    }
}