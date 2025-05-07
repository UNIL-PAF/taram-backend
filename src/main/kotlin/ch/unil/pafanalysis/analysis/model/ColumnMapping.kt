package ch.unil.pafanalysis.analysis.model

data class ExpInfo(
    val fileName: String? = null,
    val isSelected: Boolean? = null,
    val name: String? = null,
    val originalName: String? = null,
    val group: String? = null,
    val idx: Int? = null,
)

data class ColumnMapping(
    val experimentDetails: Map<String, ExpInfo>? = null,
    val experimentNames: List<String>? = null,
    val groupsOrdered: List<String>? = null,
    val intCol: String? = null
)

data class Header(
    val name: String? = null,
    val idx: Int,
    val type: ColType? = null,
    val experiment: Experiment? = null
)

data class Experiment(
    val name: String? = null,
    val field: String? = null,
    val comp: GroupComp? = null
    )

data class GroupComp(val group1: String, val group2: String)

enum class ColType(val value: String) {
    CHARACTER("character"),
    NUMBER("number"),
    EMPTY("empty"),
}