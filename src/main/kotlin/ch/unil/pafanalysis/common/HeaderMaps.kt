package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.results.model.ResultType

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

    fun getHeaderMap(resType: String?): Map<String, String> {
        return if(resType == ResultType.MaxQuant.value) maxQuant else spectronaut
    }
}