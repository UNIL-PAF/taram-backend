package ch.unil.pafanalysis.analysis.steps.rename_columns

data class RenameColumnsParams (
    val rename: List<RenameCol>? = null,
    val addConditionNames: Boolean? = null
)

data class RenameCol(val idx: Int? = null, val from: String? = null, val to: String? = null)