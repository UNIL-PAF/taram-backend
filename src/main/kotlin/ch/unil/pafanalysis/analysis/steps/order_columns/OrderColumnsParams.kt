package ch.unil.pafanalysis.analysis.steps.order_columns

data class OrderColumnsParams (
    val move: List<MoveCol>? = null,
    val newOrder: List<Int>? = null,
    val moveSelIntFirst: Boolean? = null
)

data class MoveCol(val from: Int? = null, val to: Int? = null)