package ch.unil.pafanalysis.analysis.model

data class ExpInfo(
    val fileName: String? = null,
    val isUsed: Boolean? = null,
    val name: String? = null,
)

data class ColumnMapping (
    val intensityColumns: HashMap<String, Int>? = null,
    val experimentDetails: HashMap<String, ExpInfo>? = null,
    val experimentNames: List<String>? = null,
    val groupMapping: HashMap<String, String>? = null
)