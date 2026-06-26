package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.results.model.ResultType

class HeaderTypeMapping {

    // 1. MaxQuant
    // 2. Spectronaut
    private val mapping: Map<String, List<String?>> = mapOf(
        "id" to listOf("id", null, null),
        "proteinIds" to listOf("Majority.protein.IDs", "PG.ProteinGroups", "Protein.ID"),
        "geneNames" to listOf("Gene.names", "PG.Genes", "Gene"),
        "description" to listOf("Protein.names", "PG.ProteinDescriptions", "Description"),
    )

    fun getCol(name: String, type: String?): String {
        val colName = when (type) {
            ResultType.MaxQuant.value -> mapping[name]?.get(0)
            ResultType.Spectronaut.value -> mapping[name]?.get(1)
            ResultType.FragPipe.value -> mapping[name]?.get(2)
            else -> throw StepException("Result type [${type}] not found.")
        }

        return colName ?: throw StepException("Header type [$name] is not defined.")
    }

}