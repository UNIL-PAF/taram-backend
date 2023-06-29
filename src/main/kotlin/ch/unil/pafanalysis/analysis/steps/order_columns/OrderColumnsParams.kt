package ch.unil.pafanalysis.analysis.steps.order_columns

data class OrderColumnsParams (
    val move: List<MoveCol>? = null,
    val moveSelIntFirst: Boolean? = null
)

data class MoveCol(val from: Int? = null, val to: Int? = null)