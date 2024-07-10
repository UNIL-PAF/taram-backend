package ch.unil.pafanalysis.analysis.steps.initial_result.maxquant

import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType


object AdaptMaxQuantTable {

    private val readTable = ReadTableData()

    private val fastaGeneRegex = Regex("(?<=GN=)(.+?)(?=$|\\s)")
    private val fastaProtRegex = Regex(".+?\\s(.+?)\\s[A-Z]{2}=")

    fun adaptTable(table: Table?): Table? {
        val fastaHeaders = readTable.getStringColumn(table, "Fasta.headers")
        val genes = readTable.getStringColumn(table, HeaderTypeMapping().getCol("geneNames", ResultType.MaxQuant.value))
        val prots = readTable.getStringColumn(table, HeaderTypeMapping().getCol("description", ResultType.MaxQuant.value))

        val newTable = if (fastaHeaders != null && genes != null && prots != null) {
            completeGenesFromFasta(table, genes, prots, fastaHeaders)
        } else table

        return newTable
    }

    private fun completeGenesFromFasta(table: Table?, genes: List<String>, prots: List<String>, fastaHeader: List<String>): Table? {
        val geneIdx = table?.headers?.map{it.name}?.indexOf(HeaderTypeMapping().getCol("geneNames", ResultType.MaxQuant.value))!!
        val newGenes = genes.zip(fastaHeader).map{ (gene, fasta) ->
            if(gene.isEmpty()){
                val fastaList = fasta.split(";")
                val matches = fastaList.map{oneFasta -> fastaGeneRegex.find(oneFasta)?.value}.filterNotNull().distinct()
                matches.joinToString(";")
            } else gene
        }

        val protsIdx = table?.headers?.map{it.name}?.indexOf(HeaderTypeMapping().getCol("description", ResultType.MaxQuant.value))!!
        val newProts = prots.zip(fastaHeader).map { (prot, fasta) ->
            if(prot.isEmpty()){
                val fastaList = fasta.split(";")
                val matches = fastaList.map{oneFasta ->
                    val oneMatch = fastaProtRegex.find(oneFasta)
                    if(oneMatch != null && oneMatch.groupValues.size == 2) oneMatch.groupValues[1] else null
                }.filterNotNull().distinct()
                matches.joinToString(";")
            } else prot
        }

        val newCols = table?.cols?.take(geneIdx)?.plusElement(newGenes)?.plus(table?.cols?.drop(geneIdx+1))
        val newCols2 = newCols?.take(protsIdx)?.plusElement(newProts)?.plus(newCols?.drop(protsIdx+1))
        return table.copy(cols = newCols2)
    }
}
