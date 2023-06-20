package ch.unil.pafanalysis.analysis.steps.order_columns

data class OrderColumnsParams (
    val moveColumn: List<Pair<Int, Int>>? = null,
    val addGroupsToHeaders: Boolean? = null,
    val moveSelIntFirst: Boolean? = null
)