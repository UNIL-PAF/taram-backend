package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.results.model.ResultType

class HeaderTypeMapping {

    // 1. MaxQuant
    // 2. Spectronaut
    private val mapping: Map<String, List<String?>> = mapOf(
        "id" to listOf("id", null),
        "proteinIds" to listOf("Majority.protein.IDs", "PG.ProteinGroups"),
        "geneNames" to listOf("Gene.names", "PG.Genes"),
        "description" to listOf("Protein.names", "PG.FASTAHeader"),
    )

    fun getCol(name: String, type: String?): String {
        val colName = when (type) {
            ResultType.MaxQuant.value -> mapping[name]?.get(0)
            ResultType.Spectronaut.value -> mapping[name]?.get(1)
            else -> throw StepException("Result type [${type}] not found.")
        }

        return colName ?: throw StepException("Header type [$name] is not defined.")
    }

}