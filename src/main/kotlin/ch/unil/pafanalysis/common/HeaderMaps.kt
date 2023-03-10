package ch.unil.pafanalysis.common

object HeaderMaps {
    val maxQuant = mapOf(
        "id" to "id",
        "prot" to "Majority.protein.IDs",
        "gene" to "Gene.names",
        "desc" to "Protein.names"
    )

    val spectronaut = mapOf(
        "prot" to "PG.ProteinGroups",
        "gene" to "PG.Genes",
        "desc" to "PG.FASTAHeader"
    )

}