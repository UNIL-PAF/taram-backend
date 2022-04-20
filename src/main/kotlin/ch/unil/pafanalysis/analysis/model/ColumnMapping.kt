package ch.unil.pafanalysis.analysis.model

data class ExpInfo(
    val fileName: String? = null,
    val isSelected: Boolean? = null,
    val name: String? = null,
    val originalName: String? = null,
    val group: String? = null
)

data class ColumnMapping (
    val columns: List<String>? = null,
    val intColumn: String? = null,
    val experimentColumns: List<String>? = null,
    val numericalColumns: List<String>? = null,
    val experimentDetails: HashMap<String, ExpInfo>? = null,
    val experimentNames: List<String>? = null,
)